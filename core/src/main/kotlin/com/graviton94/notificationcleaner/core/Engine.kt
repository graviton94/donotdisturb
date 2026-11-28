@file:UseSerializers(BigIntegerSerializer::class)

package com.graviton94.notificationcleaner.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

import java.math.BigInteger
import java.math.BigInteger.ZERO

enum class RowKind { App, Wild, Spam, Call, Event, Alert }

/** System alerts (rules v3.1): they never merge; leaving them costs something, swiping them helps. */
enum class Alert(val label: String) { LowBattery("배터리 부족"), Storage("저장공간 부족"), Crash("앱 충돌") }

/** One notification row on the board. Board counts stay small; value multipliers make P large. */
@Serializable
data class Row(
    val id: Long,
    val kind: RowKind,
    val app: String? = null,
    val event: EventCard? = null,
    val count: Int = 0,
    val threshold: Int = 0,
    /** Spam: swipe number at which it copies itself if still on the board. */
    val copyAt: Int = 0,
    /** Event rows: swipe number at which they leave the board (0 = stays). */
    val expireAt: Int = 0,
    /** Alert rows: which alert. For 앱 충돌, [app] is the app that stopped. */
    val alert: Alert? = null,
) {
    val merges: Boolean get() = kind == RowKind.App || kind == RowKind.Wild
}

/** What survives a factory reset: backup files and what they bought. */
@Serializable
data class Backup(
    val files: Long = 0,
    val items: Map<BackupItem, Int> = emptyMap(),
    val resets: Int = 0,
    /** All backup files ever earned. Each adds RuleSet.lifetimeFilePercent to every notification value. */
    val lifetime: Long = 0,
    /** Deepest level any run reached. */
    val bestLevel: Int = 0,
    /** Best run P ever: backup files pay only for beating it. */
    val bestRunP: BigInteger = BigInteger.ZERO,
    /** 2nd prestige: survives a sync together with the app unlock records. */
    val cloud: Cloud = Cloud(),
    /** Highest level earned before the app unlock gates moved in rules v5. */
    val legacyAppUnlockLevel: Int = 0,
) {
    fun lv(item: BackupItem): Int = items[item] ?: 0
}

/** What survives a 클라우드 동기화: GB and what they bought. */
@Serializable
data class Cloud(
    val gb: Long = 0,
    val items: Map<CloudItem, Int> = emptyMap(),
    /** Syncs that paid GB. */
    val syncs: Int = 0,
    val lifetime: Long = 0,
    /** Most backup files one cycle earned: GB pay only for beating it. */
    val bestCycleFiles: Long = 0,
    /** Paying factory resets made while a content pack was on: pack items open one level per 3 of them. */
    val packResets: Int = 0,
    /** Credits, premium stars, ads and passes: permanent, survives everything (Monetization.kt). */
    val account: Account = Account(),
) {
    fun lv(item: CloudItem): Int = items[item] ?: 0
}

@Serializable
data class GameState(
    val rulesVersion: Int,
    /** Top to bottom. */
    val rows: List<Row>,
    /** Owned apps → upgrade level. */
    val appLevels: Map<String, Int>,
    /** The app deck: owned apps allowed to notify. */
    val active: List<String>,
    val events: Set<EventCard> = emptySet(),
    /** The event deck: owned cards that appear on the board. */
    val equipped: List<EventCard> = emptyList(),
    val assists: Map<Assist, Int> = emptyMap(),
    val gauge: Int = 0,
    val energy: Int,
    val nextChargeAt: Long = 0,
    val wallet: BigInteger = ZERO,
    /** P earned in this run; converts into backup files on a factory reset. */
    val runEarned: BigInteger = ZERO,
    val lastEarned: BigInteger = ZERO,
    val bestSwipe: BigInteger = ZERO,
    val bestChain: Int = 0,
    val level: Int = 1,
    val swipes: Int = 0,
    val nextOverloadAt: Long = 0,
    val nextMajorEventAt: Long = 0,
    val previousMajorEvent: String? = null,
    val autoPausedUntil: Long = 0,
    val regionCleared: Map<Region, BigInteger> = emptyMap(),
    val categoryCleared: Map<Category, BigInteger> = emptyMap(),
    /** Apps queued to arrive next, in order (from @모두 공지). */
    val pattern: List<String> = emptyList(),
    val rng: Long,
    val nextId: Long,
    val backup: Backup = Backup(),
    /** Recent income, for offline settlement: P per minute (moving average over swipes). */
    val earnRate: BigInteger = ZERO,
    /** Apps whose pro version was bought this run. */
    val pro: Set<String> = emptySet(),
    /** 앱스토어 세일: the next app bought costs half. */
    val saleNext: Boolean = false,
    val lastSwipeAt: Long = 0,
    // Unique-effect state (docs/bible/15-unique-effects.md)
    /** 배다른민족: swipes left that cost no battery. */
    val freeSwipes: Int = 0,
    /** 트래블로까: the next swipe with a burst after buying an app pays ×3. */
    val boostNext: Boolean = false,
    /** 아고다다: the first burst after an offline settlement pays ×3. */
    val returnBurst: Boolean = false,
    /** 넷플러그: swipes left after returning that pay ×2. */
    val returnSwipes: Int = 0,
    /** 모모모 · 에어방앤방: day number of each app's last daily bonus. */
    val dailyBonusDay: Map<String, Long> = emptyMap(),
    /** 메이버: the trending category (×2) for the current 10 swipes. */
    val trend: Category? = null,
    // Timed cards and alerts (rules v3.1)
    /** 생일 축하: day number it last arrived. */
    val birthdayDay: Long = -1,
    /** 알림훈련: running until this time; taps so far (1% each, up to 100). */
    val drillUntil: Long = 0,
    val drillTaps: Int = 0,
    /** 방해 금지: active until this time; uses today. */
    val dndUntil: Long = 0,
    val dndDay: Long = -1,
    val dndUses: Int = 0,
    /** 앱 충돌: the restarted app's next burst pays ×2. */
    val crashBoost: String? = null,
    // Unique effects, phase 2
    /** P of the last swipe that burst something (음성 녹음 · 갤러리 echoes). */
    val lastBurstP: BigInteger = ZERO,
    /** 음성 녹음: swipes left that echo 10% of [lastBurstP]. */
    val echoSwipes: Int = 0,
    /** 쿠빵: swipes left of the dawn-delivery ×2 after a night return. */
    val nightSwipes: Int = 0,
    /** 우바: the next auto-clean waits 2 s less. */
    val rideBoost: Boolean = false,
    /** 와이: stacks this run (+2% to social apps each, max 15). */
    val whyStacks: Int = 0,
) {
    fun assist(a: Assist): Int = assists[a] ?: 0
}

/** How a notification was cleared, for value effects. */
data class EarnCtx(
    val direct: Boolean = false,
    val wave: Boolean = false,
    val auto: Boolean = false,
    /** 로빈훗훗: the swipe's gamble multiplier in ‰. */
    val gamblePermille: Int = 1_000,
    /** 핀트레스트: rows of this app on the board. */
    val pile: Int = 0,
    /** 수입 부스트 is running. */
    val boosted: Boolean = false,
    val effects: MutableSet<String>? = null,
)

/** What happened, in order. The UI replays these; the state is already final. */
data class BurstSynergy(val kind: String, val key: String, val percent: Int)

enum class ChainEndReason { Natural, Spam, Wave, Cap }

sealed interface Beat {
    data class Swiped(val row: Row, val earned: BigInteger) : Beat
    data class Merged(val into: Row, val fromId: Long) : Beat
    data class Burst(
        val row: Row, val chain: Int, val earned: BigInteger,
        val valueApp: String, val synergy: BurstSynergy? = null, val effects: Set<String> = emptySet(),
    ) : Beat
    data class Wave(val index: Int, val chain: Int, val cleared: BigInteger, val earned: BigInteger) : Beat
    data class Spawned(val row: Row) : Beat
    data class Spawn(val row: Row, val slot: Int, val chain: Int) : Beat
    data class ChainEnd(val reason: ChainEndReason, val chain: Int, val bursts: Int, val steps: Int) : Beat
    data class FreeUpgrade(val app: String, val level: Int) : Beat
    data class LevelUp(val level: Int) : Beat
    /** 과열 보호: P past the per-swipe level cap was lost. */
    data class Capped(val lost: BigInteger) : Beat
}

data class SwipeOutcome(
    val state: GameState,
    val beats: List<Beat>,
    val earned: BigInteger,
    /** Scoring chain number, including chain-number effects and overload waves. */
    val chain: Int,
    val cleared: BigInteger,
    val overload: Boolean,
    /** P lost to the jackpot cap (RuleSet.swipeLevelCap). */
    val capped: BigInteger = ZERO,
    val liveEventExtra: BigInteger = ZERO,
)

enum class Actor { Player, Auto }

/** SplitMix64. The whole state of the generator is one Long kept in GameState. */
internal class Rng(var seed: Long) {
    fun next(bound: Int): Int {
        seed += -0x61c8864680b583ebL
        var z = seed
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        z = z xor (z ushr 31)
        return ((z ushr 1) % bound).toInt()
    }

    fun weighted(weights: List<Int>): Int {
        var r = next(weights.sum())
        weights.forEachIndexed { i, w -> if (r < w) return i; r -= w }
        return weights.lastIndex
    }
}

data class OfflineBonusStatus(
    val rewardBonusPermille: Int,
    val ultraBonusPermille: Int,
    val adLeftMs: Long,
    val passLeftMs: Long,
    val ultraLeftMs: Long,
) {
    val rewardActive get() = adLeftMs > 0 || passLeftMs > 0
    val ultraWithPassMultiplierPermille get() =
        ((1_000L + rewardBonusPermille) * (1_000L + ultraBonusPermille) / 1_000L).toInt()
    val multiplierPermille get() = when {
        ultraLeftMs > 0 -> ultraWithPassMultiplierPermille
        rewardActive -> 1_000 + rewardBonusPermille
        else -> 1_000
    }
}

data class OfflineSettlement(
    val state: GameState,
    val paid: BigInteger,
    val basePaid: BigInteger,
    val bonus: OfflineBonusStatus,
)

class Engine(val rules: RuleSet = RuleSet(), val tuning: LiveTuning = LiveTuning()) {

    private val hundred = BigInteger.valueOf(100)

    // Derived values -------------------------------------------------------------------------

    /** SplitMix64 finaliser: effect rolls that must not touch the board's random sequence. */
    private fun mix(x: Long): Long {
        var z = x + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** Unique effects work only while the app is in the deck. */
    fun on(s: GameState, appId: String) = appId in s.active

    fun energyCap(s: GameState) = rules.energyCap + 2 * s.backup.lv(BackupItem.Battery) + if (on(s, "grab")) 2 else 0
    fun chargeMs(s: GameState, now: Long = Long.MIN_VALUE) = (rules.chargeMs - 200L * s.backup.lv(BackupItem.Battery)).let {
        if (s.rows.any { r -> r.alert == Alert.LowBattery }) it * 13 / 10 else it  // 배터리 부족 left on the board
    }.let { base -> maxOf(base * 1_000 / speedPermille(s, now), minOf(base, rules.minChargeMs)) }  // a thumb can't go faster than this

    /** [x] × [permille] / 1000 without overflowing: late-game backup files reach 10^16 and more. */
    private fun scalePermille(x: Long, permille: Long): Long = if (permille == 1_000L) x else x / 1_000 * permille + x % 1_000 * permille / 1_000

    /** 속도 구독 bonus on backup files and GB, ‰. */
    fun prestigePermille(s: GameState, now: Long): Long {
        val a = s.backup.cloud.account
        return if (a.speedPass != null && now < a.speedPassUntil) a.speedPass.prestigePermille.toLong() else 1_000L
    }

    /** 오버클럭 / 속도 구독 (Monetization.kt): the whole game runs this much faster, ‰ (the faster one wins). */
    fun speedPermille(s: GameState, now: Long): Long {
        val a = s.backup.cloud.account
        val oc = if (now < a.overclockUntil) rules.overclockPermille.toLong() else 1_000L
        val sub = if (a.speedPass != null && now < a.speedPassUntil) a.speedPass.speedPermille.toLong() else 1_000L
        return maxOf(oc, sub)
    }
    fun autoIntervalMs(s: GameState): Long {
        val reductions = 300L * s.backup.lv(BackupItem.AutoClean) + 500L * s.backup.cloud.lv(CloudItem.CloudAuto) +
            (if (on(s, "settings")) 300L else 0L) + (if (on(s, "ai")) 500L else 0L)
        return (rules.autoIntervalMs - reductions).coerceAtLeast(500L)
    }
    fun overloadDepth(s: GameState) = 1 + s.assist(Assist.Overload) + s.backup.lv(BackupItem.DeepOverload)
    fun rowCount(s: GameState) = minOf(
        rules.maxRows,
        rules.baseRows + maxOf(0, s.active.size - 4) / rules.appsPerExtraRow + s.assist(Assist.Board) + s.backup.lv(BackupItem.Board),
    ) - if (s.rows.any { it.alert == Alert.Storage }) 1 else 0  // 저장공간 부족 left on the board

    /** 방해 금지 doubles auto speed while it runs. */
    fun autoIntervalMs(s: GameState, now: Long): Long {
        val interval = autoIntervalMs(s) - if (s.rideBoost) 2_000L else 0L
        val dnd = if (now < s.dndUntil) 2 else 1
        return (interval * 1_000 / (dnd * speedPermille(s, now))).coerceAtLeast(500L)
    }
    fun sandwichPercent(s: GameState) = rules.sandwichPercent + rules.sandwichPerExtraApp * maxOf(0, s.active.size - 4) +
        rules.sandwichPerAssist * s.assist(Assist.Sandwich) + 2 * s.backup.lv(BackupItem.Sandwich) + if (on(s, "feed")) 10 else 0
    fun wildPercent(s: GameState) = rules.wildPerAssist * s.assist(Assist.Wild) + s.backup.lv(BackupItem.Wild)
    fun maxSlots(s: GameState): Int {
        val slots = rules.maxSlots(s.level, s.backup)
        // A saved Lv73 deck may already occupy the slot now earned at Lv74.
        return if (s.level == 73 && s.active.size == slots + 1) slots + 1 else slots
    }
    fun minSlots(s: GameState) = minOf(rules.minSlots(s.level), s.appLevels.size)
    fun eventSlots(s: GameState) = rules.eventSlots(s.level, s.backup)
    fun bestTier(s: GameState) = s.active.maxOf { Catalog.app(it).tier }
    fun bestApp(s: GameState) = s.active.maxBy { Catalog.app(it).tier * 1_000 + (s.appLevels[it] ?: 1) }
    fun backupFilesNow(s: GameState) = rules.backupFiles(s.runEarned, s.level, s.backup)

    /** The deck's synergy bonus % for an app's region and category (0 when alone). */
    fun synergy(s: GameState, appId: String): Pair<Int, Int> {
        val app = Catalog.app(appId)
        // Stock apps have no home market: category synergy only.
        val r = if (app.region == Region.Stock) 0 else s.active.count { Catalog.app(it).region == app.region }
        val c = s.active.count { Catalog.app(it).category == app.category }
        val rs = rules.synergyPercent(r, s.backup, rules.regionSynergySteps).let { if (on(s, "papago")) it * 110 / 100 else it }
        return rs to rules.synergyPercent(c, s.backup, rules.categorySynergySteps)
    }

    /** Regions and categories with 2+ apps in the deck: each multiplies every value by RuleSet.diversityPermille. */
    fun activeSets(s: GameState): Int = DeckGroups(s.active).sets

    private class DeckGroups(active: List<String>) {
        val regions = IntArray(Region.entries.size)
        val categories = IntArray(Category.entries.size)
        init {
            for (id in active) {
                val app = Catalog.app(id)
                regions[app.region.ordinal]++
                categories[app.category.ordinal]++
            }
        }
        val sets = regions.indices.count { it != Region.Stock.ordinal && regions[it] >= 2 } + categories.count { it >= 2 }
    }

    private data class DeckMultiplier(val region: Int, val category: Int, val sets: Int)

    private fun deckMultiplier(s: GameState, app: AppDef, groups: DeckGroups): DeckMultiplier {
        val region = if (app.region == Region.Stock) 0 else groups.regions[app.region.ordinal]
        val rs = rules.synergyPercent(region, s.backup, rules.regionSynergySteps).let { if (on(s, "papago")) it * 110 / 100 else it }
        val cs = rules.synergyPercent(groups.categories[app.category.ordinal], s.backup, rules.categorySynergySteps)
        return DeckMultiplier(rs, cs, maxOf(0, groups.sets - 1))
    }

    private fun digits(n: BigInteger?) = if (n == null || n.signum() <= 0) 1 else n.toString().length

    private fun valueBase(s: GameState, app: AppDef, level: Int = s.appLevels[app.id] ?: 1): Pair<BigInteger, BigInteger> {
        val (num, den) = rules.baseValue(app, level, s.backup)
        val rd = rules.regionDigitPercent * (digits(s.regionCleared[app.region]) - 1)
        val cd = rules.categoryDigitPercent * (digits(s.categoryCleared[app.category]) - 1)
        val pro = if (app.id in s.pro) BigInteger.valueOf(rules.proMultiplier) else BigInteger.ONE
        return num * BigInteger.valueOf((100L + rd) * (100L + cd)) * pro to den * hundred.pow(2)
    }

    private fun valueAfterMultipliers(base: Pair<BigInteger, BigInteger>, count: BigInteger, deck: DeckMultiplier,
        chainNum: Long, fx: Long, premium: Long): BigInteger {
        val thousand = BigInteger.valueOf(1_000)
        var comboNum = BigInteger.valueOf((100L + deck.region) * (100L + deck.category)) *
            BigInteger.valueOf(rules.diversityPermille.toLong()).pow(deck.sets)
        var comboDen = hundred.pow(2) * thousand.pow(deck.sets)
        val cap = BigInteger.valueOf(rules.synergyCapPermille.toLong())
        if (comboNum * thousand > cap * comboDen) { comboNum = cap; comboDen = thousand }
        return count * base.first * comboNum * BigInteger.valueOf(chainNum) * BigInteger.valueOf(fx) * BigInteger.valueOf(premium) /
            (base.second * comboDen * BigInteger.valueOf(10_000_000_000L))
    }

    /** P for [count] notifications of [appId] at chain number [chain]. */
    fun earn(s: GameState, appId: String, count: BigInteger, chain: Int, ctx: EarnCtx = EarnCtx()): BigInteger {
        val app = Catalog.app(appId)
        val link = if (chain > 0 && on(s, "tube")) {
            ctx.effects?.add("tube")
            chain + 1
        } else chain
        val chainNum = 10L + link * (10L + s.backup.lv(BackupItem.ChainBoost) + 2 * s.backup.cloud.lv(CloudItem.CloudChain))
        val premium = rules.premiumPermille(s.account.stars(appId)) * (if (ctx.boosted) rules.boostPermille.toLong() else 1_000L)
        return valueAfterMultipliers(valueBase(s, app), count, deckMultiplier(s, app, DeckGroups(s.active)),
            chainNum, effectPermille(s, app, count, chain, ctx), premium)
    }

    /** Difference between the exact rounded values before and after one app rank, holding the deck fixed. */
    fun upgradeEarnGain(s: GameState, appId: String, count: BigInteger): BigInteger {
        val app = Catalog.app(appId)
        val deck = deckMultiplier(s, app, DeckGroups(s.active))
        val effect = effectPermille(s, app, count, 0, EarnCtx())
        val premium = rules.premiumPermille(s.account.stars(appId)) * 1_000L
        val current = s.appLevels.getValue(appId)
        fun income(level: Int) = valueAfterMultipliers(valueBase(s, app, level), count, deck, 10L, effect, premium)
        return income(current + 1) - income(current)
    }

    /** Compare many decks at one unchanged progression state with the same per-app rounding as [earn]. */
    fun deckValuation(state: GameState, count: BigInteger): (List<String>) -> BigInteger {
        val bases = mutableMapOf<AppDef, Pair<BigInteger, BigInteger>>()
        data class Quote(val app: AppDef, val deck: DeckMultiplier, val effect: Long)
        val quotes = mutableMapOf<Quote, BigInteger>()
        val context = EarnCtx()
        return { active ->
            val s = state.copy(active = active)
            val groups = DeckGroups(active)
            active.fold(ZERO) { sum, id ->
                val app = Catalog.app(id)
                val quote = Quote(app, deckMultiplier(s, app, groups), effectPermille(s, app, count, 0, context))
                sum + quotes.getOrPut(quote) {
                    valueAfterMultipliers(bases.getOrPut(app) { valueBase(state, app) }, count, quote.deck,
                        10L, quote.effect, rules.premiumPermille(state.account.stars(id)) * 1_000L)
                }
            }
        }
    }

    /** Value effects of the deck on one clear of [app], in ‰ (1000 = none). */
    fun effectPermille(s: GameState, app: AppDef, count: BigInteger, chain: Int, ctx: EarnCtx): Long {
        var m = 1_000L
        val id = app.id
        fun times(p: Long, source: String = id) {
            if (p != 1_000L) ctx.effects?.add(source)
            m = m * p / 1_000
        }
        fun countCategory(category: Category, excludingSelf: Boolean = false) =
            s.active.count { (!excludingSelf || it != id) && Catalog.app(it).category == category }
        if (id == "memo" && ctx.direct) times(2_000)
        if (id == "calc" && on(s, id) && chain > 0 && chain % 2 == 0 && !ctx.wave) times(1_500)
        if (id == "pin" && ctx.pile >= 3) times(1_200)
        if (id == "contacts") times(1_000L + 150L * countCategory(Category.Social, excludingSelf = true))
        if (id == "photo") times(1_000L + 150L * countCategory(Category.Media, excludingSelf = true))
        if (id == "career") times(1_000L + 100L * s.active.map { Catalog.app(it).category }.distinct().count { it != app.category })
        if (on(s, "jobs") && app.category == Category.Work && countCategory(Category.Work) >= 3) times(1_200, "jobs")
        if (id == "joox" && ctx.wave) times(1_500)
        if (id == "beat" && ctx.auto && chain > 0 && !ctx.wave) times(1_300)
        if (id == "mail" && chain >= 2) times(1_500)
        if (id == "robin" && chain > 0 && !ctx.wave) times(ctx.gamblePermille.toLong())
        if (on(s, "portal") && s.trend == app.category) times(2_000, "portal")
        if (id == "duo") times(1_000L + 20L * minOf(30, s.backup.cloud.account.streak))  // 듀오링구: login streak
        if (on(s, "why") && app.category == Category.Social) times(1_000L + 20L * s.whyStacks, "why")  // 와이
        return m
    }

    /** Most P this run can still gain in one go without rising more than [steps] levels. */
    fun jackpotRoom(s: GameState, steps: Int): BigInteger =
        if (s.level < rules.jackpotCapFrom) BigInteger.TEN.pow(400) else (rules.levelCeil(s.level + steps + 1, s.backup.lv(BackupItem.FastBoot)) - BigInteger.ONE - s.runEarned).max(ZERO)

    private fun valueApp(s: GameState, row: Row): String = if (row.kind == RowKind.Wild) bestApp(s) else row.app!!

    // Game lifecycle --------------------------------------------------------------------------

    /**
     * First board of a run. Fixed so the first ten seconds teach one thing: the middle row (날씨) sits
     * between two 메모 rows; removing it makes them touch and burst. Counts set the size of the award.
     */
    fun newGame(now: Long, seed: Long, backup: Backup = Backup()): GameState {
        val layout = listOf("alarm" to 1, "weather" to 2, "memo" to 2, "weather" to 1, "memo" to 2, "alarm" to 1, "weather" to 2)
        val rows = layout.mapIndexed { i, (app, n) -> Row(i + 1L, RowKind.App, app, count = n, threshold = rules.threshold) }
        val remembered = Catalog.apps.filter { it.id !in Catalog.starter }.take(backup.lv(BackupItem.RememberedApps)).map { it.id }
        val s = GameState(
            rulesVersion = rules.version, rows = rows,
            appLevels = (Catalog.starter + remembered).associateWith { 1 }, active = Catalog.starter,
            energy = rules.energyCap + 2 * backup.lv(BackupItem.Battery),
            wallet = BigInteger.valueOf(1_250L * backup.lv(BackupItem.StartFunds)),
            rng = seed, nextId = rows.size + 1L, backup = backup,
        )
        return s
    }

    /** Factory reset: converts this run into backup files. Null below the reset level. */
    fun factoryReset(s: GameState, now: Long): GameState? {
        if (s.level < rules.resetLevel) return null
        val files = backupFilesNow(s).let { f -> if (s.backup.cloud.account.backupBoost) scalePermille(f, 10L * (100 + rules.backupBoostPercent)) else f }
            .let { f -> scalePermille(f, prestigePermille(s, now)) }
        // Only a reset that pays files counts toward reset-gated content; empty resets cannot farm the count.
        return newGame(now, s.rng, s.backup.copy(
            files = s.backup.files + files, resets = s.backup.resets + if (files > 0) 1 else 0,
            lifetime = s.backup.lifetime + files, bestLevel = maxOf(s.backup.bestLevel, s.level),
            bestRunP = s.backup.bestRunP.max(s.runEarned),
            legacyAppUnlockLevel = legacyAppUnlockLevel(s),
            cloud = s.backup.cloud.let { c -> if (files > 0 && Catalog.packs.isNotEmpty()) c.copy(packResets = c.packResets + 1) else c }
                .let { c -> c.copy(account = s.rememberCollection().account.copy(backupBoost = false)) },
        )).copy(nextOverloadAt = s.nextOverloadAt, nextMajorEventAt = s.nextMajorEventAt, previousMajorEvent = s.previousMajorEvent)
    }

    /** Backup shop price after 클라우드 할인. */
    fun backupPrice(s: GameState, item: BackupItem): Long {
        val owned = s.backup.lv(item)
        val price = if (item == BackupItem.EventSlot && owned == 1) rules.secondEventSlotPrice else rules.backupPrice(item, owned)
        return maxOf(1L, price * (10 - s.backup.cloud.lv(CloudItem.Discount)) / 10)
    }

    /** Reset count the backup shop gates on: real resets plus 백업 상점 단계 유지. */
    fun shopStage(s: GameState) = s.backup.resets + 3 * s.backup.cloud.lv(CloudItem.KeepShop)

    /** Max level of a backup item; 알림 가치 한도 raises 알림 가치's. */
    fun backupMax(s: GameState, item: BackupItem) = item.max + when {
        item == BackupItem.Value -> 10 * s.backup.cloud.lv(CloudItem.ValueCap)
        item == BackupItem.EventSlot && s.backup.resets >= rules.secondEventSlotResets -> 1
        else -> 0
    }

    fun backupOpen(s: GameState, item: BackupItem) = s.backup.lv(item) < backupMax(s, item) && shopStage(s) >= item.minResets

    fun buyBackup(s: GameState, item: BackupItem): GameState {
        val owned = s.backup.lv(item)
        val price = backupPrice(s, item)
        if (!backupOpen(s, item) || s.backup.files < price) return s
        return s.copy(backup = s.backup.copy(files = s.backup.files - price, items = s.backup.items + (item to owned + 1)))
    }

    /** GB a sync pays now: only what beating the best cycle adds. Includes this run's pending backup files. */
    fun cloudGbNow(s: GameState): Long {
        if (s.level < rules.syncLevel) return 0
        val cycle = s.backup.lifetime + backupFilesNow(s)
        return maxOf(0L, rules.cloudTotal(maxOf(cycle, s.backup.cloud.bestCycleFiles)) - s.backup.cloud.lifetime)
    }

    /**
     * 클라우드 동기화 (2nd prestige): the run and the whole backup layer (files, shop, resets, best run) restart;
     * GB, the cloud shop and app unlocks (best level) stay. Null below [RuleSet.syncLevel].
     */
    fun sync(s: GameState, now: Long): GameState? {
        if (s.level < rules.syncLevel) return null
        val gb = cloudGbNow(s)
        val c = s.rememberCollection().backup.cloud
        val cloud = c.copy(
            // 속도 구독 bonus GB is spendable but not counted in lifetime, so later syncs still pay in full.
            gb = c.gb + scalePermille(gb, prestigePermille(s, now)), syncs = c.syncs + if (gb > 0) 1 else 0, lifetime = c.lifetime + gb,
            bestCycleFiles = maxOf(c.bestCycleFiles, s.backup.lifetime + backupFilesNow(s)),
        )
        return newGame(now, s.rng, Backup(bestLevel = maxOf(s.backup.bestLevel, s.level), cloud = cloud,
            legacyAppUnlockLevel = legacyAppUnlockLevel(s)))
            .copy(nextOverloadAt = s.nextOverloadAt, nextMajorEventAt = s.nextMajorEventAt, previousMajorEvent = s.previousMajorEvent)
    }

    /** A cloud item can be bought now: below max, enough syncs; pack items open one level per 3 resets after the update. */
    fun cloudOpen(s: GameState, item: CloudItem): Boolean {
        val c = s.backup.cloud
        val owned = c.lv(item)
        if (owned >= item.max || c.syncs < item.minSyncs) return false
        if (item.pack != null) return item.pack in Catalog.packs && owned < c.packResets / 3
        return true
    }

    fun buyCloud(s: GameState, item: CloudItem): GameState {
        val c = s.backup.cloud
        val owned = c.lv(item)
        val price = rules.cloudPrice(item, owned)
        if (!cloudOpen(s, item) || c.gb < price) return s
        return s.copy(backup = s.backup.copy(cloud = c.copy(gb = c.gb - price, items = c.items + (item to owned + 1))))
    }

    /** The save's board can be returned to the player before the clock ticks. */
    internal fun restoreBoard(input: GameState): GameState {
        val migrated = if (input.rulesVersion < rules.version) input.copy(
            rulesVersion = rules.version,
            rows = input.rows.map { if (it.merges) it.copy(threshold = rules.threshold) else it },
            backup = input.backup.copy(legacyAppUnlockLevel = legacyAppUnlockLevel(input)),
        ) else input
        return stabilizeBoard(migrated)
    }

    /** Battery recharge up to [now]. */
    fun tick(input: GameState, now: Long): GameState {
        val s = restoreBoard(input)
        val cap = energyCap(s)
        if (s.energy >= cap) return if (s.nextChargeAt == 0L) s else s.copy(nextChargeAt = 0)
        if (s.nextChargeAt == 0L) return s.copy(nextChargeAt = now + chargeMs(s, now))
        if (now < s.nextChargeAt) return s
        val ms = chargeMs(s, now)
        val steps = ((now - s.nextChargeAt) / ms + 1).coerceAtMost((cap - s.energy).toLong()).toInt()
        val energy = s.energy + steps
        return s.copy(energy = energy, nextChargeAt = if (energy >= cap) 0 else s.nextChargeAt + steps * ms)
    }

    // Swipe ----------------------------------------------------------------------------------

    private fun canMerge(s: GameState, a: Row, b: Row): Boolean {
        if (!a.merges || !b.merges) return false
        if (a.app == b.app || a.kind == RowKind.Wild || b.kind == RowKind.Wild) return true
        return on(s, "maps") && (a.app == "maps" || b.app == "maps") &&
            Catalog.app(a.app!!).region != Catalog.app(b.app!!).region
    }

    /** Keep a row as drawn when possible. A substitute keeps its notification count and identity. */
    private fun quietRow(s: GameState, row: Row, rows: List<Row>, above: Row?, below: Row?): Row? {
        fun fits(candidate: Row) = (above == null || !canMerge(s, above, candidate)) &&
            (below == null || !canMerge(s, candidate, below))
        if (fits(row)) return row
        val size = s.active.size
        if (size == 0) return null
        val offset = Math.floorMod(row.id, size.toLong()).toInt()
        for (i in 0 until size) {
            val app = s.active[(offset + i) % size]
            if (rows.any { it.alert == Alert.Crash && it.app == app }) continue
            val candidate = row.copy(kind = RowKind.App, app = app)
            if (fits(candidate)) return candidate
        }
        return null
    }

    /** Check only contacts that change when we swap two notification rows. */
    private fun quietSwap(s: GameState, rows: List<Row>, from: Int): Int? {
        fun fits(to: Int): Boolean {
            fun at(i: Int) = when (i) {
                from -> rows[to]
                to -> rows[from]
                else -> rows.getOrNull(i)
            }
            fun clear(a: Row, b: Row?) = b == null || !canMerge(s, a, b)
            return clear(rows[to], at(from - 1)) && clear(rows[to], at(from + 1)) &&
                clear(rows[from], at(to - 1)) && clear(rows[from], at(to + 1))
        }
        return rows.indices.firstOrNull { it != from && rows[it].merges && fits(it) }
    }

    /** Remove unresolved contacts without reopening a chain, changing the RNG, or granting P. */
    private fun quietRows(s: GameState, original: List<Row>): List<Row> {
        if (!rules.fallingChains) return original  // Historical simulation mode.
        var repaired: MutableList<Row>? = null
        var index = 1
        while (true) {
            val rows = repaired ?: original
            if (index >= rows.size) return rows
            val upper = rows[index - 1]
            val lower = rows[index]
            if (!canMerge(s, upper, lower)) {
                index++
                continue
            }
            // Try replacing a wild first, or the newer of two ordinary notifications.
            val replaceUpperFirst = upper.kind == RowKind.Wild || (lower.kind != RowKind.Wild && upper.id > lower.id)
            val first = if (replaceUpperFirst) index - 1 else index
            val second = if (replaceUpperFirst) index else index - 1
            if (s.level >= rules.stableSandwichFrom) {
                val firstSwap = quietSwap(s, rows, first)
                val from = if (firstSwap != null) first else second
                val to = firstSwap ?: quietSwap(s, rows, second)
                if (to != null) {
                    val writable = repaired ?: original.toMutableList().also { repaired = it }
                    val moved = writable[from]
                    writable[from] = writable[to]
                    writable[to] = moved
                    continue
                }
            }
            fun checked(at: Int) = quietRow(s, rows[at], rows, rows.getOrNull(at - 1), rows.getOrNull(at + 1))
            val preferred = checked(first)
            val alternative = if (preferred == null) checked(second) else null
            val writable = repaired ?: original.toMutableList().also { repaired = it }
            when {
                preferred != null -> writable[first] = preferred
                alternative != null -> writable[second] = alternative
                else -> {
                    // A deck with no legal alternative may leave space. Removing a row exposes a new seam.
                    writable.removeAt(first)
                    index = maxOf(1, first)
                }
            }
        }
    }

    private fun stabilizeBoard(s: GameState): GameState {
        val rows = quietRows(s, s.rows)
        return if (rows === s.rows) s else s.copy(rows = rows)
    }

    fun swipe(input: GameState, rowId: Long, now: Long, actor: Actor = Actor.Player, visuals: Boolean = false): SwipeOutcome? =
        swipe(input, rowId, now, actor, preview = false, visuals = visuals)

    private fun swipe(input: GameState, rowId: Long, now: Long, actor: Actor, preview: Boolean, visuals: Boolean = false): SwipeOutcome? {
        val state = tick(input, now)
        val auto = actor == Actor.Auto
        if (state.energy <= 0) return null
        if (actor == Actor.Auto && now < state.autoPausedUntil) return null
        val index = state.rows.indexOfFirst { it.id == rowId }
        if (index < 0) return null

        val rng = Rng(state.rng)
        val rows = state.rows.toMutableList()
        val beats = mutableListOf<Beat>()
        val removed = rows.removeAt(index)
        var s = state
        var earned = ZERO
        var cleared = ZERO
        var chain = 0
        var gauge = state.gauge
        var pausedUntil = state.autoPausedUntil
        var pattern = state.pattern
        val regionAdd = mutableMapOf<Region, BigInteger>()
        val categoryAdd = mutableMapOf<Category, BigInteger>()
        var energyGain = 0
        var crashBoost = state.crashBoost
        var drillUntil = state.drillUntil
        var drillTaps = state.drillTaps
        var freeSwipes = maxOf(0, state.freeSwipes - if (state.freeSwipes > 0) 1 else 0)
        val usesBattery = state.freeSwipes <= 0
        // 로빈훗훗: one gamble roll per swipe, ×0.5 to ×3 (mean about ×1.4). Hashed from the state so effects never
        // shift the board's random sequence for decks without the app.
        val roll = mix(state.rng xor state.swipes.toLong())
        val rollStep = Math.floorMod(roll, 2_501L).toInt()
        val gamble = if (rollStep < 1_500) 500 + rollStep * 800 / 1_500 else 1_300 + (rollStep - 1_500) * 1_700 / 1_000
        fun pileOf(app: String?) = rows.count { it.app == app }
        fun ctx(wave: Boolean = false, direct: Boolean = false, app: String? = null, effects: MutableSet<String>? = null) =
            EarnCtx(direct = direct, wave = wave, auto = auto, gamblePermille = gamble, pile = pileOf(app),
                boosted = now < state.backup.cloud.account.boostUntil, effects = effects)

        fun track(row: Row, n: BigInteger) {
            if (row.kind != RowKind.App) return
            val app = Catalog.app(row.app!!)
            regionAdd.merge(app.region, n, BigInteger::add)
            categoryAdd.merge(app.category, n, BigInteger::add)
        }

        if (!preview || removed.event == EventCard.GroupText || removed.event == EventCard.Update ||
            removed.event == EventCard.Firmware) when (removed.kind) {
            RowKind.App, RowKind.Wild -> {
                val n = BigInteger.valueOf(removed.count.toLong())
                val e = earn(state, valueApp(state, removed), n, 0, ctx(direct = !auto, app = removed.app))
                earned += e
                cleared += n
                track(removed, n)
                beats += Beat.Swiped(removed, e)
            }
            RowKind.Call -> {
                pausedUntil = 0
                if (on(state, "phone")) energyGain += 2  // 전화: answering recharges
                val e = if (on(state, "meet")) state.earnRate else ZERO  // 주움: joining the call pays a minute of income
                earned += e
                beats += Beat.Swiped(removed, e)
            }
            RowKind.Spam -> beats += Beat.Swiped(removed, ZERO)
            RowKind.Alert -> {
                when (removed.alert!!) {
                    Alert.LowBattery -> energyGain += 4  // 절전 모드
                    Alert.Storage -> {}
                    Alert.Crash -> crashBoost = removed.app
                }
                beats += Beat.Swiped(removed, ZERO)
            }
            RowKind.Event -> {
                var e = ZERO
                when (removed.event!!) {
                    EventCard.Update -> {
                        val app = state.active[rng.next(state.active.size)]
                        val lv = (s.appLevels[app] ?: 1) + 1
                        s = s.copy(appLevels = s.appLevels + (app to lv))
                        beats += Beat.FreeUpgrade(app, lv)
                    }
                    EventCard.AllHands -> {
                        val a = state.active[rng.next(state.active.size)]
                        val others = state.active - a
                        val b = others[rng.next(others.size)]
                        pattern = listOf(a, b, a, b, a)
                    }
                    EventCard.Otp -> e = state.lastEarned * BigInteger.TEN
                    EventCard.Sale -> s = s.copy(saleNext = true)
                    EventCard.GroupText -> for (i in rows.indices) if (rows[i].kind == RowKind.App) rows[i] = rows[i].copy(count = rows[i].count + 1)
                    EventCard.Firmware -> s = s.copy(appLevels = s.appLevels.mapValues { (id, lv) -> if (id in s.active) lv + 1 else lv })
                    EventCard.Lotto -> if (rng.next(2) == 1) e = state.lastEarned * BigInteger.valueOf(1L + rng.next(60))
                    EventCard.Birthday -> e = state.earnRate * BigInteger.TEN
                    EventCard.Drill -> { drillUntil = now + rules.drillMs; drillTaps = 0 }
                    EventCard.Dnd -> {}
                }
                earned += e
                beats += Beat.Swiped(removed, e)
            }
        }

        // Resolve the deletion seam first, then untouched contacts made by falling rows.
        val falling = rules.fallingChains && !preview
        val fallOdds = rules.fallMatchPermille + if (s.level >= rules.lateFallMatchFrom) rules.lateFallMatchBonusPermille else 0
        val fallBoardSize = state.rows.size - 1 // Reserve the original swipe's slot for scheduled cards and alerts.
        val landings = ArrayDeque<Pair<Long, Long>>()
        var nextId = maxOf(state.nextId, (state.rows.maxOfOrNull(Row::id) ?: 0L) + 1)
        var physicalBursts = 0
        var steps = 0
        var stop = ChainEndReason.Natural
        var overload = false
        fun refillChain(): Boolean {
            var spawnedSpam = false
            while (rows.size < fallBoardSize && steps < rules.fallWorkLimit) {
                val below = rows.firstOrNull()
                val spamChance = ((physicalBursts - rules.fallSpamFrom + 1).coerceAtLeast(0) *
                    rules.fallSpamStepPermille).coerceAtMost(rules.fallSpamMaxPermille)
                val row = when {
                    now >= s.dndUntil && spamChance > 0 && rows.count { it.kind == RowKind.Spam } < rules.spamMax &&
                        rng.next(1_000) < spamChance ->
                        Row(nextId++, RowKind.Spam, copyAt = s.swipes + 1 + rules.spamCopyAfterSwipes)
                    pattern.isNotEmpty() -> Row(nextId++, RowKind.App, pattern.first(),
                        count = rng.weighted(rules.countWeights) + 1, threshold = rules.threshold).also { pattern = pattern.drop(1) }
                    below?.kind == RowKind.App && below.app in s.active &&
                        rows.none { it.alert == Alert.Crash && it.app == below.app } &&
                        rng.next(1_000) < fallOdds ->
                        Row(nextId++, RowKind.App, below.app, count = rng.weighted(rules.countWeights) + 1, threshold = rules.threshold)
                    else -> spawnApp(s, rows, rng, nextId++)
                }
                rows.add(0, row)
                if (below != null && canMerge(s, row, below)) landings.addLast(row.id to below.id)
                beats += Beat.Spawn(row, 0, chain)
                spawnedSpam = spawnedSpam || row.kind == RowKind.Spam
                steps++
            }
            return spawnedSpam
        }
        fun matches(at: Int) = at in 1 until rows.size && canMerge(s, rows[at - 1], rows[at])
        fun remainingLanding(): Int? {
            while (landings.isNotEmpty()) {
                val (above, below) = landings.removeFirst()
                val at = rows.indexOfFirst { it.id == below }
                if (at > 0 && rows[at - 1].id == above && matches(at)) return at
            }
            return null
        }
        // When falling is disabled, keep the old collapse and wave ordering for backtests.
        val bursts = mutableListOf<Row>()
        val burstEffects = if (visuals) mutableListOf<MutableSet<String>>() else null
        var pendingChainEffect = false
        var rideBoost = state.rideBoost && !auto  // the boosted auto-clean uses it up
        var whyStacks = state.whyStacks
        val bestTier = bestTier(s)
        var pivot = index
        var chainBonus = 0  // 메롱: later bursts in this swipe count one more chain
        var waveFactor = rules.waveFactor
        while (true) {
            if (!matches(pivot)) {
                val blockedBySpam = rows.getOrNull(pivot - 1)?.kind == RowKind.Spam ||
                    rows.getOrNull(pivot)?.kind == RowKind.Spam
                val landing = if (falling) remainingLanding() else null
                if (landing == null) {
                    if (blockedBySpam) stop = ChainEndReason.Spam
                    break
                }
                pivot = landing
            }
            val a = rows[pivot - 1]
            val b = rows[pivot]
            val wild = a.kind == RowKind.Wild && b.kind == RowKind.Wild
            var merged = a.copy(
                kind = if (wild) RowKind.Wild else RowKind.App,
                app = a.app ?: b.app,
                count = a.count + b.count,
                threshold = rules.threshold,
            )
            rows.removeAt(pivot)
            rows[pivot - 1] = merged
            gauge += rules.gaugePerMerge
            beats += Beat.Merged(merged, b.id)
            // A taxi shares its explosion with one adjacent taxi, including that row's full count.
            var taxiPulled = false
            if (merged.app == "ctaxi" && on(s, "ctaxi")) {
                val j = listOf(pivot - 2, pivot).firstOrNull { i ->
                    rows.getOrNull(i)?.let { it.kind == RowKind.App && it.app == "ctaxi" } == true
                }
                if (j != null) {
                    val pulled = rows.removeAt(j)
                    taxiPulled = true
                    if (j < pivot - 1) pivot--
                    merged = merged.copy(count = merged.count + pulled.count)
                    rows[pivot - 1] = merged
                    gauge += rules.gaugePerMerge
                    beats += Beat.Merged(merged, pulled.id)
                }
            }
            if (merged.count < rules.threshold) break
            rows.removeAt(--pivot)
            val matched = a.app == "match" && b.app == "match" && on(s, "match")
            chain += if (matched) 3 else 1
            physicalBursts++
            steps++
            val burstApp = valueApp(s, merged)
            val tier = Catalog.app(burstApp).tier
            gauge += rules.gaugePerBurst + rules.igniterGaugePerTier * (bestTier - tier)
            val effects = if (visuals) linkedSetOf<String>() else null
            if (!preview) {
                val n = BigInteger.valueOf(merged.count.toLong())
                if (matched) effects?.add("match")
                if (taxiPulled) effects?.add("ctaxi")
                if (pendingChainEffect) effects?.add("music")
                val e = earn(s, burstApp, n, chain + chainBonus, ctx(app = burstApp, effects = effects))
                earned += e
                cleared += n
                track(merged, n)
                bursts += merged
                if (effects != null) burstEffects?.add(effects)
                val burstSynergy = if (visuals) {
                    val app = Catalog.app(burstApp)
                    val (region, category) = synergy(s, burstApp)
                    when {
                        region > 0 && region >= category -> BurstSynergy("region", app.region.id, region)
                        category > 0 -> BurstSynergy("cat", app.category.name, category)
                        else -> null
                    }
                } else null
                beats += Beat.Burst(merged, chain, e, burstApp, burstSynergy, effects ?: emptySet())
            }
            // Burst effects of the app that burst. Mark branch effects only when they change the outcome.
            fun fired() { effects?.add(burstApp) }
            when (burstApp.takeIf { on(s, it) }) {
                "green" -> { gauge += 10; fired() }
                "music" -> { chainBonus++; pendingChainEffect = true }
                "alarm" -> { pattern = pattern + "alarm"; fired() }
                "flash" -> rows.indexOfFirst { it.kind == RowKind.Spam }.takeIf { it >= 0 }
                    ?.let { rows.removeAt(it); if (it < pivot) pivot--; fired() }
                "sms" -> rows.getOrNull(pivot - 1)?.takeIf { it.kind == RowKind.App }
                    ?.let { rows[pivot - 1] = it.copy(count = it.count + 1); fired() }
                "chat" -> rows.getOrNull(pivot - 1)?.takeIf { it.app == "chat" }
                    ?.let { rows[pivot - 1] = it.copy(count = it.count + 1); fired() }
                "michat" -> rows.getOrNull(pivot)?.takeIf { it.kind == RowKind.App }
                    ?.let { rows[pivot] = it.copy(count = it.count + 1); fired() }
                "reddit" -> for (i in rows.indices) rows[i].app?.takeIf { it != "reddit" && Catalog.app(it).category == Category.Social }
                    ?.let { rows[i] = rows[i].copy(count = rows[i].count + 1); fired() }
                "pay" -> { energyGain++; if (state.energy - (if (usesBattery) 1 else 0) + energyGain <= energyCap(s)) fired() }
                "food" -> { if (freeSwipes < 3) fired(); freeSwipes = 3 }
                "zalo" -> { gauge += 2 * s.active.count { Catalog.app(it).region == Region.SoutheastAsia }; fired() }
                "line" -> { gauge += 2 * s.active.count { Catalog.app(it).region == Region.Asia }; fired() }
                "bigo" -> waveFactor = 4
                "superapp" -> if (!preview) {
                    Category.entries.forEach { c -> if (c != Catalog.app("superapp").category) categoryAdd.merge(c, BigInteger.valueOf(merged.count.toLong()), BigInteger::add) }
                    fired()
                }
                "mega" -> if (!preview) { earned += rules.appPrice(Catalog.app("mega")) * BigInteger.TWO / BigInteger.valueOf(1_000); fired() }
                // phase 2: 25% copies by hash roll, so the board's random sequence is untouched
                "lazada", "dance" -> if (Math.floorMod(mix(roll xor (chain * 7919L)), 4L) == 0L) { pattern = pattern + burstApp; fired() }
                "ride" -> if (merged.count >= 4 && !rideBoost) { rideBoost = true; fired() }
                "why" -> if (whyStacks < 15) { whyStacks++; fired() }
            }
            if (falling || (preview && rules.fallingChains)) {
                if (gauge >= 100 && now >= s.nextOverloadAt) {
                    overload = true
                    stop = ChainEndReason.Wave
                    break
                }
                if (falling) {
                    if (physicalBursts >= rules.fallBurstLimit || steps >= rules.fallWorkLimit) {
                        stop = ChainEndReason.Cap
                        break
                    }
                    val addedBefore = rows.size
                    val spawnedSpam = refillChain()
                    pivot += rows.size - addedBefore
                    if (spawnedSpam || steps >= rules.fallWorkLimit) {
                        stop = if (spawnedSpam) ChainEndReason.Spam else ChainEndReason.Cap
                        break
                    }
                }
            }
        }

        // Overload: a full gauge turns the bursts into waves through the hidden backlog.
        if (!rules.fallingChains) overload = chain > 0 && gauge >= 100 && now >= s.nextOverloadAt
        val depth = overloadDepth(s)
        // Target ranking needs the collapse outcome only. Waves add their depth to the winning chain.
        if (preview) return SwipeOutcome(state, beats, ZERO, chain + if (overload) depth else 0, ZERO, overload, ZERO)
        if (overload) {
            if (waveFactor != rules.waveFactor) bursts.forEachIndexed { i, row ->
                if (valueApp(s, row) == "bigo") burstEffects?.get(i)?.add("bigo")
            }
            val factor = BigInteger.valueOf(waveFactor.toLong())
            var backlog = bursts.map { BigInteger.valueOf(it.count.toLong()) }
            repeat(depth) { wave ->
                backlog = backlog.map { it * factor }
                chain++
                var waveEarned = ZERO
                var waveCleared = ZERO
                bursts.forEachIndexed { i, row ->
                    waveCleared += backlog[i]
                    waveEarned += earn(s, valueApp(s, row), backlog[i], chain + chainBonus, ctx(wave = true, app = valueApp(s, row), effects = burstEffects?.get(i)))
                    track(row, backlog[i])
                }
                if (rules.waveIncomePermille != 1_000) waveEarned = waveEarned * BigInteger.valueOf(rules.waveIncomePermille.toLong()) / BigInteger.valueOf(1_000)
                earned += waveEarned
                cleared += waveCleared
                beats += Beat.Wave(wave + 1, chain, waveCleared, waveEarned)
                if (on(s, "edit")) for (i in rows.indices) if (rows[i].app == "edit") {
                    rows[i] = rows[i].copy(count = rows[i].count + 1)
                    burstEffects?.firstOrNull()?.add("edit")
                }
            }
            gauge = 0
        }
        if (falling && physicalBursts > 0) beats += Beat.ChainEnd(stop, chain, physicalBursts, steps)
        gauge = gauge.coerceAtMost(100)
        if (chain >= 2 && on(s, "camera")) rows.filter { it.kind == RowKind.App }.takeIf { it.isNotEmpty() }
            ?.let { apps ->
                pattern = pattern + apps[Math.floorMod(mix(roll xor 0xCA3E), apps.size.toLong()).toInt()].app!!
                burstEffects?.lastOrNull()?.add("camera")
            }

        // Swipe-level effects: daily, return, after-purchase and binge bonuses.
        var dailyBonusDay = s.dailyBonusDay
        var boostNext = s.boostNext
        var returnBurst = s.returnBurst
        var returnSwipes = s.returnSwipes
        val today = now / 86_400_000L
        val firstEffects = burstEffects?.firstOrNull()
        if (bursts.isNotEmpty()) {
            for ((app, mult) in listOf("momo" to 3L, "airbnb" to 5L)) {
                if (on(s, app) && (dailyBonusDay[app] ?: -1L) < today) { earned *= BigInteger.valueOf(mult); dailyBonusDay = dailyBonusDay + (app to today); firstEffects?.add(app) }
            }
            if (boostNext && on(s, "travel")) { earned *= BigInteger.valueOf(3); firstEffects?.add("travel") }
            boostNext = false
            if (returnBurst && on(s, "agoda")) { earned *= BigInteger.valueOf(3); firstEffects?.add("agoda") }
            returnBurst = false
        }
        if (returnSwipes > 0) { if (on(s, "ott")) { earned *= BigInteger.TWO; firstEffects?.add("ott") }; returnSwipes-- }
        var nightSwipes = s.nightSwipes
        if (nightSwipes > 0) { if (on(s, "mall")) { earned *= BigInteger.TWO; firstEffects?.add("mall") }; nightSwipes-- }  // 쿠빵 새벽 배송
        // 날씨: the first swipe of the day adds one to its rows.
        if (on(s, "weather") && (dailyBonusDay["weather"] ?: -1L) < today) {
            for (i in rows.indices) if (rows[i].app == "weather") rows[i] = rows[i].copy(count = rows[i].count + 1)
            dailyBonusDay = dailyBonusDay + ("weather" to today)
        }
        // 음성 녹음 · 갤러리: echoes of the last swipe that burst.
        var echoSwipes = s.echoSwipes
        var lastBurstP = s.lastBurstP
        if (bursts.isEmpty() && echoSwipes > 0) { if (on(s, "recorder")) earned += lastBurstP / BigInteger.TEN; echoSwipes-- }
        if (on(s, "gallery") && s.swipes % 5 == 4) earned += lastBurstP / BigInteger.TEN
        if (bursts.isNotEmpty()) { lastBurstP = earned; echoSwipes = 3 }
        // 앱 충돌 restarted: that app's next burst ×2. 알림훈련: taps raise income while the drill runs.
        if (crashBoost != null && bursts.any { valueApp(s, it) == crashBoost }) { earned *= BigInteger.TWO; crashBoost = null }
        if (now < state.drillUntil) earned = earned * BigInteger.valueOf(100L + minOf(100, state.drillTaps)) / hundred
        // 메이버: a new trending category every 10 swipes.
        val trend = if (on(s, "portal") && s.swipes % 10 == 0) Category.entries[Math.floorMod(mix(roll), Category.entries.size.toLong()).toInt()] else s.trend

        if (auto && rules.autoSwipePermille != 1_000)
            earned = earned * BigInteger.valueOf(rules.autoSwipePermille.toLong()) / BigInteger.valueOf(1_000)
        val baselineIncome = earned
        if (!auto && s.level >= rules.lateDirectIncomeFrom)
            earned = earned * BigInteger.valueOf(rules.lateDirectIncomePermille.toLong()) / BigInteger.valueOf(1_000)
        val beforeEvent = earned
        val event = if (auto) null else tuning.eventAt(now)?.event
        if (event != null && event.qualifies(s.active, bursts)) {
            val bp = tuning.bonusBasisPoints(event)
            earned += earned * BigInteger.valueOf(bp.toLong()) / BigInteger.valueOf(10_000)
        }
        // Jackpot cap: luck may jump a few levels, never the whole run.
        val capSteps = rules.swipeLevelCap + if (s.level >= rules.lateGrowthFrom) rules.lateSwipeCapSteps else 0
        val tailHeadroom = if (overload && maxOf(s.level, s.backup.bestLevel) >= 124) rules.post124WaveCapSteps else 0
        val room = jackpotRoom(s, capSteps + tailHeadroom)
        val capped = (earned - room).max(ZERO)
        if (capped.signum() > 0) { earned = room; beats += Beat.Capped(capped) }

        // Progress: level follows this run's cumulative P.
        val runEarned = s.runEarned + earned
        val level = maxOf(s.level, rules.levelFor(runEarned, s.backup.lv(BackupItem.FastBoot)))
        for (l in s.level + 1..level) beats += Beat.LevelUp(l)

        val cap = energyCap(s)
        val dt = (if (s.lastSwipeAt == 0L) chargeMs(s) else now - s.lastSwipeAt).coerceIn(chargeMs(s, now), 60_000L)
        val rateWindow = rules.recentIncomeWindow.toLong()
        val rateIncome = baselineIncome.min(room)
        var next = s.copy(
            rows = rows,
            gauge = gauge,
            nextOverloadAt = if (overload) now + rules.overloadMinIntervalMs else s.nextOverloadAt,
            energy = minOf(energyCap(s), state.energy - (if (usesBattery) 1 else 0) + energyGain),
            // Any swipe below a full battery keeps a charge scheduled, even if the cap grew meanwhile.
            nextChargeAt = if (state.nextChargeAt == 0L) now + chargeMs(s, now) else state.nextChargeAt,
            wallet = s.wallet + earned,
            runEarned = runEarned,
            lastEarned = earned,
            bestSwipe = s.bestSwipe.max(earned),
            bestChain = maxOf(s.bestChain, chain),
            level = level,
            swipes = s.swipes + 1,
            autoPausedUntil = pausedUntil,
            pattern = pattern,
            nextId = nextId,
            regionCleared = s.regionCleared.toMutableMap().also { m -> regionAdd.forEach { (k, v) -> m.merge(k, v, BigInteger::add) } },
            categoryCleared = s.categoryCleared.toMutableMap().also { m -> categoryAdd.forEach { (k, v) -> m.merge(k, v, BigInteger::add) } },
            rng = rng.seed,
            // 수입 부스트 is left out of the rate, so offline settlement and 시간 도약 don't inherit it.
            earnRate = (s.earnRate * BigInteger.valueOf(rateWindow - 1) + (if (now < s.backup.cloud.account.boostUntil) rateIncome * BigInteger.valueOf(1_000) / BigInteger.valueOf(rules.boostPermille.toLong()) else rateIncome) * BigInteger.valueOf(60_000 / dt)) / BigInteger.valueOf(rateWindow),
            lastSwipeAt = now,
            lastBurstP = lastBurstP,
            echoSwipes = echoSwipes,
            nightSwipes = nightSwipes,
            rideBoost = rideBoost,
            whyStacks = whyStacks,
            freeSwipes = freeSwipes,
            boostNext = boostNext,
            returnBurst = returnBurst,
            returnSwipes = returnSwipes,
            dailyBonusDay = dailyBonusDay,
            trend = trend,
            crashBoost = crashBoost,
            drillUntil = drillUntil,
            drillTaps = drillTaps,
        )
        next = fillDeck(next)
        next = refill(next, now, beats)
        return SwipeOutcome(next, beats, earned, chain, cleared, overload, capped, if (event != null) earned - beforeEvent.min(room) else ZERO)
    }

    /** A rising minimum slot count pulls the best owned apps into the deck. */
    private fun fillDeck(s: GameState): GameState {
        val need = minSlots(s) - s.active.size
        if (need <= 0) return s
        val extra = s.appLevels.keys.filter { it !in s.active }.sortedByDescending { Catalog.app(it).tier * 10 + Catalog.app(it).order }.take(need)
        return stabilizeBoard(s.copy(active = s.active + extra))
    }

    /** New rows arrive at the top until the board is full again. */
    private fun refill(state: GameState, now: Long, beats: MutableList<Beat>): GameState {
        val rng = Rng(state.rng)
        val n = state.swipes
        val surviving = state.rows.filterNot { it.kind == RowKind.Event && it.expireAt in 1..n }
        val rows = quietRows(state, surviving).toMutableList()
        var id = state.nextId
        var pausedUntil = state.autoPausedUntil
        val pattern = state.pattern.toMutableList()

        // Spam copies itself only while the board holds fewer than spamMax: otherwise young copies keep every refill spam
        // and the board locks into spam forever (found by the BM simulator, 11-23).
        val spamRows = rows.count { it.kind == RowKind.Spam }
        val overdue = if (on(state, "secret") || spamRows >= rules.spamMax) -1 else rows.indexOfFirst { it.kind == RowKind.Spam && it.copyAt in 1..n }  // 텔레그림
        if (overdue >= 0) rows[overdue] = rows[overdue].copy(copyAt = 0)
        val quiet = now < state.dndUntil  // 방해 금지: no interruptions
        var spam = !quiet && spamRows < rules.spamMax && (overdue >= 0 || n % rules.spamEverySwipes == 0)
        var call = !quiet && state.level >= rules.callUnlockLevel && n % rules.callEverySwipes == 0 && rows.none { it.kind == RowKind.Call }
        val cards = state.equipped.filter { c -> c.everySwipes > 0 && n % c.everySwipes == 0 && rows.none { it.event == c } }.toMutableList()
        val today = now / 86_400_000L
        var birthdayDay = state.birthdayDay
        if (EventCard.Birthday in state.equipped && today > birthdayDay && rows.none { it.event == EventCard.Birthday }) { cards += EventCard.Birthday; birthdayDay = today }
        fun noAlert(a: Alert) = rows.none { it.alert == a }
        val alerts = mutableListOf<Row>()
        if (!quiet) {
            if (state.level >= rules.lowBatteryLevel && state.energy <= rules.lowBatteryAt && noAlert(Alert.LowBattery)) alerts += Row(0, RowKind.Alert, alert = Alert.LowBattery)
            if (state.level >= rules.storageLevel && n % rules.storageEverySwipes == rules.storageEverySwipes / 2 && noAlert(Alert.Storage)) alerts += Row(0, RowKind.Alert, alert = Alert.Storage)
            if (state.level >= rules.crashLevel && n % rules.crashEverySwipes == rules.crashEverySwipes / 2 && noAlert(Alert.Crash) && state.active.size >= 2)
                alerts += Row(0, RowKind.Alert, app = state.active[rng.next(state.active.size)], alert = Alert.Crash)
        }

        var nextMajor = state.nextMajorEventAt
        var previousMajor = state.previousMajorEvent
        fun majorAllowed(tag: String) = rules.majorEventCooldownMs == 0L || (now >= nextMajor && tag != previousMajor)
        fun majorSpawned(tag: String) {
            if (rules.majorEventCooldownMs == 0L) return
            nextMajor = now + rules.majorEventCooldownMs
            previousMajor = tag
        }
        while (rows.size < rowCount(state)) {
            val alert = alerts.firstOrNull { it.alert == Alert.Storage || majorAllowed(it.alert!!.name) }
            val card = cards.firstOrNull { (it != EventCard.Drill && it != EventCard.Lotto) || majorAllowed(it.name) }
            val row = when {
                call && majorAllowed("call") -> {
                    call = false
                    majorSpawned("call")
                    pausedUntil = now + rules.callPauseMs / (if (on(state, "secret")) 2 else 1)
                    Row(id++, RowKind.Call)
                }
                spam -> { spam = false; Row(id++, RowKind.Spam, copyAt = n + rules.spamCopyAfterSwipes) }
                alert != null -> {
                    alerts.remove(alert)
                    if (alert.alert != Alert.Storage) majorSpawned(alert.alert!!.name)
                    alert.copy(id = id++)
                }
                card != null -> {
                    cards.remove(card)
                    if (card == EventCard.Drill || card == EventCard.Lotto) majorSpawned(card.name)
                    Row(id++, RowKind.Event, event = card, expireAt = if (card == EventCard.Otp) n + rules.otpLifeSwipes else 0)
                }
                pattern.isNotEmpty() -> pattern.removeAt(0).let { a -> Row(id++, RowKind.App, a, count = rng.weighted(rules.countWeights) + 1, threshold = rules.threshold) }
                else -> spawnApp(state, rows, rng, id++)
            }
            val replacement = if (rules.fallingChains && state.level >= rules.stableSandwichFrom && row.merges && rows.size >= 2 && canMerge(state, row, rows[0])) {
                val second = rows[1]
                if (second.kind == RowKind.App && second.app in state.active &&
                    rows.none { it.alert == Alert.Crash && it.app == second.app } && !canMerge(state, rows[0], second))
                    row.copy(kind = RowKind.App, app = second.app)
                else row
            } else row
            val arrival = if (rules.fallingChains) quietRow(state, replacement, rows, null, rows.firstOrNull()) ?: break else replacement
            rows.add(0, arrival)
            beats += Beat.Spawned(arrival)
        }
        return state.copy(rows = rows, rng = rng.seed, nextId = id, autoPausedUntil = pausedUntil, pattern = pattern, birthdayDay = birthdayDay,
            nextMajorEventAt = nextMajor, previousMajorEvent = previousMajor)
    }

    private fun spawnApp(s: GameState, rows: List<Row>, rng: Rng, id: Long): Row {
        val count = { rng.weighted(rules.countWeights) + 1 }
        if (rng.next(100) < wildPercent(s)) return Row(id, RowKind.Wild, count = count(), threshold = rules.threshold)
        val top = rows.firstOrNull()
        val second = rows.getOrNull(1)
        val crashed = rows.filter { it.alert == Alert.Crash }.mapNotNull { it.app }  // 앱 충돌: that app posts nothing until restarted
        val options = s.active.filter { (top?.kind != RowKind.App || it != top.app) && it !in crashed }.ifEmpty { s.active }
        val sandwich = second?.kind == RowKind.App && second.app != top?.app && second.app in options
        val app = if (sandwich && rng.next(100) < sandwichPercent(s)) second!!.app!! else options[rng.next(options.size)]
        return Row(id, RowKind.App, app, count = count(), threshold = rules.threshold)
    }

    // Offline --------------------------------------------------------------------------------

    fun offlineCapMs(s: GameState): Long =
        (rules.step(rules.offlineCapSteps, s.level) + s.backup.lv(BackupItem.Offline) + s.backup.cloud.lv(CloudItem.CloudOffline)) * 3_600_000L
    fun offlinePercent(s: GameState): Int {
        val veteranDelta = if (maxOf(s.level, s.backup.bestLevel) >= 124) tuning.veteranOfflinePercentDelta else 0
        return (rules.offlineEfficiencyPercent + veteranDelta).coerceAtLeast(0) + 2 * s.backup.lv(BackupItem.Offline)
    }

    /** Eligibility at return time. A watched ad and a pass share one reward; Ultra multiplies that payout. */
    fun offlineBonus(s: GameState, now: Long): OfflineBonusStatus {
        val acc = s.account
        val today = dayOf(now)
        val adLeftMs = if (acc.lastAdWatchDay >= 0 && acc.lastAdWatchDay in (today - 1)..today)
            (acc.lastAdWatchDay + 2) * DAY_MS - now else 0
        val speedLeftMs = if (acc.speedPass != null) (acc.speedPassUntil - now).coerceAtLeast(0) else 0
        return OfflineBonusStatus(
            rewardBonusPermille = rules.adWatchOfflinePermille - 1_000,
            ultraBonusPermille = rules.ultraOfflinePermille - 1_000,
            adLeftMs = adLeftMs,
            passLeftMs = maxOf(0, acc.passUntil - now, speedLeftMs),
            ultraLeftMs = if (acc.speedPass == SpeedPass.Ultra) speedLeftMs else 0,
        )
    }

    fun settleOffline(s: GameState, awayMs: Long, now: Long): Pair<GameState, BigInteger> {
        val result = settleOfflineWithBonus(s, awayMs, now)
        return result.state to result.paid
    }

    /**
     * Returning after [awayMs]: pays the recent income rate for the capped time at the offline efficiency,
     * in P; the level follows. The battery refills through [tick] as usual.
     */
    fun settleOfflineWithBonus(s: GameState, awayMs: Long, now: Long): OfflineSettlement {
        // 오버클럭 time inside the away window counts that many times over.
        val from = now - awayMs
        val acc = s.backup.cloud.account
        val sub = if (acc.speedPass != null && now < acc.speedPassUntil) acc.speedPass.speedPermille.toLong() else 1_000L
        val until = acc.overclockUntil
        val boosted = if (until <= 0L || sub >= rules.overclockPermille) 0L else maxOf(0L, minOf(now, until) - from)
        val effective = minOf(awayMs, offlineCapMs(s)) * sub / 1_000 + boosted * (rules.overclockPermille - 1_000) / 1_000
        val minutes = BigInteger.valueOf(effective / 60_000)
        val pct = BigInteger.valueOf(offlinePercent(s).toLong())
        val income = s.earnRate * minutes * pct * BigInteger.valueOf(rules.offlineAutoPermille.toLong())
        val bonus = offlineBonus(s, now)
        val cap = jackpotRoom(s, (rules.offlineLevelCap * sub / 1_000).toInt())
        val basePaid = (income / BigInteger.valueOf(100_000)).min(cap)
        val multiplier = bonus.multiplierPermille
        val p = if (multiplier == 1_000) basePaid else
            (income * BigInteger.valueOf(multiplier.toLong()) / BigInteger.valueOf(100_000_000)).min(cap)
        val runEarned = s.runEarned + p
        val level = maxOf(s.level, rules.levelFor(runEarned, s.backup.lv(BackupItem.FastBoot)))
        val next = fillDeck(tick(s, now).copy(wallet = s.wallet + p, runEarned = runEarned, level = level, lastSwipeAt = 0,
            returnBurst = true, returnSwipes = 3, nightSwipes = if (Math.floorMod(now / 3_600_000L + 9, 24L) !in 7..21) 3 else 0))  // KST 22~7시
        return OfflineSettlement(next, p, basePaid, bonus)
    }

    // Targeting ------------------------------------------------------------------------------

    /** Rows whose removal makes two mergeable rows touch. */
    fun mergeTargets(s: GameState): List<Row> = s.rows.indices.filter { i ->
        val a = s.rows.getOrNull(i - 1)
        val b = s.rows.getOrNull(i + 1)
        a != null && b != null && canMerge(s, a, b)
    }.map { s.rows[it] }

    /**
     * Auto-clean is deliberately simple: the lowest row that makes a merge, else an event card, else spam,
     * else the bottom row. From AutoClean backup level 5 it looks one move ahead like [bestTarget].
     */
    fun autoTarget(s: GameState, now: Long): Long? {
        if (on(s, "ai")) return bestTwoStepTarget(s, now)
        if (s.backup.lv(BackupItem.AutoClean) >= rules.smartAutoLevel || on(s, "search")) return bestTarget(s, now)
        return mergeTargets(s).lastOrNull()?.id
            ?: s.rows.lastOrNull { it.kind == RowKind.Event }?.id
            ?: s.rows.lastOrNull { it.kind == RowKind.Spam }?.id
            ?: s.rows.lastOrNull()?.id
    }

    private fun bestTwoStepTarget(s: GameState, now: Long): Long? {
        val candidates = mergeTargets(s)
        if (candidates.size < 2) return bestTarget(s, now)
        // Only forecast genuine merge targets. Each simulated move works on an independent copy of the RNG and board.
        return candidates.mapNotNull { row ->
            val first = swipe(s, row.id, now, Actor.Auto) ?: return@mapNotNull null
            val nextAt = now + autoIntervalMs(first.state, now)
            val next = bestTarget(first.state, nextAt)?.let { swipe(first.state, it, nextAt, Actor.Auto, preview = true) }
            row.id to (first.chain + (next?.chain ?: 0))
        }.maxByOrNull { it.second }?.first ?: bestTarget(s, now)
    }

    /**
     * Reference "good player" for simulators: tries every row and keeps the best immediate result
     * (chain, overload, merges, then event cards and spam). Ties go to the lower row.
     */
    fun bestTarget(s: GameState, now: Long): Long? {
        val state = tick(s, now)
        if (state.energy <= 0) return null
        var best: Long? = null
        var bestKey = -1L
        val rows = state.rows
        for ((i, row) in rows.withIndex()) {
            val before = rows.getOrNull(i - 1)
            val after = rows.getOrNull(i + 1)
            val o = if (before != null && after != null && canMerge(state, before, after))
                swipe(state, row.id, now, Actor.Player, preview = true) else null
            val chain = o?.chain ?: 0
            val overload = if (o?.overload == true) 1 else 0
            val merges = o?.beats?.count { it is Beat.Merged } ?: 0
            val kind = when (row.kind) { RowKind.Event -> 2; RowKind.Spam -> 1; else -> 0 }
            val key = ((chain * 2L + overload) * (rows.size + 1) + merges) * 3 + kind
            if (key >= bestKey) { best = row.id; bestKey = key }
        }
        return best
    }

    // Shop and deck --------------------------------------------------------------------------

    /** App price now: half after 앱스토어 세일. */
    fun appPrice(s: GameState, appId: String): BigInteger {
        val app = Catalog.app(appId)
        var p = rules.appPrice(app)
        if (app.tier == 2 && firstRun(s) && rules.firstRunTier2PricePermille != 1_000)
            p = (p * BigInteger.valueOf(rules.firstRunTier2PricePermille.toLong()) / BigInteger.valueOf(1_000)).max(BigInteger.ONE)
        if (on(s, "shopee") && p < rules.appPrice(Catalog.app("shopee"))) p = p * BigInteger.valueOf(9) / BigInteger.TEN  // 쇼피피
        return if (s.saleNext) p / BigInteger.TWO else p
    }

    private fun firstRun(s: GameState) = s.backup.bestLevel < rules.resetLevel && s.backup.resets == 0 && s.backup.cloud.syncs == 0

    /** Upgrade price after 글린츠츠 (업무 −10%) and 페이팔팔 (금융 −10%). */
    fun upgradePrice(s: GameState, appId: String): BigInteger {
        val app = Catalog.app(appId)
        val rank = s.appLevels[appId] ?: 1
        var p = rules.upgradePrice(app, rank)
        if (rules.firstRunRankPricePermille != 1_000 && firstRun(s) && s.level >= 8 && app.tier <= 2 && rank < 25)
            p = (p * BigInteger.valueOf(rules.firstRunRankPricePermille.toLong()) / BigInteger.valueOf(1_000)).max(BigInteger.ONE)
        if ((on(s, "glints") && app.category == Category.Work) || (on(s, "paypal") && app.category == Category.Money)) p = p * BigInteger.valueOf(9) / BigInteger.TEN
        return p
    }

    private fun legacyAppUnlockLevel(s: GameState): Int =
        if (s.rulesVersion < APP_UNLOCK_RULES_VERSION) maxOf(s.backup.legacyAppUnlockLevel, s.level, s.backup.bestLevel)
        else s.backup.legacyAppUnlockLevel

    /** Unlocks are permanent: the deepest level any run reached (and the reset count) opens apps for good. */
    fun unlocked(s: GameState, appId: String): Boolean {
        val app = Catalog.app(appId)
        if (s.backup.resets < app.minResets) return false
        if (maxOf(s.level, s.backup.bestLevel) >= app.unlockLevel) return true
        val previousGate = Catalog.legacyAppUnlockLevels[appId] ?: return false
        return legacyAppUnlockLevel(s) >= previousGate
    }

    fun canBuy(s: GameState, appId: String): Boolean =
        appId !in s.appLevels && unlocked(s, appId) && s.wallet >= appPrice(s, appId)

    /** A bought app joins the deck if a slot is free; otherwise the player swaps it in. */
    fun buy(s: GameState, appId: String): GameState {
        if (!canBuy(s, appId)) return s
        val active = if (s.active.size < maxSlots(s)) s.active + appId else s.active
        val start = if (on(s, "store")) 2 else 1  // 큐글플레이: new apps arrive upgraded once
        return stabilizeBoard(s.copy(wallet = s.wallet - appPrice(s, appId), appLevels = s.appLevels + (appId to start),
            active = active, saleNext = false, boostNext = on(s, "travel") || s.boostNext).rememberCollection())
    }

    private val earlyProLevels = mapOf("alarm" to 10, "weather" to 11, "memo" to 12, "flash" to 13, "calc" to 14, "recorder" to 15)

    fun proLevel(s: GameState, appId: String): Int =
        (if (rules.firstRunEarlyPro && firstRun(s)) earlyProLevels[appId] else null) ?: Catalog.proLevel.getValue(appId)

    fun proPrice(s: GameState, appId: String): BigInteger {
        val level = proLevel(s, appId)
        return if (level == Catalog.proLevel.getValue(appId)) rules.proPrice(appId)
        else (rules.levelP(level + 1) * BigInteger.valueOf(3) / BigInteger.TEN).max(BigInteger.ONE)
    }

    /** Pro version: ×3 value for an owned app, from its pro level; priced from the level curve. */
    fun buyPro(s: GameState, appId: String): GameState {
        if (appId !in s.appLevels || appId in s.pro || s.level < proLevel(s, appId)) return s
        val price = proPrice(s, appId)
        if (s.wallet < price) return s
        return s.copy(wallet = s.wallet - price, pro = s.pro + appId)
    }

    fun upgrade(s: GameState, appId: String): GameState {
        val level = s.appLevels[appId] ?: return s
        val price = upgradePrice(s, appId)
        if (s.wallet < price) return s
        return s.copy(wallet = s.wallet - price, appLevels = s.appLevels + (appId to level + 1))
    }

    /** Bulk upgrade: up to [times] levels (x10 button), stopping when the wallet runs out. Returns the state and levels bought. */
    fun upgradeMany(s: GameState, appId: String, times: Int): Pair<GameState, Int> {
        var cur = s
        var n = 0
        while (n < times) {
            val next = upgrade(cur, appId)
            if (next === cur || next == cur) break
            cur = next; n++
        }
        return cur to n
    }

    /** Max button: as many levels of one app as the wallet allows. */
    fun upgradeMax(s: GameState, appId: String): Pair<GameState, Int> = upgradeMany(s, appId, Int.MAX_VALUE)

    /** Total price of the next [times] levels of an app (what the x10 button shows). */
    fun upgradeCost(s: GameState, appId: String, times: Int): BigInteger {
        var total = ZERO
        var cur = s
        repeat(times) {
            val p = upgradePrice(cur, appId)
            total += p
            cur = cur.copy(appLevels = cur.appLevels + (appId to (cur.appLevels[appId] ?: 1) + 1))
        }
        return total
    }

    /**
     * 모두 강화: spends the wallet on the deck, always buying the cheapest next level among deck apps, so low
     * tiers catch up first. Returns the state and how many levels were bought.
     */
    fun upgradeAll(s: GameState): Pair<GameState, Int> {
        var cur = s
        var n = 0
        while (true) {
            val id = cur.active.minByOrNull { upgradePrice(cur, it) } ?: break
            val next = upgrade(cur, id)
            if (next == cur) break
            cur = next; n++
        }
        return cur to n
    }

    /** 알림훈련: one tap on the drill panel while it runs (+1% income, up to +100%). */
    fun tapDrill(s: GameState, now: Long): GameState =
        if (now < s.drillUntil && s.drillTaps < 100) s.copy(drillTaps = s.drillTaps + 1) else s

    /** 방해 금지: switch on if equipped and used fewer than [RuleSet.dndPerDay] times today. */
    fun activateDnd(s: GameState, now: Long): GameState {
        val today = now / 86_400_000L
        val uses = if (s.dndDay == today) s.dndUses else 0
        if (EventCard.Dnd !in s.equipped || uses >= rules.dndPerDay || now < s.dndUntil) return s
        return s.copy(dndUntil = now + rules.dndMs, dndDay = today, dndUses = uses + 1)
    }

    fun buyEvent(s: GameState, card: EventCard): GameState {
        val price = rules.cardPrice(card)
        if (card in s.events || s.level < card.unlockLevel || s.wallet < price) return s
        val equipped = if (s.equipped.size < eventSlots(s)) s.equipped + card else s.equipped
        return s.copy(wallet = s.wallet - price, events = s.events + card, equipped = equipped)
    }

    fun buyAssist(s: GameState, a: Assist): GameState {
        val owned = s.assist(a)
        val price = rules.assistPrice(a, owned)
        if (s.level < a.unlockLevel || owned >= a.max(s.level) || s.wallet < price) return s
        return s.copy(wallet = s.wallet - price, assists = s.assists + (a to owned + 1))
    }

    /** Set both decks. Apps: owned, distinct, between the level's minimum and maximum. Takes effect on new rows. */
    fun setDeck(s: GameState, apps: List<String>, cards: List<EventCard> = s.equipped): GameState? {
        val ok = apps.distinct().size == apps.size && apps.size >= maxOf(2, minSlots(s)) && apps.size <= maxSlots(s) &&
            s.appLevels.keys.containsAll(apps) && cards.distinct().size == cards.size && cards.size <= eventSlots(s) &&
            s.events.containsAll(cards)
        return if (ok) stabilizeBoard(s.copy(active = apps, equipped = cards)) else null
    }
}
