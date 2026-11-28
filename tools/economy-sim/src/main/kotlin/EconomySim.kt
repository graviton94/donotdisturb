import com.graviton94.notificationcleaner.core.Assist
import com.graviton94.notificationcleaner.core.BackupItem
import com.graviton94.notificationcleaner.core.Beat
import com.graviton94.notificationcleaner.core.Catalog
import com.graviton94.notificationcleaner.core.CloudItem
import com.graviton94.notificationcleaner.core.Engine
import com.graviton94.notificationcleaner.core.EventCard
import com.graviton94.notificationcleaner.core.GameState
import java.math.BigInteger
import java.math.BigInteger.ZERO

/*
 * Economy v0.2 simulator (docs/economy-v0.2.md). Plays whole runs of the core rules:
 * the reference player swipes every battery charge with Engine.bestTarget, spends P with a greedy
 * policy, rebuilds the deck by a deck policy, factory-resets when backup files per hour peak, and
 * spends backup files on the cheapest useful item.
 *
 *   ./gradlew -p tools/economy-sim run --args="6 8 2"   # hours cap per run, seeds, runs per seed
 */

enum class DeckPolicy(val label: String) { Min("최소 덱"), Max("최대 덱 (가치순)"), Synergy("최대 덱 (시너지)") }

/** Require the shipped screen actions and reward eligibility in launch simulations. */
internal val screenOnly = System.getenv("SIM_SCREEN_ONLY") == "1"

private val e = Engine(simRules())  // same rules as Daily.kt, so SIM_RULES reaches prices too
private const val T0 = 1_000_000L

class RunLog {
    val buys = mutableListOf<Pair<Double, String>>()
    val levelAt = sortedMapOf<Int, Double>()
    val digitsAt = sortedMapOf<Int, Int>()
    var resetAt: Double? = null
    var files = 0L
    var swipes = 0
    var maxChain = 0
    var runDigits = 0
}

private fun digits(n: BigInteger) = if (n.signum() <= 0) 1 else n.toString().length
private val valuationCount = BigInteger.valueOf(1_000)
private fun perNotification(s: GameState, app: String): BigInteger = e.earn(s, app, valuationCount, 0)
private val appPrices = java.util.concurrent.ConcurrentHashMap<com.graviton94.notificationcleaner.core.AppDef, BigInteger>()

// Prefer AllHands sandwiches, then Drill, record events, and permanent upgrade cards.
private val cardPriority = listOf(
    EventCard.AllHands, EventCard.Drill, EventCard.Lotto, EventCard.Otp, EventCard.Birthday,
    EventCard.Firmware, EventCard.GroupText, EventCard.Update, EventCard.Sale, EventCard.Dnd,
)

/** Rebuild the app deck and the event deck by policy. */
fun chooseDeck(s: GameState, policy: DeckPolicy): GameState {
    val owned = s.appLevels.keys.toList()
    val size = when (policy) {
        DeckPolicy.Min -> maxOf(2, e.minSlots(s))
        else -> e.maxSlots(s)
    }.coerceAtMost(owned.size)
    val deck: List<String> = when (policy) {
        DeckPolicy.Synergy -> {
            val picked = mutableListOf<String>()
            val value = e.deckValuation(s, valuationCount)
            repeat(size) {
                val best = owned.asSequence().filter { it !in picked }.maxBy { cand -> value(picked + cand) }
                picked += best
            }
            picked
        }
        else -> owned.sortedByDescending { perNotification(s.copy(active = listOf(it)), it) }.take(size)
    }
    val cards = cardPriority.filter { it in s.events }.take(e.eventSlots(s))
    if (!screenOnly) return e.setDeck(s, deck, cards) ?: s
    // The app deck and the cards are separate actions in WebSession.
    val withApps = if (s.active == deck) s else e.setDeck(s, deck) ?: s
    return if (withApps.equipped == cards) withApps else e.setDeck(withApps, withApps.active, cards) ?: withApps
}

/** Greedy spending. Returns the new state and what was bought. */
fun spend(input: GameState, policy: DeckPolicy): Pair<GameState, List<String>> {
    var s = input
    val bought = mutableListOf<String>()
    while (true) {
        val bestTier = s.appLevels.keys.maxOf { Catalog.app(it).tier }
        val nextApp = Catalog.apps.asSequence().filter { it.id !in s.appLevels && e.unlocked(s, it.id) }
            .minByOrNull { appPrices.computeIfAbsent(it, e.rules::appPrice) }
        val appPrice = nextApp?.let { appPrices.computeIfAbsent(it, e.rules::appPrice) }
        // 1. A new app: always for a new tier, otherwise only while the deck has room or the policy wants options.
        if (nextApp != null && s.wallet >= e.appPrice(s, nextApp.id) &&
            (nextApp.tier > bestTier || s.active.size < e.maxSlots(s) || policy != DeckPolicy.Min)) {
            s = e.buy(s, nextApp.id); bought += "앱:${nextApp.name}"; continue
        }
        // 2. Event cards, board assists and Pro versions when they cost at most half the wallet.
        val half = s.wallet / BigInteger.TWO
        val card = EventCard.entries.firstOrNull { it !in s.events && s.level >= it.unlockLevel && e.rules.cardPrice(it) <= half }
        if (card != null) { s = e.buyEvent(s, card); bought += "카드:${card.label}"; continue }
        val assist = Assist.entries.firstOrNull { s.level >= it.unlockLevel && s.assist(it) < it.max(s.level) && e.rules.assistPrice(it, s.assist(it)) <= half }
        if (assist != null) { s = e.buyAssist(s, assist); bought += "판:${assist.label}"; continue }
        val pro = s.active.firstOrNull { it !in s.pro && s.level >= e.proLevel(s, it) && e.proPrice(s, it) <= half }
        if (pro != null) { s = e.buyPro(s, pro); bought += "프로:${Catalog.app(pro).name}"; continue }
        // 3. The upgrade with the best value gain per P, unless saving for an app it would delay a lot.
        val ups = s.active.asSequence().map { id -> id to e.upgradePrice(s, id) }
            .filter { (_, price) -> s.wallet >= price }
            .map { (id, price) ->
                val gain = e.upgradeEarnGain(s, id, valuationCount)
                Triple(id, price, gain.toDouble() / price.toDouble())
            }
        val up = ups.maxByOrNull { it.third } ?: break
        if (appPrice != null && up.second * BigInteger.TEN > appPrice && s.wallet < appPrice) break
        s = e.upgrade(s, up.first); bought += "강화"
    }
    return s to bought
}

private val backupOrder = listOf(
    BackupItem.Value, BackupItem.FastBoot, BackupItem.StartFunds, BackupItem.Sandwich, BackupItem.ChainBoost,
    BackupItem.Milestone, BackupItem.Synergy, BackupItem.RememberedApps, BackupItem.DeepOverload, BackupItem.Battery,
    BackupItem.AutoClean, BackupItem.ExtraSlot, BackupItem.Board, BackupItem.Wild, BackupItem.EventSlot,
)

fun spendBackup(input: GameState): GameState {
    var s = input
    while (true) {
        val open = BackupItem.entries.filter { e.backupOpen(s, it) }
        // The infinite value sink first whenever it costs no more than twice the cheapest other item.
        val cheapest = open.minByOrNull { e.backupPrice(s, it) } ?: break
        val value = open.firstOrNull { it == BackupItem.Value }
        val item = if (value != null && e.backupPrice(s, value) <= 2 * e.backupPrice(s, cheapest)) value else cheapest
        val next = e.buyBackup(s, item)
        if (next == s) break
        s = next
    }
    return s
}

/** Cloud shop: cheapest open item first. */
fun spendCloud(input: GameState): GameState {
    var s = input
    while (true) {
        val c = s.backup.cloud
        val item = CloudItem.entries
            .filter { e.cloudOpen(s, it) }
            .minByOrNull { e.rules.cloudPrice(it, c.lv(it)) } ?: break
        val next = e.buyCloud(s, item)
        if (next == s) break
        s = next
    }
    return s
}

/** One run from [start] until the reset policy fires or [capHours]. */
fun playRun(start: GameState, policy: DeckPolicy, capHours: Double): Pair<GameState, RunLog> {
    val log = RunLog()
    var s = chooseDeck(start, policy)
    var t = T0
    var bestFilesRate = 0.0
    var lv25At: Long? = null
    val marks = listOf(1, 5, 15, 30, 60, 120, 240, 360, 480, 720)
    var mark = 0
    while (t < T0 + (capHours * 3_600_000).toLong()) {
        t += e.chargeMs(s)
        val min = (t - T0) / 60_000.0
        while (mark < marks.size && min >= marks[mark]) { log.digitsAt[marks[mark]] = digits(s.bestSwipe); mark++ }
        s = e.tick(s, t)
        val target = e.bestTarget(s, t) ?: continue
        val o = e.swipe(s, target, t) ?: continue
        val levelUp = o.beats.any { it is Beat.LevelUp }
        s = o.state
        log.swipes++
        log.maxChain = maxOf(log.maxChain, o.chain)
        if (levelUp) log.levelAt.putIfAbsent(s.level, min)
        val (spent, bought) = spend(s, policy)
        s = spent
        bought.filter { !it.startsWith("강화") }.forEach { log.buys += min to it }
        bought.filter { it == "강화" }.forEach { log.buys += min to it }
        if (bought.isNotEmpty() || levelUp) s = chooseDeck(s, policy)
        if (s.level >= e.rules.resetLevel) {
            if (lv25At == null) lv25At = t
            val rate = e.backupFilesNow(s) / ((t - T0) / 3_600_000.0)
            bestFilesRate = maxOf(bestFilesRate, rate)
            if (t - lv25At >= 600_000 && rate < bestFilesRate * 0.97) {
                log.resetAt = min
                log.files = e.backupFilesNow(s)
                log.runDigits = digits(s.runEarned)
                break
            }
        }
    }
    if (log.resetAt == null && s.level >= e.rules.resetLevel) { log.resetAt = (t - T0) / 60_000.0; log.files = e.backupFilesNow(s); log.runDigits = digits(s.runEarned) }
    return s to log
}

fun pct(xs: List<Double>, p: Double) = if (xs.isEmpty()) Double.NaN else xs.sorted()[((xs.size - 1) * p).toInt()]
fun p50(xs: List<Double>) = pct(xs, .5)
fun fmtMin(m: Double) = when { m.isNaN() -> "-"; m < 2 -> "%.0f초".format(m * 60); m < 60 -> "%.0f분".format(m); else -> "%.1f시간".format(m / 60) }

fun main(args: Array<String>) {
    if (args.firstOrNull() == "opening") return opening(java.io.File(args[1]), args[2].toInt())
    if (args.firstOrNull() == "launch-v1") return launchV1(java.io.File(args[1]), args[2].toInt(), args[3].toInt(),
        profilesFor(args.getOrNull(4)), (args.getOrNull(5) ?: "Best").split(',').toSet())
    if (args.firstOrNull() == "challenge-star-audit") return challengeStarAudit(java.io.File(args.getOrNull(1) ?: error("Choose a CSV output file")))
    if (args.firstOrNull() == "challenge-star-proposal") return challengeStarProposal(java.io.File(args.getOrNull(1) ?: error("Choose a CSV output file")))
    if (args.firstOrNull() == "offline-upgrade-audit") return offlineUpgradeAudit(java.io.File(args.getOrNull(1) ?: error("Choose an output directory")))
    if (args.firstOrNull() == "growth-tail-audit") return growthTailAudit(java.io.File(args.getOrNull(1) ?: error("Choose an output directory")))
    if (args.firstOrNull() == "balance-commerce") return balanceCommerce(java.io.File(args.getOrNull(1) ?: error("Choose an output directory")))
    if (args.firstOrNull() in setOf("balance-report", "balance-audit")) return balanceAudit(
        java.io.File(args.getOrNull(1) ?: error("Choose an output directory")),
        args.getOrNull(2), args.getOrNull(3) == "available-rewards")
    if (args.firstOrNull() == "balance-detail") return balanceDetail(java.io.File(args.getOrNull(1) ?: error("Choose an output directory")))
    if (args.firstOrNull() == "debug") return debugRun()
    if (args.firstOrNull() == "money") return monetization(args.getOrNull(1)?.toInt() ?: 84, args.getOrNull(2)?.toLong() ?: 4L, profilesFor(args.getOrNull(3)))
    if (args.firstOrNull() == "daily") return daily(args.getOrNull(1)?.toInt() ?: 56, args.getOrNull(2)?.toLong() ?: 4L)
    if (args.firstOrNull() == "update") return updateCheck(args.getOrNull(1)?.toInt() ?: 84, args.getOrNull(2)?.toInt() ?: 56,
        args.getOrNull(3)?.toLong() ?: 4L, com.graviton94.notificationcleaner.core.ContentPack.valueOf(args.getOrNull(4) ?: "AiTier"))
    val cap = args.getOrNull(0)?.toDouble() ?: 6.0
    val seeds = args.getOrNull(1)?.toLong() ?: 8L
    val runs = args.getOrNull(2)?.toInt() ?: 2
    for (policy in DeckPolicy.entries) {
        val perRun = List(runs) { mutableListOf<RunLog>() }
        var sample: List<Pair<Double, String>> = emptyList()
        for (seed in 1L..seeds) {
            var s = e.newGame(T0, seed)
            for (r in 0 until runs) {
                val (end, log) = playRun(s, policy, cap)
                perRun[r] += log
                if (seed == 1L && r == 0) sample = log.buys
                val reset = e.factoryReset(end, T0) ?: break
                s = spendBackup(reset)
            }
        }
        println("\n=== ${policy.label} · 회차 상한 ${cap}시간 · 시드 $seeds")
        perRun.forEachIndexed { r, logs ->
            if (logs.isEmpty()) return@forEachIndexed
            val first = logs.mapNotNull { it.buys.firstOrNull()?.first }
            val firstApp = logs.mapNotNull { l -> l.buys.firstOrNull { it.second.startsWith("앱") }?.first }
            println("[회차 ${r + 1}] 첫 구매 ${fmtMin(p50(first))} · 첫 앱 ${fmtMin(p50(firstApp))} · " +
                listOf(5, 10, 15, 20, 25).joinToString(" · ") { lv -> "Lv$lv ${fmtMin(p50(logs.mapNotNull { it.levelAt[lv] }))}" } +
                " · 초기화 ${fmtMin(p50(logs.mapNotNull { it.resetAt }))} (${logs.count { it.resetAt != null }}/${logs.size}) · 백업파일 ${p50(logs.map { it.files.toDouble() }).toLong()} · 누적 P ${p50(logs.map { it.runDigits.toDouble() }).toInt()}자리")
            println("         최고점 자릿수 " + listOf(1, 5, 15, 30, 60, 120, 240).joinToString(" · ") { m ->
                "${fmtMin(m.toDouble())} ${p50(logs.mapNotNull { it.digitsAt[m]?.toDouble() }).let { if (it.isNaN()) "-" else it.toInt().toString() }}"
            } + " · 최대 연쇄 ${p50(logs.map { it.maxChain.toDouble() }).toInt()}")
        }
        // Sawtooth: minutes between non-upgrade purchases for seed 1, run 1.
        val big = sample.filter { !it.second.startsWith("강화") }
        println("  시드1 주요 구매: " + big.joinToString(" ") { "${fmtMin(it.first)}:${it.second.substringAfter(':')}" })
        val ups = sample.filter { it.second == "강화" }.map { it.first }
        val perHour = (0 until 6).map { h -> ups.count { it >= h * 60 && it < (h + 1) * 60 } }
        println("  시드1 강화 횟수/시간: $perHour")
    }
}
