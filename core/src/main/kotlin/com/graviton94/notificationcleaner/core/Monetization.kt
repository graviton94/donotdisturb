package com.graviton94.notificationcleaner.core

import kotlinx.serialization.Serializable

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Base64

/**
 * Monetization layer (docs/bible/17-monetization.md). Four currencies:
 * P (run gold) · 데이터 (paid, permanent; `credits` in code) · 백업파일/GB (prestige) · 배터리 (energy).
 * Everything here is permanent and lives in [Account], which survives resets and syncs.
 */

/** Rewarded ad placements and their daily caps. A month pass gives the same rewards without the ad. */
enum class AdSlot(val label: String, val daily: Int) {
    BestDouble("최고 기록 ×2", 2),
    OfflineDouble("오프라인 ×2", 1),
    Boost("수입 부스트 ×2", 2),
    Warp("시간 도약", 1),
    Overclock("오버클럭 1시간", 2),
    Battery("보조배터리", 2),
    CreditBox("데이터 상자", 1),
    FreeUpgrade("무료 강화", 1),
}

/** What 데이터 (credits) buys besides premium stars. */
enum class CreditItem(val label: String, val price: Long, val daily: Int) {
    Boost("수입 부스트 ×2", 40, 6),
    Warp("시간 도약", 80, 12),
    Battery("보조배터리", 10, 20),
    BackupBoost("백업 부스트 (다음 초기화 파일 ×2)", 150, 1),
    Overclock("오버클럭 1시간", 40, 24),
}

/** Speed subscriptions, each including the month pass. */
enum class SpeedPass(val label: String, val won: Int, val speedPermille: Int, val dailyData: Long, val prestigePermille: Int, val productId: String) {
    Turbo("터보 패스", 33_000, 3_500, 0, 1_750, "pass_turbo"),
    Ultra("울트라 패스", 99_000, 8_000, 200, 4_000, "pass_ultra"),
}

/** One day of the 30-day attendance board: data, free uses of credit items, premium star vouchers, a title. */
data class AttendReward(val data: Long, val tickets: Map<CreditItem, Int> = emptyMap(), val starVouchers: Int = 0, val title: String? = null)

data class DataPack(val id: String, val data: Long, val won: Int)

const val PASS_DAYS = 30

data class PassValue(
    val wonPrice: Int,
    val dataNow: Long,
    val dataDaily: Long,
    val overclockMinutesDaily: Int,
    val speedPermille: Int,
    val prestigePermille: Int,
) {
    val dataTotal get() = dataNow + dataDaily * PASS_DAYS
    val dataEquivalent get() = dataTotal + (overclockMinutesDaily.toLong() * PASS_DAYS * CreditItem.Overclock.price / 60)
}

data class CommerceOffers(
    val packs: List<DataPack> = listOf(
        DataPack("data_100", 100, 2_200), DataPack("data_540", 540, 5_900), DataPack("data_1200", 1_200, 12_000),
        DataPack("data_3700", 3_700, 33_000), DataPack("data_8600", 8_600, 69_000), DataPack("data_16800", 16_800, 119_000),
    ),
    val firstBonusCap: Long = 1_200,
    val starterWon: Int = 2_500,
    val starterData: Long = 500,
    val starterMs: Long = 48 * 3_600_000L,
    val starterTickets: Map<CreditItem, Int> = mapOf(CreditItem.Boost to 3, CreditItem.Overclock to 3),
    val rookieWon: Int = 3_300,
    val rookieNow: Long = 300,
    val rookieDaily: Long = 50,
    val rookieDays: Int = 7,
    val passWon: Int = 5_500,
    val starterProductId: String = "starter_pack",
    val rookieProductId: String = "rookie_pass",
    val monthlyProductId: String = "pass_monthly",
) {
    val baseWonPerData: Double get() = packs.first().won.toDouble() / packs.first().data

    /** One decimal on the same list-price basis used for first-purchase offers. */
    fun valueX(dataEquivalent: Long, won: Int): Double {
        val reference = packs.first()
        return BigDecimal.valueOf(dataEquivalent).multiply(BigDecimal.valueOf(reference.won.toLong()))
            .divide(BigDecimal.valueOf(won.toLong()).multiply(BigDecimal.valueOf(reference.data)), 1, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun passValue(rules: RuleSet, speed: SpeedPass? = null) = PassValue(
        wonPrice = speed?.won ?: passWon,
        dataNow = rules.passInstantCredits,
        dataDaily = rules.passDailyCredits + (speed?.dailyData ?: 0),
        overclockMinutesDaily = rules.passOverclockMinutes,
        speedPermille = speed?.speedPermille ?: 0,
        prestigePermille = speed?.prestigePermille ?: 0,
    )

    fun dataReceived(pack: DataPack, first: Boolean): Long =
        pack.data + if (first) minOf(pack.data, firstBonusCap) else 0

    fun voucherValue(rules: RuleSet): Long = Catalog.apps.first { it.tier == 5 }.let {
        rules.premiumCost(it, 1) + rules.premiumCost(it, 2)
    }

    fun starterValue(rules: RuleSet): Long =
        starterData + voucherValue(rules) + starterTickets.entries.sumOf { (item, quantity) -> item.price * quantity }

    fun rookieValue(rules: RuleSet): Long = rookieNow + rookieDaily * rookieDays + voucherValue(rules)
}

@Serializable
data class Account(
    val credits: Long = 0,
    /** App id → premium stars (0..5). */
    val premium: Map<String, Int> = emptyMap(),
    val attendDay: Long = -1,
    /** Day of the 30-day board reached by the current unbroken streak (0 = not started). A missed day restarts at 1. */
    val streak: Int = 0,
    /** Free uses of credit items (from attendance), spent like a purchase without data. */
    val tickets: Map<CreditItem, Int> = emptyMap(),
    /** 프리미엄 ★ 선택권: +1 star on any unlocked app, up to [RuleSet.voucherMaxStars]. */
    val starVouchers: Int = 0,
    val titles: Set<String> = emptySet(),
    /** Lifetime counters for achievements (Meta.kt), keyed by [Stat] name. */
    val stats: Map<String, Long> = emptyMap(),
    /** Today's mission progress (Meta.kt), reset when [missionProgressDay] changes. */
    val missionProgress: Map<String, Long> = emptyMap(),
    val missionProgressDay: Long = -1,
    /** 꾸미기 조각 from achievements and the daily challenge: 10 = one free cosmetic. */
    val pieces: Int = 0,
    val collectedApps: Set<String> = emptySet(),
    val claimedBook: Set<Int> = emptySet(),
    /** 일일 도전: day of the last key use, keys used that day, personal best score. */
    val challengeDay: Long = -1,
    val challengeKeys: Int = 0,
    val challengeBest: String = "0",
    /** Granted purchase ids. Older saves also stored debug ad ids here. */
    val processedClaims: List<String> = emptyList(),
    /** UTC claim day → compact rewarded ad ids, kept for seven calendar days. */
    val processedAdClaims: Map<Long, List<String>> = emptyMap(),
    val missionDay: Long = -1,
    val adDay: Long = -1,
    val lastAdWatchDay: Long = -1,
    val adUses: Map<AdSlot, Int> = emptyMap(),
    val shopDay: Long = -1,
    val shopUses: Map<CreditItem, Int> = emptyMap(),
    /** 수입 부스트: all income ×2 until this time. */
    val boostUntil: Long = 0,
    /** 오버클럭: the game runs faster until this time. */
    val overclockUntil: Long = 0,
    /** 속도 구독 (터보 ×3 / 울트라 ×6): permanent game speed until [speedPassUntil]. */
    val speedPass: SpeedPass? = null,
    val speedPassUntil: Long = 0,
    /** 월간 패스: ad-free rewards and daily credits until this time. */
    val passUntil: Long = 0,
    val passDay: Long = -1,
    /** 백업 부스트 bought for the next factory reset. */
    val backupBoost: Boolean = false,
    val creditsFree: Long = 0,
    val creditsBought: Long = 0,
    val creditsSpent: Long = 0,
    /** First day the game ran: the 초심자 패스 is sold for 7 days from here. */
    val installDay: Long = -1,
    /** Install time in ms: the starter pack's 48-hour countdown runs from here. */
    val installAt: Long = -1,
    /** 초심자 패스: purchase day (-1 = not bought) and the last day its daily data was taken. */
    val rookieDay: Long = -1,
    val rookieClaimDay: Long = -1,
    /** Store products already bought once (first purchase pays double). */
    val firstBuys: Set<String> = emptySet(),
) {
    fun stars(appId: String) = premium[appId] ?: 0
}

val GameState.account: Account get() = backup.cloud.account
fun GameState.withAccount(f: (Account) -> Account): GameState = copy(backup = backup.copy(cloud = backup.cloud.copy(account = f(backup.cloud.account))))

internal const val DAY_MS = 86_400_000L
private const val AD_CLAIM_DAYS = 7
private val AD_CLAIM_LIMIT = AD_CLAIM_DAYS * AdSlot.entries.sumOf { it.daily }
private val rewardIdEncoding = Base64.getUrlEncoder().withoutPadding()

internal fun rewardKey(id: String): String = rewardIdEncoding.encodeToString(
    MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8)).copyOf(16)
)

private fun Map<Long, List<String>>.retainRecentAds(today: Long): Map<Long, List<String>> {
    var remaining = AD_CLAIM_LIMIT
    return entries.sortedByDescending { it.key }.filter { it.key > today - AD_CLAIM_DAYS }.mapNotNull { (day, ids) ->
        if (remaining == 0) return@mapNotNull null
        val recent = ids.takeLast(remaining)
        remaining -= recent.size
        day to recent
    }.toMap()
}

/** Migrate timestamp-less debug ad ids once. Preserve all ambiguous ids as purchase receipts. */
internal fun GameState.migrateRewardClaims(now: Long): GameState {
    val (legacyAds, purchases) = account.processedClaims.partition { it.startsWith("fakead-") }
    if (legacyAds.isEmpty()) return this
    val day = dayOf(now)
    val carried = legacyAds.takeLast(AD_CLAIM_LIMIT).map(::rewardKey)
    return withAccount { a ->
        val ads = a.processedAdClaims + (day to (carried + a.processedAdClaims[day].orEmpty()).distinct())
        a.copy(processedClaims = purchases, processedAdClaims = ads.retainRecentAds(day))
    }
}

internal fun GameState.expireRewardClaims(now: Long): GameState {
    val day = dayOf(now)
    val claims = account.processedAdClaims
    if (claims.keys.none { it <= day - AD_CLAIM_DAYS }) return this
    return withAccount { it.copy(processedAdClaims = claims.retainRecentAds(day)) }
}
fun dayOf(now: Long) = Math.floorDiv(now, DAY_MS)

/** Premium star value multiplier in ‰. */
fun RuleSet.premiumPermille(stars: Int): Long { var m = 1_000L; repeat(stars) { m = m * premiumStepPermille / 1_000 }; return m }

/**
 * Credits for one premium star. A full ★0→★5 costs [RuleSet.premiumBudget] for a top-tier app, half for a
 * mid-tier one and a quarter for tier 1: what 12 weeks of steady free play (attendance + missions) earns.
 */
fun RuleSet.premiumCost(app: AppDef, star: Int): Long {
    val full = premiumBudget / 4.0 * Math.pow(2.0, (app.tier - 1) / 4.5)
    val share = premiumStarShare[star - 1]
    return (Math.round(full * share / 1_000 / 5) * 5).coerceAtLeast(5)
}

/** All monetization actions. Pure: state in, state out. */
class Shop(private val engine: Engine) {
    private val rules get() = engine.rules

    fun credit(s: GameState, n: Long, bought: Boolean = false): GameState = s.withAccount {
        it.copy(credits = it.credits + n, creditsFree = it.creditsFree + if (bought) 0 else n, creditsBought = it.creditsBought + if (bought) n else 0)
    }

    /**
     * Pays a store purchase at most once: [grant] runs only for an unseen [claimId], and the id is stored with the
     * result so the caller saves both in one transaction before confirming to the SDK. Retains previously granted ids across saves.
     */
    fun claimOnce(s: GameState, claimId: String, grant: (GameState) -> GameState?): GameState? {
        if (claimId.isBlank() || claimId in s.account.processedClaims) return null
        val paid = grant(s) ?: return null
        return paid.withAccount { it.copy(processedClaims = it.processedClaims + claimId) }
    }

    /** Deduplicate ad callbacks for seven UTC dates and keep the ad ledger bounded. */
    fun claimAdOnce(s: GameState, claimId: String, now: Long, grant: (GameState) -> GameState?): GameState? {
        if (claimId.isBlank() || claimId in s.account.processedClaims) return null
        val current = s.expireRewardClaims(now)
        val key = rewardKey(claimId)
        if (current.account.processedAdClaims.values.any { key in it }) return null
        val paid = grant(current) ?: return null
        return paid.withAccount { a ->
            val day = dayOf(now)
            val ads = a.processedAdClaims + (day to (a.processedAdClaims[day].orEmpty() + key))
            a.copy(processedAdClaims = ads.retainRecentAds(day))
        }
    }

    private fun spend(s: GameState, n: Long): GameState? =
        if (s.account.credits < n) null else s.withAccount { it.copy(credits = it.credits - n, creditsSpent = it.creditsSpent + n) }

    // Daily free credits -------------------------------------------------------------------

    /** The board day today's check-in would claim: the streak continues only from yesterday. */
    fun attendanceDay(s: GameState, now: Long): Int {
        val a = s.account
        val continues = a.attendDay == dayOf(now) - 1 && a.streak < rules.attendanceBoard.size
        return if (continues) a.streak + 1 else 1
    }

    /** 30-day consecutive attendance board: once per calendar day; a missed day starts again at day 1. */
    fun claimAttendance(s: GameState, now: Long): GameState {
        if (s.account.attendDay >= dayOf(now)) return s
        val day = attendanceDay(s, now)
        val r = rules.attendanceBoard[day - 1]
        return credit(s, r.data).withAccount { a ->
            val t = a.tickets.toMutableMap().also { m -> r.tickets.forEach { (k, n) -> m.merge(k, n, Int::plus) } }
            a.copy(attendDay = dayOf(now), streak = day, tickets = t, starVouchers = a.starVouchers + r.starVouchers, titles = a.titles + listOfNotNull(r.title))
        }
    }

    /** Spend a 프리미엄 ★ 선택권 on [appId]. */
    fun useStarVoucher(s: GameState, appId: String): GameState? {
        if (s.account.starVouchers <= 0 || !premiumOpen(s, appId) || s.account.stars(appId) >= rules.voucherMaxStars) return null
        return s.withAccount { it.copy(starVouchers = it.starVouchers - 1, premium = it.premium + (appId to it.stars(appId) + 1)) }
    }

    /** Spend a ticket: the item's effect, no data, no daily limit. */
    fun useTicket(s: GameState, item: CreditItem, now: Long): GameState? {
        val have = s.account.tickets[item] ?: 0
        if (have <= 0 || (item == CreditItem.BackupBoost && s.account.backupBoost)) return null
        return apply(s.withAccount { it.copy(tickets = it.tickets + (item to have - 1)) }, item, now)
    }

    /** Daily missions (the game decides when they are done): one payout per day. */
    fun claimMissions(s: GameState, now: Long): GameState {
        if (s.account.missionDay >= dayOf(now)) return s
        return credit(s, rules.missionCredits).withAccount { it.copy(missionDay = dayOf(now)) }
    }

    // Month pass -----------------------------------------------------------------------------

    fun passActive(s: GameState, now: Long) = now < s.account.passUntil

    /** Purchase: 30 days of ad-free rewards and daily credits, plus credits now. Stacks. */
    fun buyPass(s: GameState, now: Long): GameState =
        credit(s, rules.passInstantCredits, bought = true).withAccount { it.copy(passUntil = maxOf(now, it.passUntil) + PASS_DAYS * DAY_MS) }

    /** 터보·울트라: 30 days of permanent speed, the month pass included. */
    fun buySpeedPass(s: GameState, now: Long, pass: SpeedPass): GameState =
        buyPass(s, now).withAccount { it.copy(speedPass = pass, speedPassUntil = maxOf(now, it.speedPassUntil) + PASS_DAYS * DAY_MS) }

    fun claimPassDaily(s: GameState, now: Long): GameState {
        if (!passActive(s, now) || s.account.passDay >= dayOf(now)) return s
        val extra = s.account.speedPass?.takeIf { now < s.account.speedPassUntil }?.dailyData ?: 0
        return overclock(credit(s, rules.passDailyCredits + extra, bought = true).withAccount { it.copy(passDay = dayOf(now)) }, now, rules.passOverclockMinutes)
    }

    // Premium versions --------------------------------------------------------------------------

    /** Premium works on any app the player has ever unlocked; the stars stay through resets and syncs. */
    fun premiumOpen(s: GameState, appId: String): Boolean {
        val app = Catalog.app(appId)
        val ever = maxOf(s.backup.bestLevel, s.level)
        return app.unlockLevel <= ever && s.account.stars(appId) < rules.premiumMaxStars
    }

    fun premiumPrice(s: GameState, appId: String): Long = rules.premiumCost(Catalog.app(appId), s.account.stars(appId) + 1)

    fun upgradePremium(s: GameState, appId: String): GameState? {
        if (!premiumOpen(s, appId)) return null
        val paid = spend(s, premiumPrice(s, appId)) ?: return null
        return paid.withAccount { it.copy(premium = it.premium + (appId to it.stars(appId) + 1)) }
    }

    // Ads and the credit shop share their rewards --------------------------------------------

    fun adsLeft(s: GameState, slot: AdSlot, now: Long): Int {
        val a = s.account
        return if (a.adDay < dayOf(now)) slot.daily else slot.daily - (a.adUses[slot] ?: 0)
    }

    /**
     * One rewarded placement (an ad watched, or claimed directly with a month pass). [offerP] is the P the
     * offer doubles: the swipe for [AdSlot.BestDouble], the settlement for [AdSlot.OfflineDouble].
     */
    fun watchAd(s: GameState, slot: AdSlot, now: Long, offerP: BigInteger = BigInteger.ZERO): GameState? {
        if (adsLeft(s, slot, now) <= 0) return null
        if ((slot == AdSlot.BestDouble || slot == AdSlot.OfflineDouble) && offerP.signum() <= 0) return null
        val counted = s.withAccount { a ->
            val uses = if (a.adDay < dayOf(now)) emptyMap() else a.adUses
            val today = dayOf(now)
            val watched = if (passActive(s, now)) a.lastAdWatchDay else maxOf(a.lastAdWatchDay, today)
            a.copy(adDay = today, lastAdWatchDay = watched, adUses = uses + (slot to (uses[slot] ?: 0) + 1))
        }
        return when (slot) {
            AdSlot.BestDouble -> grantP(counted, offerP, rules.adLevelCap)
            AdSlot.OfflineDouble -> grantP(counted, offerP, rules.adLevelCap)
            AdSlot.Boost -> boost(counted, now)
            AdSlot.Warp -> warp(counted)
            AdSlot.Battery -> battery(counted)
            AdSlot.CreditBox -> credit(counted, rules.adCredits)
            AdSlot.Overclock -> overclock(counted, now, 60)
            AdSlot.FreeUpgrade -> engine.bestApp(counted).let { id -> counted.copy(appLevels = counted.appLevels + (id to (counted.appLevels[id] ?: 1) + rules.freeUpgradeLevels)) }
        }
    }

    fun price(item: CreditItem): Long = item.price

    fun shopLeft(s: GameState, item: CreditItem, now: Long): Int {
        val a = s.account
        return if (a.shopDay < dayOf(now)) item.daily else item.daily - (a.shopUses[item] ?: 0)
    }

    fun buyItem(s: GameState, item: CreditItem, now: Long): GameState? {
        if (shopLeft(s, item, now) <= 0) return null
        if (item == CreditItem.BackupBoost && s.account.backupBoost) return null
        val paid = spend(s, item.price) ?: return null
        val counted = paid.withAccount { a ->
            val uses = if (a.shopDay < dayOf(now)) emptyMap() else a.shopUses
            a.copy(shopDay = dayOf(now), shopUses = uses + (item to (uses[item] ?: 0) + 1))
        }
        return apply(counted, item, now)
    }

    private fun apply(s: GameState, item: CreditItem, now: Long): GameState = when (item) {
        CreditItem.Boost -> boost(s, now)
        CreditItem.Warp -> warp(s)
        CreditItem.Battery -> battery(s)
        CreditItem.BackupBoost -> s.withAccount { it.copy(backupBoost = true) }
        CreditItem.Overclock -> overclock(s, now, 60)
    }

    fun overclockActive(s: GameState, now: Long) = now < s.account.overclockUntil

    private fun overclock(s: GameState, now: Long, minutes: Int) = s.withAccount { it.copy(overclockUntil = maxOf(now, it.overclockUntil) + minutes * 60_000L) }

    fun boostActive(s: GameState, now: Long) = now < s.account.boostUntil

    private fun boost(s: GameState, now: Long) = s.withAccount { it.copy(boostUntil = maxOf(now, it.boostUntil) + rules.boostMinutes * 60_000L) }

    /** 시간 도약: [RuleSet.warpMinutes] of auto-clean income now (the recent rate at the auto share, no offline penalty). */
    private fun warp(s: GameState) = grantP(s, s.earnRate * BigInteger.valueOf(rules.warpMinutes.toLong() * rules.offlineAutoPermille) / BigInteger.valueOf(1_000), rules.adLevelCap)

    /** 보조배터리: full battery plus an overcharge above the cap. */
    private fun battery(s: GameState) = s.copy(energy = engine.energyCap(s) + rules.batteryOvercharge, nextChargeAt = 0)

    /** P granted outside a swipe; the one-shot level cap still applies (과열 보호). */
    fun grantP(s: GameState, p: BigInteger, levelCap: Int): GameState {
        val got = p.min(engine.jackpotRoom(s, levelCap)).max(BigInteger.ZERO)
        val run = s.runEarned + got
        return s.copy(wallet = s.wallet + got, runEarned = run, level = maxOf(s.level, rules.levelFor(run, s.backup.lv(BackupItem.FastBoot))))
    }
}
