import com.graviton94.notificationcleaner.core.Actor
import com.graviton94.notificationcleaner.core.Assist
import com.graviton94.notificationcleaner.core.BackupItem
import com.graviton94.notificationcleaner.core.Beat
import com.graviton94.notificationcleaner.core.Catalog
import com.graviton94.notificationcleaner.core.CloudItem
import com.graviton94.notificationcleaner.core.CollectionRewards
import com.graviton94.notificationcleaner.core.LiveTuning
import com.graviton94.notificationcleaner.core.Engine
import com.graviton94.notificationcleaner.core.EventCard
import com.graviton94.notificationcleaner.core.GameState
import com.graviton94.notificationcleaner.core.Meta
import com.graviton94.notificationcleaner.core.RowKind
import com.graviton94.notificationcleaner.core.SwipeOutcome
import com.graviton94.notificationcleaner.core.AdSlot
import com.graviton94.notificationcleaner.core.CreditItem
import com.graviton94.notificationcleaner.core.SpeedPass
import com.graviton94.notificationcleaner.core.account

/*
 * Economy v0.3: weeks of everyday play (docs/economy-v0.3.md).
 * A day is [sessions] sessions of [sessionMin] minutes of direct play spread over 16 waking hours,
 * offline in between and overnight. Offline income is settled on return. Resets follow [ResetPolicy].
 *
 *   ./gradlew -p tools/economy-sim run --args="daily 56 4"   # days, seeds
 */

/** SIM_RULES="overclockPermille=3000,premiumStepPermille=2000": try rule values without touching the engine. */
internal fun simRules(): com.graviton94.notificationcleaner.core.RuleSet {
    var r = com.graviton94.notificationcleaner.core.RuleSet()
    System.getenv("SIM_RULES")?.split(",")?.filter { "=" in it }?.forEach { kv ->
        val (k, v) = kv.split("=")
        r = when (k.trim()) {
            "overclockPermille" -> r.copy(overclockPermille = v.toInt())
            "premiumStepPermille" -> r.copy(premiumStepPermille = v.toInt())
            "passOverclockMinutes" -> r.copy(passOverclockMinutes = v.toInt())
            "boostPermille" -> r.copy(boostPermille = v.toInt())
            "warpMinutes" -> r.copy(warpMinutes = v.toInt())
            "levelRatioPermille" -> r.copy(levelRatioPermille = v.toInt())
            "adWatchOfflinePermille" -> r.copy(adWatchOfflinePermille = v.toInt())
            "ultraOfflinePermille" -> r.copy(ultraOfflinePermille = v.toInt())
            "offlineEfficiencyPercent" -> r.copy(offlineEfficiencyPercent = v.toInt())
            "boostMinutes" -> r.copy(boostMinutes = v.toInt())
            "upgradeGrowthPerTierPermille" -> r.copy(upgradeGrowthPerTierPermille = v.toInt())
            "overloadMinIntervalMs" -> r.copy(overloadMinIntervalMs = v.toLong())
            "majorEventCooldownMs" -> r.copy(majorEventCooldownMs = v.toLong())
            "resetLevel" -> r.copy(resetLevel = v.toInt())
            "syncLevel" -> r.copy(syncLevel = v.toInt())
            "gaugePerMerge" -> r.copy(gaugePerMerge = v.toInt())
            "gaugePerBurst" -> r.copy(gaugePerBurst = v.toInt())
            "igniterGaugePerTier" -> r.copy(igniterGaugePerTier = v.toInt())
            "fallingChains" -> r.copy(fallingChains = v.toBooleanStrict())
            "fallMatchPermille" -> r.copy(fallMatchPermille = v.toInt())
            "fallSpamStepPermille" -> r.copy(fallSpamStepPermille = v.toInt())
            "fallSpamFrom" -> r.copy(fallSpamFrom = v.toInt())
            "lateSwipeCapSteps" -> r.copy(lateSwipeCapSteps = v.toInt())
            "swipeLevelCap" -> r.copy(swipeLevelCap = v.toInt())
            "offlineLevelCap" -> r.copy(offlineLevelCap = v.toInt())
            "sandwichPercent" -> r.copy(sandwichPercent = v.toInt())
            "sandwichPerAssist" -> r.copy(sandwichPerAssist = v.toInt())
            "wildPerAssist" -> r.copy(wildPerAssist = v.toInt())
            "waveFactor" -> r.copy(waveFactor = v.toInt())
            "waveIncomePermille" -> r.copy(waveIncomePermille = v.toInt())
            "recentIncomeWindow" -> r.copy(recentIncomeWindow = v.toInt())
            "offlineAutoPermille" -> r.copy(offlineAutoPermille = v.toInt())
            "autoSwipePermille" -> r.copy(autoSwipePermille = v.toInt())
            "firstRunEarlyPro" -> r.copy(firstRunEarlyPro = v.toBooleanStrict())
            "firstRunRankPricePermille" -> r.copy(firstRunRankPricePermille = v.toInt())
            "firstRunTier2PricePermille" -> r.copy(firstRunTier2PricePermille = v.toInt())
            "appPriceLevelPermille" -> r.copy(appPriceLevelPermille = v.toInt())
            "tierBase" -> r.copy(tierBase = v.toLong())
            "callEverySwipes" -> r.copy(callEverySwipes = v.toInt())
            "backupExponentPermille" -> r.copy(backupExponentPermille = v.toInt())
            "cloudExponentPermille" -> r.copy(cloudExponentPermille = v.toInt())
            "lateGrowthFrom" -> r.copy(lateGrowthFrom = v.toInt())
            "lateGrowthPermille" -> r.copy(lateGrowthPermille = v.toInt())
            "levelLateFrom" -> r.copy(levelLateFrom = v.toInt())
            "levelRatioStepPermille" -> r.copy(levelRatioStepPermille = v.toInt())
            else -> error("unknown rule $k")
        }
    }
    return r
}
internal val simTuningJson = System.getenv("SIM_LIVE_TUNING").orEmpty()
internal val claimBookRewards = System.getenv("SIM_BOOK_CLAIMS") != "off"
private val engine = Engine(simRules(), LiveTuning.parse(simTuningJson))
private val shop = com.graviton94.notificationcleaner.core.Shop(engine)
private val meta = Meta(engine)
/** An alternate visible ad policy for comparison against the historical opportunistic policy. */
internal val scheduledAds = screenOnly && System.getenv("SIM_AD_PLAN") == "scheduled"
private val traceDays = System.getenv("SIM_TRACE") != null
private val traceResets = System.getenv("SIM_RESETS") != null
private val traceSeed = System.getenv("SIM_SEED")?.toLongOrNull() ?: 1L

/** Spending profiles (docs/bible/17-monetization.md). */
/** [adsPerDay]: rewarded ads watched a day; [speed]: speed subscription kept up; [monthlyData]: data bought each 30 days. */
enum class Payer(val label: String, val adsPerDay: Int, val pass: Boolean, val monthlyData: Long, val speed: SpeedPass? = null) {
    Free("무과금·무광고", 0, false, 0),
    Ads("광고 꾸준히 (하루 5회)", 5, false, 0),
    Pass("월간 패스 ₩5,500", 99, true, 0),
    Spend30("터보 패스 ₩33,000", 99, true, 0, SpeedPass.Turbo),
    Spend100("울트라 패스 ₩99,000", 99, true, 0, SpeedPass.Ultra),
}

/** A spending profile; [slots] limits which ad placements it uses (for one-at-a-time ablation). */
data class Profile(val label: String, val payer: Payer, val slots: Set<AdSlot> = AdSlot.entries.toSet())
/** The app with the highest unlock level: "last app" in the reports. */
private val LAST_APP get() = Catalog.apps.maxBy { it.unlockLevel }.id
private const val START = 1_000_000L
private const val DAY = 86_400_000L

data class DayPlan(val sessions: Int = 4, val sessionMin: Int = 15, val manualFloorMs: Long = 0)

enum class InputPolicy { Best, Random, Auto }

private data class SessionInput(val at: Long, val actor: Actor)

/** Manually play 90 s, browse a sheet for 4 s, then leave the board clear for 6 s. */
private class SessionControls(private val start: Long, private val end: Long, private val autoOnly: Boolean) {
    private var boardReady = start
    private var lastAuto: Long? = null

    fun next(t: Long, manualInterval: Long, autoInterval: Long): SessionInput? {
        val autoCadence = autoInterval.coerceAtLeast(800L)
        val nextAuto = lastAuto?.plus(autoCadence) ?: (start + 5_500)
        if (autoOnly) return maxOf(nextAuto, boardReady, t + 1).takeIf { it <= end }
            ?.let { SessionInput(it, Actor.Auto) }
        val manuallyAt = maxOf(t + manualInterval, boardReady)
        val block = start + (t - start) / 100_000 * 100_000
        if (manuallyAt < block + 90_000) return manuallyAt.takeIf { it <= end }?.let { SessionInput(it, Actor.Player) }
        // The idle clock restarts on closing the sheet. Hold it for 4 seconds, then resume the auto clock.
        val idleAt = maxOf(block + 99_500, nextAuto, boardReady, t + 1)
        if (idleAt < block + 100_000 && idleAt <= end) return SessionInput(idleAt, Actor.Auto)
        val nextBlock = maxOf(block + 100_000 + manualInterval, boardReady)
        return nextBlock.takeIf { it <= end }?.let { SessionInput(it, Actor.Player) }
    }

    fun dispatched(input: SessionInput, outcome: SwipeOutcome?) {
        if (input.actor == Actor.Auto) lastAuto = input.at
        if (outcome == null) return
        // Simulate a bounded board replay. The same board must be free before the next touch or auto swipe.
        val animations = outcome.beats.count { it is Beat.Merged || it is Beat.Burst || it is Beat.Spawn }
        val waves = outcome.beats.count { it is Beat.Wave }
        val replayMs = minOf(6_000L, 250L + animations * 85L + waves * 300L)
        val eventMs = if (!screenOnly) 0L else {
            val row = outcome.beats.filterIsInstance<Beat.Swiped>().firstOrNull()?.row
            when {
                row?.event == EventCard.Drill -> 12_400L // Ten seconds to tap, then the result overlay.
                row?.event == EventCard.Lotto -> 3_600L
                row?.kind == RowKind.Call -> 1_300L
                else -> 0L
            }
        }
        boardReady = input.at + maxOf(replayMs, eventMs)
    }
}

interface DailyAudit {
    val throughDay: Int
    fun startSession(day: Int, state: GameState, now: Long) = Unit
    fun endSession() = Unit
    fun adAward(slot: AdSlot, credited: java.math.BigInteger) = Unit
    fun activeTime(elapsedMs: Long) = Unit
    fun observe(engine: Engine, state: GameState, now: Long) = Unit
    fun input(actor: Actor) = Unit
    fun screenAction(action: String, item: String = "") = Unit
    fun swipe(engine: Engine, before: GameState, outcome: SwipeOutcome, now: Long)
    fun prestige(kind: String, before: GameState, after: GameState, now: Long, gain: Double, stalls: Int, sessions: Int)
    fun endDay(day: Int, state: GameState)
    fun dayEconomy(day: Int, state: GameState, timeline: Timeline) = Unit
}

class Timeline {
    /** (active minutes, day, label) of every first-time unlock. */
    val unlocks = mutableListOf<Triple<Double, Double, String>>()
    val tierDay = sortedMapOf<Int, Double>()
    var lastAppDay: Double? = null
    val levelDays = sortedMapOf<Int, Double>()
    val level124Day get() = levelDays[124]
    var bestSwipeP = java.math.BigInteger.ZERO
    val appBuys = sortedMapOf<String, Double>()
    val appLevels = sortedMapOf<String, MutableMap<Int, Double>>()
    val premiumByApp = sortedMapOf<String, Int>()
    var dataSpent = 0L
    var dataBalance = 0L
    var vouchers = 0
    var vouchersEarned = 0
    val resetDays = mutableListOf<Double>()
    var credits = 0L
    var bought = 0L
    val stars = mutableMapOf<Int, Int>()
    var ads = 0
    val syncDays = mutableListOf<Double>()
    /** Best level when the update arrived (update mode). */
    var updateLevel = 0
    val bestLevelByWeek = sortedMapOf<Int, Int>()
    val runPeakByWeek = sortedMapOf<Int, Int>()
    var finalLevel = 1
    var lastResetLevel: Int? = null
    var lastSyncLevel: Int? = null
    val digitsByWeek = sortedMapOf<Int, Int>()
    var activeMin = 0.0
    var lifetimeFiles = 0L
    var offlineP = java.math.BigInteger.ZERO
    var activeP = java.math.BigInteger.ZERO
    val lifetimeByWeek = sortedMapOf<Int, Long>()
    val runDigitsByDay = sortedMapOf<Int, Int>()
}

/** Every level that opens something: apps, cards, assists, deck slots, event slots, reset, calls. */
fun unlockLevels(): Map<Int, List<String>> {
    val r = engine.rules
    val m = sortedMapOf<Int, MutableList<String>>()
    fun add(l: Int, s: String) { m.getOrPut(l) { mutableListOf() } += s }
    Catalog.apps.filter { it.unlockLevel > 1 }.forEach { add(it.unlockLevel, "앱 ${it.name}") }
    EventCard.entries.forEach { add(it.unlockLevel, "카드 ${it.label}") }
    Assist.entries.forEach { a -> add(a.unlockLevel, "판 강화 ${a.label}"); a.caps.drop(1).forEach { add(it.first, "${a.label} 최대 ${it.second}") } }
    Catalog.proLevel.forEach { (id, l) -> add(l, "${Catalog.app(id).name} 프로") }
    r.offlineCapSteps.drop(1).forEach { add(it.first, "오프라인 ${it.second}시간") }
    r.maxSlotSteps.drop(1).forEach { add(it.first, "덱 최대 ${it.second}") }
    r.minSlotSteps.drop(1).forEach { add(it.first, "덱 최소 ${it.second}") }
    r.eventSlotSteps.forEach { add(it.first, "이벤트 슬롯 ${it.second}") }
    add(r.resetLevel, "공장 초기화")
    add(r.syncLevel, "클라우드 동기화")
    add(r.callUnlockLevel, "스팸 전화")
    return m
}

/** [update]: on this day (0-based) the pack is switched on, as a content update reaching a veteran save. */
fun playDays(seed: Long, days: Int, plan: DayPlan, resets: Boolean, spam: Boolean = false, update: Pair<Int, com.graviton94.notificationcleaner.core.ContentPack>? = null, payer: Payer = Payer.Free, slots: Set<AdSlot> = AdSlot.entries.toSet(), stopAtLv124: Boolean = false, stopPaidAfterDay: Int? = null, audit: DailyAudit? = null, input: InputPolicy = InputPolicy.Best): Timeline {
    if (Catalog.packs.isNotEmpty()) Catalog.packs = emptySet()  // shared global: only touch it when needed (parallel runs)
    require(!screenOnly || payer.monthlyData == 0L) { "Screen profiles must use a Play product for data purchases" }
    val tl = Timeline()
    var levels = unlockLevels()
    fun refreshDeck(state: GameState): GameState {
        val next = chooseDeck(state, DeckPolicy.Synergy)
        if (screenOnly && state.equipped != next.equipped) audit?.screenAction("setCards", next.equipped.joinToString("|") { it.name })
        return next
    }
    var s = refreshDeck(engine.newGame(START, seed))
    var t = START
    var everLevel = 1
    var everResets = 0
    var stallSessions = 0
    var sinceReset = 0
    var everSyncs = 0
    val gap = (16 * 60 / plan.sessions) * 60_000L
    val ads = AdBudget(payer.adsPerDay)
    val inputRandom = kotlin.random.Random(seed xor 0x52554c4542L)
    fun dayOf(ms: Long) = (ms - START) / DAY.toDouble()
    fun note(label: String) { tl.unlocks += Triple(tl.activeMin, dayOf(t), label) }
    var seenLevels: Map<String, Int>? = null
    var highestLevel = 0
    var weekRunPeak = 1
    val milestonesToRecord = listOf(1, 3, 5, 8, 12)
    fun reachedTarget(day: Int) = stopAtLv124 && day >= (audit?.throughDay ?: 0) && tl.level124Day != null
    fun observe() {
        audit?.observe(engine, s, t)
        weekRunPeak = maxOf(weekRunPeak, s.level)
        while (highestLevel < s.level) tl.levelDays[++highestLevel] = dayOf(t)
        if (s.appLevels === seenLevels) return
        seenLevels = s.appLevels
        for ((app, level) in s.appLevels) {
            tl.appBuys.putIfAbsent(app, dayOf(t))
            tl.tierDay.putIfAbsent(Catalog.app(app).tier, dayOf(t))
            val milestones = tl.appLevels.getOrPut(app) { sortedMapOf() }
            for (tier in milestonesToRecord) if (level >= tier) milestones.putIfAbsent(tier, dayOf(t))
        }
    }
    observe()

    play@ for (day in 0 until days) {
        if (update != null && day == update.first) {
            Catalog.packs = setOf(update.second)
            levels = unlockLevels()
            tl.updateLevel = maxOf(s.backup.bestLevel, s.level)
            tl.lastAppDay = null  // from now on the pack's top app is the last app
        }
        for (session in 0 until plan.sessions) {
            val begin = START + day * DAY + session * gap
            if (t < begin) {
                val (back, got) = engine.settleOffline(s, begin - t, begin)
                s = if (screenOnly) shop.claimPassDaily(shop.claimAttendance(back, begin), begin) else back
                tl.offlineP += got
                t = begin
                observe()
                if (reachedTarget(day)) break@play
                // Save one of the five ads for the longest return; reserve the others for the planned boosts.
                val takeReturnAd = !scheduledAds || payer != Payer.Ads || session == if (day == 0) plan.sessions / 2 else 0
                if (takeReturnAd && AdSlot.OfflineDouble in slots && got.signum() > 0 && ads.take(t)) {
                    shop.watchAd(s, AdSlot.OfflineDouble, t, got)?.let {
                        val credited = it.runEarned - s.runEarned
                        s = it
                        tl.offlineP += credited
                        tl.ads++
                        audit?.adAward(AdSlot.OfflineDouble, credited)
                    } ?: ads.refund()
                }
            }
            val beforeEconomy = s.runEarned
            s = economy(s, t, session, payer, tl, slots, ads, audit)
            tl.activeP += (s.runEarned - beforeEconomy).max(java.math.BigInteger.ZERO)
            observe()
            if (reachedTarget(day)) break@play
            val quiet = engine.activateDnd(s, t)  // 방해 금지 at the start of a session
            if (screenOnly && quiet !== s) audit?.screenAction("dnd")
            s = quiet
            if (traceDays && seed == traceSeed && day in 4..5) println("  세션 d$day s$session lv=${s.level} levelFor=${engine.rules.levelFor(s.runEarned, s.backup.lv(BackupItem.FastBoot))} room=${engine.jackpotRoom(s, 3)} run=${s.runEarned} rows=" + s.rows.joinToString(" ") { "${it.kind}:${it.app}:${it.count}${it.alert ?: ""}" } + " deck=${s.active} drill=${s.drillUntil - t} dnd=${s.dndUntil - t} paused=${s.autoPausedUntil - t} crash=${s.crashBoost}")
            audit?.startSession(day + 1, s, t)
            val levelAtStart = s.level
            val end = t + plan.sessionMin * 60_000L
            val controls = SessionControls(t, end, input == InputPolicy.Auto)
            while (t < end) {
                val move = if (engine.rules.fallingChains) {
                    controls.next(t, maxOf(plan.manualFloorMs, engine.chargeMs(s, t)), engine.autoIntervalMs(s, t))
                } else {
                    val legacyStep = if (input == InputPolicy.Auto) engine.autoIntervalMs(s, t) else engine.chargeMs(s, t)
                    SessionInput(t + legacyStep, if (input == InputPolicy.Auto) Actor.Auto else Actor.Player)
                }
                val step = (move?.at ?: end) - t
                check(step > 0) { "${input.name} clock must advance at Lv${s.level}" }
                audit?.activeTime(minOf(step, end - t))
                t += step
                tl.activeMin += step / 60_000.0
                if (move == null) break
                s = engine.tick(s, t)
                observe()
                val actor = move.actor
                val target = when {
                    actor == Actor.Auto -> engine.autoTarget(s, t)
                    input == InputPolicy.Random -> s.rows.randomOrNull(inputRandom)?.id
                    else -> if (screenOnly) s.rows.firstOrNull { it.kind == RowKind.Call }?.id ?: engine.bestTarget(s, t)
                        else engine.bestTarget(s, t)
                }
                val o0 = target?.let { engine.swipe(s, it, t, actor) }
                if (engine.rules.fallingChains) controls.dispatched(move, o0)
                if (o0 == null && traceDays && seed == traceSeed && day == 5 && session == 0 && t < begin + 20_000) println("  막힘 target=$target energy=${s.energy} rows=" + s.rows.joinToString(" ") { "${it.kind}:${it.app}:${it.count}${it.alert ?: ""}" })
                val best = s.bestSwipe
                val o = o0 ?: continue
                audit?.input(actor)
                audit?.swipe(engine, s, o, t)
                s = if (screenOnly) meta.record(o.state, o, t) else o.state
                if (stopAtLv124) tl.bestSwipeP = tl.bestSwipeP.max(o.earned)
                observe()
                if (reachedTarget(day)) break@play
                // 최고 기록 ×2: offered when a swipe beats the run's best (only worth it once the run is going).
                val takeRecordAd = !scheduledAds || payer != Payer.Ads || session == plan.sessions - 1
                if (takeRecordAd && AdSlot.BestDouble in slots && s.level >= engine.rules.bestDoubleFrom && o.earned > best &&
                    shop.adsLeft(s, AdSlot.BestDouble, t) > 0 && ads.take(t)) {
                    shop.watchAd(s, AdSlot.BestDouble, t, o.earned)?.let {
                        val credited = it.runEarned - s.runEarned
                        s = it
                        tl.activeP += credited
                        tl.ads++
                        audit?.adAward(AdSlot.BestDouble, credited)
                    } ?: ads.refund()
                }
                if (s.energy == 0) shop.useTicket(s, CreditItem.Battery, t)?.let { s = it }
                if (payer.pass && AdSlot.Battery in slots && s.energy == 0) {
                    shop.watchAd(s, AdSlot.Battery, t)?.let { s = it; tl.ads++; audit?.adAward(AdSlot.Battery, java.math.BigInteger.ZERO) }
                }
                // Five taps per second while the drill overlay has the board locked.
                if ((screenOnly || actor == Actor.Player) && o.beats.any { it is Beat.Swiped && it.row.event == EventCard.Drill }) {
                    repeat(50) { i ->
                        val tapAt = if (screenOnly) t + (i + 1) * 200L else t
                        if (!screenOnly || tapAt <= end) s = engine.tapDrill(s, tapAt)
                    }
                }
                tl.activeP += o.earned
                val (spent, bought) = spend(s, DeckPolicy.Synergy)
                s = spent
                if (screenOnly) {
                    val upgrades = bought.count { it == "강화" }
                    if (upgrades > 0) s = meta.upgraded(s, upgrades, t)
                    for (purchase in bought) {
                        val action = when (purchase.substringBefore(':')) {
                            "카드" -> "buyEvent"
                            "판" -> "buyAssist"
                            "프로" -> "buyPro"
                            else -> continue
                        }
                        audit?.screenAction(action, purchase.substringAfter(':'))
                    }
                }
                if (bought.isNotEmpty() || o.beats.any { it is Beat.LevelUp }) s = refreshDeck(s)
                while (s.level > everLevel) {
                    everLevel++
                    levels[everLevel]?.forEach { note(it) }
                }
                observe()
                if (reachedTarget(day)) break@play
                if (tl.lastAppDay == null && LAST_APP in s.appLevels) tl.lastAppDay = dayOf(t)
                // Exploit probe: from week 2, reset the moment Lv25 is reached.
                if (spam && day >= 7 && s.level >= engine.rules.resetLevel && engine.backupFilesNow(s) > 0) {
                    s = chooseDeck(spendBackup(engine.factoryReset(s, t)!!), DeckPolicy.Synergy)
                    tl.resetDays += dayOf(t)
                }
            }
            audit?.endSession()
            if (screenOnly && s.account.missionDay < com.graviton94.notificationcleaner.core.dayOf(t)) {
                val claimed = meta.claimMissions(shop, s, t) ?: s
                if (claimed !== s) audit?.screenAction("claimMissions")
                s = claimed
            }
            if (screenOnly && claimBookRewards && s.account.claimedBook.size < CollectionRewards.milestones.keys.count { it <= Catalog.apps.size }) {
                for (at in CollectionRewards.milestones.keys) {
                    val claimed = CollectionRewards.claimBook(s, at, shop) ?: continue
                    s = claimed
                    audit?.screenAction("claimBook", at.toString())
                }
            }
            stallSessions = if (s.level > levelAtStart) 0 else stallSessions + 1
            if (traceDays && seed == traceSeed && resets && day in 4..6) println("  d$day s$session lv ${s.level} P ${s.runEarned.toString().length}자리 지갑 ${s.wallet.toString().length} 덱 ${s.active.size} 백업 ${s.backup.files}/${s.backup.lifetime} 초기화 ${s.backup.resets} 배터리 ${s.energy} 속도 ${s.earnRate} 부스트 ${s.account.boostUntil - t} 레벨P ${s.runEarned}")
            // Reset when it raises the lifetime-file bonus by 60% or more, or when the run has stalled for 3 sessions.
            val offer = engine.backupFilesNow(s)
            val pct = engine.rules.lifetimeFilePercent
            val gain = (100.0 + pct * (s.backup.lifetime + offer)) / (100.0 + pct * s.backup.lifetime)
            sinceReset++
            // 2nd prestige: sync at the wall, when resets have stopped paying for a while.
            val gb = engine.cloudGbNow(s)
            if (resets && gb > 0 && sinceReset >= 4 && stallSessions >= 2) {
                if (traceResets && seed == traceSeed) println("  동기화 d${"%.1f".format(dayOf(t))} Lv${s.level} +$gb GB 평생 ${s.backup.cloud.lifetime + gb} 이번 주기 파일 ${s.backup.lifetime}")
                tl.lastSyncLevel = s.level
                val synced = (if (screenOnly) meta.sync(s, t) else engine.sync(s, t))!!
                audit?.prestige("sync", s, synced, t, gain, stallSessions, sinceReset)
                s = synced
                tl.syncDays += dayOf(t)
                everSyncs++
                CloudItem.entries.filter { it.minSyncs == everSyncs }.forEach { note("클라우드 ${it.label}") }
                s = refreshDeck(spendBackup(spendCloud(s)))
                stallSessions = 0
                sinceReset = 0
                continue
            }
            if (resets && s.level >= engine.rules.resetLevel && offer > 0 && (gain >= 1.6 || stallSessions >= 3)) {
                if (traceResets && seed == traceSeed) println("  초기화 d${"%.1f".format(dayOf(t))} Lv${s.level} P${s.runEarned.toString().length}자리 +$offer 파일 평생 ${s.backup.lifetime + offer} 가치Lv${s.backup.lv(com.graviton94.notificationcleaner.core.BackupItem.Value)} 유효초기화 ${s.backup.resets} 덱 ${s.active.size} 최고티어 ${s.appLevels.keys.maxOf { com.graviton94.notificationcleaner.core.Catalog.app(it).tier }} 정체 $stallSessions")
                tl.lastResetLevel = s.level
                s = shop.useTicket(s, CreditItem.BackupBoost, t) ?: if (payer.monthlyData > 0) shop.buyItem(s, CreditItem.BackupBoost, t) ?: s else s
                val reset = (if (screenOnly) meta.reset(s, t) else engine.factoryReset(s, t))!!
                audit?.prestige("reset", s, reset, t, gain, stallSessions, sinceReset)
                s = reset
                tl.resetDays += dayOf(t)
                everResets++
                if (s.backup.cloud.syncs == 0) BackupItem.entries
                    .filter { it.minResets == everResets }
                    .forEach { note("백업 ${it.label}") }
                if (s.backup.cloud.syncs == 0 && s.backup.resets == engine.rules.secondEventSlotResets) {
                    note("백업 두 번째 이벤트 슬롯 +1")
                }
                s = refreshDeck(spendBackup(spendCloud(s)))
                stallSessions = 0
                sinceReset = 0
            }
        }
        tl.runDigitsByDay[day + 1] = s.runEarned.toString().length
        val week = day / 7 + 1
        tl.bestLevelByWeek[week] = maxOf(s.backup.bestLevel, s.level, tl.bestLevelByWeek[week] ?: 0)
        tl.runPeakByWeek[week] = weekRunPeak
        if (day % 7 == 6) weekRunPeak = 1
        tl.digitsByWeek[week] = s.bestSwipe.toString().length
        tl.lifetimeByWeek[week] = s.backup.lifetime
        audit?.endDay(day + 1, s)
        audit?.dayEconomy(day + 1, s, tl)
        if (audit != null && reachedTarget(day + 1)) break
        if (stopPaidAfterDay != null && payer != Payer.Free && day + 1 >= stopPaidAfterDay && tl.lastAppDay != null) break
    }
    tl.finalLevel = s.level
    tl.credits = s.account.creditsFree
    tl.bought = s.account.creditsBought
    tl.dataSpent = s.account.creditsSpent
    tl.dataBalance = s.account.credits
    tl.vouchers = s.account.starVouchers
    tl.premiumByApp.putAll(s.account.premium)
    s.account.premium.forEach { (id, n) -> tl.stars.merge(Catalog.app(id).tier, n, Int::plus) }
    return tl
}

/** Rewarded ads a profile still watches today. */
class AdBudget(private val perDay: Int) {
    private var day = -1L
    private var left = 0
    fun take(t: Long): Boolean {
        val d = (t - START) / DAY
        if (d != day) { day = d; left = perDay }
        if (left <= 0) return false
        left--; return true
    }
    fun refund() { left++ }
}

/** Daily claims, passes, ads, tickets, data spending and premium stars for a [payer] at the start of a session. */
private fun economy(state: GameState, t: Long, session: Int, payer: Payer, tl: Timeline, slots: Set<AdSlot>, ads: AdBudget, audit: DailyAudit?): GameState {
    var s = shop.claimAttendance(state, t)
    if (!screenOnly) s = shop.claimMissions(s, t)
    tl.vouchersEarned += s.account.starVouchers - state.account.starVouchers
    if (payer.pass) {
        if (!shop.passActive(s, t)) s = if (payer.speed != null) shop.buySpeedPass(s, t, payer.speed) else shop.buyPass(s, t)
        s = shop.claimPassDaily(s, t)
    }
    if (payer.monthlyData > 0 && session == 0 && dayOf(t).toLong() % 30 == 0L) s = shop.credit(s, payer.monthlyData * if (dayOf(t) < 1) 2 else 1, bought = true)
    fun ad(slot: AdSlot) {
        if (slot !in slots || shop.adsLeft(s, slot, t) <= 0 || !ads.take(t)) return
        shop.watchAd(s, slot, t)?.let {
            audit?.adAward(slot, it.runEarned - s.runEarned)
            s = it
            tl.ads++
        } ?: ads.refund()
    }
    // A steady ad watcher's day: boost in two sessions, one time warp; a pass claims every placement.
    // Steady watcher (5 a day): boost, two overclocks, a warp, plus the offline double on return.
    if (session == 0) { if (!shop.boostActive(s, t)) ad(AdSlot.Boost); ad(AdSlot.Warp) }
    if (session == 1 || session == 3) ad(AdSlot.Overclock)
    if (payer.pass && session == 2 && !shop.boostActive(s, t)) ad(AdSlot.Boost)
    if (payer.pass && session == 1) { ad(AdSlot.CreditBox); ad(AdSlot.FreeUpgrade) }
    // Attendance tickets.
    if (!shop.boostActive(s, t)) shop.useTicket(s, CreditItem.Boost, t)?.let { s = it }
    while (true) s = shop.useTicket(s, CreditItem.Warp, t) ?: break
    // Spenders: boost every session, then up to a quarter of the daily warp cap each session, stars with the rest.
    if (payer.monthlyData > 0) {
        if (!shop.boostActive(s, t)) shop.buyItem(s, CreditItem.Boost, t)?.let { s = it }
        // Data budget per day goes to 오버클럭 hours (time is what the curve needs), a warp or two with the rest.
        val perDay = payer.monthlyData / 30
        val hours = (perDay * 6 / 10 / shop.price(CreditItem.Overclock)).toInt()
        // Cover the day: buy this session's share of overclock hours, keeping it running into the offline gap.
        repeat((hours + session) / 4) { shop.buyItem(s, CreditItem.Overclock, t)?.let { s = it } }
        if (session == 0) shop.buyItem(s, CreditItem.Warp, t)?.let { s = it }
    }
    while (true) s = shop.useTicket(s, CreditItem.Overclock, t) ?: break
    // Star vouchers go to the deck's best app first.
    while (s.account.starVouchers > 0) {
        val id = s.active.filter { s.account.stars(it) < engine.rules.voucherMaxStars && shop.premiumOpen(s, it) }.maxByOrNull { Catalog.app(it).tier } ?: break
        s = shop.useStarVoucher(s, id) ?: break
    }
    // Premium: the next star of the deck's best app, as soon as it is affordable.
    while (true) {
        val id = s.active.filter { shop.premiumOpen(s, it) }.maxByOrNull { Catalog.app(it).tier * 10 - s.account.stars(it) } ?: break
        s = shop.upgradePremium(s, id) ?: break
    }
    return s
}

private fun dayOf(ms: Long) = (ms - START) / DAY.toDouble()

fun monetization(days: Int, seeds: Long, profiles: List<Profile>) {
    val plan = DayPlan()
    println("=== 과금 프로필 ${days}일 · 시드 $seeds · ${profiles.size}개 프로필 병렬")
    fun d(x: Double?) = if (x == null || x.isNaN()) "-" else "%.1f일".format(x)
    // Every (profile, seed) run is independent: spread them over all cores.
    val jobs = profiles.flatMap { p -> (1L..seeds).map { p to it } }
    val done = jobs.parallelStream().map { (p, seed) -> p to playDays(seed, days, plan, true, payer = p.payer, slots = p.slots) }.toList()
    for (p in profiles) {
        val runs = done.filter { it.first == p }.map { it.second }
        val off = p50(runs.map { val tot = it.offlineP + it.activeP; if (tot.signum() == 0) 0.0 else it.offlineP.toBigDecimal().divide(tot.toBigDecimal(), java.math.MathContext.DECIMAL64).toDouble() * 100 })
        println("[${p.label}] 첫 초기화 ${d(p50(runs.mapNotNull { it.resetDays.firstOrNull() }))} · 마지막 앱 ${d(p50(runs.mapNotNull { it.lastAppDay }))} (${runs.count { it.lastAppDay != null }}/$seeds) · 오프라인 ${"%.0f%%".format(off)}" +
            " · 무료 데이터 ${p50(runs.map { it.credits.toDouble() }).toLong()} · 구매 데이터 ${p50(runs.map { it.bought.toDouble() }).toLong()} · 광고/일 ${"%.1f".format(p50(runs.map { it.ads.toDouble() / days }))}")
        println("      주별 최고 레벨 " + runs[0].bestLevelByWeek.keys.joinToString(" · ") { w -> "${w}주 ${p50(runs.map { (it.bestLevelByWeek[w] ?: 0).toDouble() }).toInt()}" })
    }
}

/** Standard profiles, or "ablate": no ads, each ad placement alone, all ads. */
fun profilesFor(arg: String?): List<Profile> = when (arg) {
    null, "all" -> Payer.entries.map { Profile(it.label, it) }
    "ablate" -> listOf(Profile("광고 없음", Payer.Pass, emptySet())) + AdSlot.entries.map { Profile("${it.label}만", Payer.Pass, setOf(it)) } + Profile("광고 전부", Payer.Pass)
    else -> when {
        arg.startsWith("slot:") -> listOf(Profile(arg, Payer.Pass, setOf(AdSlot.valueOf(arg.removePrefix("slot:")))))
        arg == "screen" -> profilesFor("all").map { it.copy(slots = it.slots - AdSlot.OfflineDouble) }
        arg.startsWith("screen:") -> profilesFor(arg.removePrefix("screen:")).map { it.copy(slots = it.slots - AdSlot.OfflineDouble) }
        else -> arg.split(",").map { Payer.valueOf(it) }.map { Profile(it.label, it) }
    }
}

fun daily(days: Int, seeds: Long) {
    val plan = DayPlan()
    println("=== 일상 플레이 ${days}일 · 하루 ${plan.sessions}회 × ${plan.sessionMin}분 직접 + 오프라인 · 시드 $seeds · 시너지 덱")
    val withReset = (1L..seeds).map { playDays(it, days, plan, true) }
    val noReset = (1L..seeds).map { playDays(it, minOf(days, 30), plan, false) }
    fun d(x: Double?) = if (x == null || x.isNaN()) "-" else "%.1f일".format(x)
    println("[환생] 첫 초기화 ${d(p50(withReset.mapNotNull { it.resetDays.firstOrNull() }))} · 초기화 횟수 ${p50(withReset.map { it.resetDays.size.toDouble() }).toInt()}" +
        " · " + (2..10).joinToString(" · ") { tr -> "${tr}티어 ${d(p50(withReset.mapNotNull { it.tierDay[tr] }))}" } +
        " · 마지막 앱 ${d(p50(withReset.mapNotNull { it.lastAppDay }))} (${withReset.count { it.lastAppDay != null }}/$seeds)")
    println("       주별 최고 레벨 " + withReset[0].bestLevelByWeek.keys.joinToString(" · ") { w -> "${w}주 ${p50(withReset.map { (it.bestLevelByWeek[w] ?: 0).toDouble() }).toInt()}" })
    println("       주별 최고점 자릿수 " + withReset[0].digitsByWeek.keys.joinToString(" · ") { w -> "${w}주 ${p50(withReset.map { (it.digitsByWeek[w] ?: 0).toDouble() }).toInt()}" })
    println("[환생 없음 30일] 최고 레벨 ${p50(noReset.map { (it.bestLevelByWeek.values.maxOrNull() ?: 0).toDouble() }).toInt()} · " +
        (2..10).joinToString(" · ") { tr -> "${tr}티어 ${d(p50(noReset.mapNotNull { it.tierDay[tr] }))}" } + " · 마지막 앱 ${noReset.count { it.lastAppDay != null }}/$seeds")
    // Unlock density: gaps between first-time unlocks.
    val gapsMin = withReset.flatMap { tl -> tl.unlocks.zipWithNext { a, b -> b.first - a.first } }
    val gapsDay = withReset.flatMap { tl -> tl.unlocks.zipWithNext { a, b -> b.second - a.second } }
    println("[오프라인 몫] P 중 오프라인 비율 중앙값 " + "%.0f%%".format(p50(withReset.map { val tot = it.offlineP + it.activeP; if (tot.signum() == 0) 0.0 else it.offlineP.toBigDecimal().divide(tot.toBigDecimal(), java.math.MathContext.DECIMAL64).toDouble() * 100 })))
    println("[환생 없음] 회차 누적 P 자릿수 " + listOf(1, 2, 3, 5, 7, 10, 14, 21, 30).joinToString(" · ") { d -> "${d}일 ${p50(noReset.mapNotNull { it.runDigitsByDay[d]?.toDouble() }).toInt()}" })
    println("[해금 밀도] 해금 ${p50(withReset.map { it.unlocks.size.toDouble() }).toInt()}개 · 간격(직접 플레이) p50 ${fmtMin(pct(gapsMin, .5))} p90 ${fmtMin(pct(gapsMin, .9))} 최대 ${fmtMin(gapsMin.maxOrNull() ?: 0.0)} · 달력 최대 공백 ${d(gapsDay.maxOrNull())}")
    if (System.getenv("SIM_SPAM") != null) {
        val spamRuns = (1L..seeds).map { playDays(it, days, plan, true, spam = true) }
        println("[단타 탐지] 평생 백업파일 주별 — 일반: " + withReset[0].lifetimeByWeek.keys.joinToString(" · ") { w -> "${w}주 ${p50(withReset.map { (it.lifetimeByWeek[w] ?: 0).toDouble() }).toLong()}" })
        println("            단타(2주차부터 Lv25 즉시 초기화): " + spamRuns[0].lifetimeByWeek.keys.joinToString(" · ") { w -> "${w}주 ${p50(spamRuns.map { (it.lifetimeByWeek[w] ?: 0).toDouble() }).toLong()}" })
        println("            단타 최고 레벨: " + spamRuns[0].bestLevelByWeek.keys.joinToString(" · ") { w -> "${w}주 ${p50(spamRuns.map { (it.bestLevelByWeek[w] ?: 0).toDouble() }).toInt()}" } + " · 초기화 ${p50(spamRuns.map { it.resetDays.size.toDouble() }).toInt()}회 · 마지막 앱 ${spamRuns.count { it.lastAppDay != null }}/$seeds")
    }
    val tl = withReset[0]
    println("  시드1 해금 순서: " + tl.unlocks.joinToString(" | ") { "${d(it.second)} ${it.third}" })
    println("  시드1 초기화: " + tl.resetDays.joinToString(" ") { d(it) })
    println("[동기화] 첫 동기화 ${d(p50(withReset.mapNotNull { it.syncDays.firstOrNull() }))} · 횟수 ${p50(withReset.map { it.syncDays.size.toDouble() }).toInt()} · 시드1 " + tl.syncDays.joinToString(" ") { d(it) })
}


/**
 * Update check: [launchDays] of play on launch content, then [pack] switches on and play continues for
 * [extraDays]. Reports whether growth restarts and when the pack's top app opens.
 */
fun updateCheck(launchDays: Int, extraDays: Int, seeds: Long, pack: com.graviton94.notificationcleaner.core.ContentPack) {
    val plan = DayPlan()
    println("=== 업데이트 검증 · ${pack.label} · 출시 ${launchDays}일 뒤 적용, ${extraDays}일 더 · 시드 $seeds")
    val runs = (1L..seeds).map { playDays(it, launchDays + extraDays, plan, true, update = launchDays to pack) }
    Catalog.packs = setOf(pack)
    val top = Catalog.apps.maxBy { it.unlockLevel }
    if (Catalog.packs.isNotEmpty()) Catalog.packs = emptySet()  // shared global: only touch it when needed (parallel runs)
    fun d(x: Double?) = if (x == null || x.isNaN()) "-" else "%.1f일".format(x)
    println("적용 시점 최고 레벨 ${p50(runs.map { it.updateLevel.toDouble() }).toInt()} · 11티어 첫 앱 ${d(p50(runs.mapNotNull { it.tierDay[11] }))} · 팩 마지막 앱(${top.name} Lv${top.unlockLevel}) ${d(p50(runs.mapNotNull { it.lastAppDay }))} (${runs.count { it.lastAppDay != null }}/$seeds)")
    println("주별 최고 레벨 " + runs[0].bestLevelByWeek.keys.joinToString(" · ") { w -> "${w}주 ${p50(runs.map { (it.bestLevelByWeek[w] ?: 0).toDouble() }).toInt()}" })
    println("동기화 횟수 ${p50(runs.map { it.syncDays.size.toDouble() }).toInt()} · 적용 후 동기화 ${p50(runs.map { tl -> tl.syncDays.count { it >= launchDays }.toDouble() }).toInt()}")
}
