package com.graviton94.notificationcleaner.core

data class CollectionReward(val data: Long = 0, val pieces: Int, val vouchers: Int = 0) {
    internal fun grant(s: GameState, shop: Shop): GameState = (if (data == 0L) s else shop.credit(s, data))
        .withAccount { it.copy(pieces = it.pieces + pieces, starVouchers = it.starVouchers + vouchers) }
}

enum class WeeklyTier(val id: String, val pieces: Int) {
    First("first", 8), Top10("top10", 5), Top100("top100", 3), Participant("participant", 1);

    companion object {
        fun forRank(rank: Int): WeeklyTier? = when (rank) {
            1 -> First
            in 2..10 -> Top10
            in 11..100 -> Top100
            in 101..Int.MAX_VALUE -> Participant
            else -> null
        }
    }
}

object CollectionRewards {
    val milestones = mapOf(
        10 to CollectionReward(pieces = 1),
        20 to CollectionReward(pieces = 2),
        30 to CollectionReward(pieces = 3),
        40 to CollectionReward(pieces = 4),
        50 to CollectionReward(pieces = 6),
        60 to CollectionReward(pieces = 8),
        66 to CollectionReward(data = 30, pieces = 8, vouchers = 1),
    )

    fun owned(s: GameState): Set<String> = s.account.collectedApps + s.appLevels.keys

    fun claimBook(s: GameState, at: Int, shop: Shop): GameState? {
        val reward = milestones[at] ?: return null
        if (at in s.account.claimedBook || owned(s).count { Catalog.hasApp(it) } < at) return null
        return reward.grant(s, shop).withAccount { it.copy(claimedBook = it.claimedBook + at) }
    }

    /** The host supplies an authenticated settlement id stable for the account and UTC ISO week. */
    fun grantWeekly(s: GameState, claimId: String, tier: String, shop: Shop): GameState? {
        if (claimId.isBlank() || claimId.length > 128) return null
        val reward = WeeklyTier.entries.firstOrNull { it.id == tier } ?: return null
        return shop.claimOnce(s, "weekly:${rewardKey(claimId)}") { CollectionReward(pieces = reward.pieces).grant(it, shop) }
    }
}

internal fun GameState.rememberCollection(): GameState {
    val known = CollectionRewards.owned(this)
    return if (known == account.collectedApps) this else withAccount { it.copy(collectedApps = known) }
}
