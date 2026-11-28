package com.graviton94.notificationcleaner.core

import java.math.BigInteger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveEconomyTest {
    private val now = 100 * DAY_MS
    private val engine = Engine()

    private fun value(json: String, key: String) = Json.parseToJsonElement(json).jsonObject.getValue(key)
    private fun ok(json: String) = value(json, "ok").jsonPrimitive.boolean

    @Test
    fun collectionMigratesKnownOwnershipAndPaysEachMilestoneOnce() {
        val ownedApps = Catalog.apps.take(10).associate { it.id to 1 }
        val original = engine.newGame(now, 7).copy(appLevels = ownedApps, level = engine.rules.syncLevel)
            .withAccount { Account() }
        val oldSave = SaveCodec.encode(original)
        assertFalse("collectedApps" in oldSave)
        val session = WebSession(engine, SaveCodec.decode(oldSave), now)
        assertEquals(10, value(session.panel(now), "book").jsonObject.getValue("owned").jsonPrimitive.int)
        assertFalse(ok(session.claimBook(20, now)))
        assertFalse(ok(session.claimBook(11, now)))
        assertTrue(ok(session.claimBook(10, now)))
        assertFalse(ok(session.claimBook(10, now)))
        assertEquals(1, session.state.account.pieces)
        for (next in listOf(engine.factoryReset(session.state, now)!!, engine.sync(session.state, now)!!)) {
            val loaded = WebSession(engine, SaveCodec.decode(SaveCodec.encode(next)), now)
            assertEquals(10, CollectionRewards.owned(loaded.state).size)
            assertFalse(ok(loaded.claimBook(10, now)))
        }
        val afterBuy = engine.buy(engine.newGame(now, 7).copy(wallet = BigInteger.TEN.pow(6), level = 8), "phone")
        assertTrue("phone" in afterBuy.account.collectedApps)
    }

    @Test
    fun launchCollectionPaysCosmeticsAndExpandedCollectionPaysDataAndAVoucher() {
        val priorPacks = Catalog.packs
        Catalog.packs = setOf(ContentPack.AiTier)
        try {
            val owned = Catalog.apps.map { it.id }.toSet()
            val session = WebSession(engine, engine.newGame(now, 7).withAccount { it.copy(collectedApps = owned) }, now)
            assertTrue(ok(session.claimBook(60, now)))
            assertEquals(0L, session.state.account.credits)
            assertTrue(ok(session.claimBook(66, now)))
            assertEquals(30L, session.state.account.credits)
            assertEquals(30L, session.state.account.creditsFree)
            assertEquals(1, session.state.account.starVouchers)
            assertEquals(16, session.state.account.pieces)
        } finally {
            Catalog.packs = priorPacks
        }
    }

    @Test
    fun verifiedWeeklySettlementIsIdempotentEvenDuringChallenge() {
        val session = WebSession(now = now)
        assertFalse(ok(session.grantWeekly("", "first", now)))
        assertFalse(ok(session.grantWeekly("week-1", "unknown", now)))
        assertTrue(ok(session.grantWeekly("week-1", "top100", now)))
        assertFalse(ok(session.grantWeekly("week-1", "first", now)))
        assertTrue(ok(session.challengeStart(now)))
        assertTrue(ok(session.grantWeekly("week-2", "first", now)))
        val save = SaveCodec.decode(session.save())
        assertEquals(11, save.account.pieces)
        assertEquals(0L, save.account.credits)
        assertFalse(ok(session.grantWeekly("week-2", "first", now)))
        assertFalse(ok(WebSession(state = save, now = now).grantWeekly("week-1", "top100", now)))
        assertEquals(WeeklyTier.Top10, WeeklyTier.forRank(10))
        assertEquals(WeeklyTier.Top100, WeeklyTier.forRank(11))
        assertNull(WeeklyTier.forRank(0))
    }

    @Test
    fun configFallsBackAndDoesNotChangeSavedOrChallengeState() {
        val veteran = engine.newGame(now, 9).copy(level = 124)
        val session = WebSession(state = veteran, now = now)
        val snapshotBefore = session.save()
        val window = """{"id":"chain_week","startAt":$now,"endAt":${now + 1000}}"""
        session.applyTuning("""{"event":$window,"veteranOfflinePercentDelta":1,"eventBonusScalePermille":1100}""")
        assertEquals(10, session.engine.offlinePercent(veteran))
        assertEquals(9, session.engine.offlinePercent(veteran.copy(level = 123)))
        assertEquals("chain_week", value(session.snapshot(now), "liveEvent").jsonObject.getValue("id").jsonPrimitive.content)
        assertFalse("liveEvent" in session.snapshot(now + 1000))
        assertEquals(snapshotBefore, session.save())
        assertTrue(ok(session.challengeStart(now)))
        assertEquals(LiveTuning(), session.engine.tuning)
        assertTrue(ok(session.challengeEnd(now)))
        assertEquals(10, session.engine.offlinePercent(veteran))
        session.applyTuning("""{"veteranOfflinePercentDelta":2,"eventBonusScalePermille":"950","event":{"id":"missing","startAt":0,"endAt":1}}""")
        assertEquals(LiveTuning(), session.engine.tuning)
        assertEquals(LiveTuning(), LiveTuning.parse("""{"event":{"id":"chain_week","startAt":1,"endAt":604800002}}"""))
        session.applyTuning("invalid")
        assertEquals(LiveTuning(), session.engine.tuning)
    }

    @Test
    fun eventUsesTheNormalCapAndDoesNotRaiseTheOfflineRate() {
        val model = Engine(tuning = LiveTuning(event = LiveEventWindow(LiveEvent.ChainWeek, now, now + DAY_MS)))
        var state = engine.newGame(now, 7)
        var paidSwipes = 0
        var untouchedSwipes = 0
        repeat(500) { i ->
            val time = now + (i + 1) * 5_000L
            val target = engine.bestTarget(state, time) ?: return@repeat
            val raw = engine.swipe(state, target, time) ?: return@repeat
            val live = model.swipe(state, target, time)!!
            val without = model.swipe(state, target, now - 1)
            assertEquals(raw.state.earnRate, live.state.earnRate)
            assertEquals(raw.earned + live.liveEventExtra, live.earned)
            if (live.liveEventExtra.signum() > 0) {
                paidSwipes++
                assertTrue(live.beats.count { it is Beat.Burst } >= 3)
            } else untouchedSwipes++
            assertTrue(without == null || without.liveEventExtra.signum() == 0)
            assertEquals(BigInteger.ZERO, model.swipe(state, target, time, Actor.Auto)?.liveEventExtra ?: BigInteger.ZERO)
            state = raw.state
        }
        assertTrue(paidSwipes > 0)
        assertTrue(untouchedSwipes > 0)
    }

    @Test
    fun rankedAndCollectionClaimsStayCompactAlongsideAFullYearOfAds() {
        val shop = Shop(engine)
        var s = engine.newGame(now, 9).withAccount { it.copy(collectedApps = Catalog.apps.map { app -> app.id }.toSet()) }
        val launchGoals = CollectionRewards.milestones.keys.filter { it <= Catalog.apps.size }.toSet()
        for (at in launchGoals) s = CollectionRewards.claimBook(s, at, shop)!!
        repeat(365) { day ->
            repeat(5) { i ->
                s = shop.claimAdOnce(s, "ad-day-$day-$i", now + day * DAY_MS) { it }!!
            }
            if (day % 7 == 0) s = CollectionRewards.grantWeekly(s, "week-$day", "participant", shop)!!
        }
        val stored = SaveCodec.encode(s)
        assertTrue(stored.toByteArray().size <= 20_000)
        val roundTrip = SaveCodec.decode(stored)
        assertEquals(53, roundTrip.account.processedClaims.size)
        assertEquals(launchGoals, roundTrip.account.claimedBook)
    }
}
