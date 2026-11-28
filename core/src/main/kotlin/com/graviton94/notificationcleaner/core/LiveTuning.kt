package com.graviton94.notificationcleaner.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** Scheduled content packs for ordinary play. The catalog and saved content version stay independent. */
enum class LiveEvent(val id: String, val title: String, val bonusPermille: Int) {
    ChainWeek("chain_week", "연쇄 주간", 120),
    AsiaWeek("asia_week", "아시아권 시너지 주간", 180);

    internal fun qualifies(deck: List<String>, bursts: List<Row>): Boolean = when (this) {
        ChainWeek -> bursts.size >= 3
        AsiaWeek -> deck.count { Catalog.app(it).region == Region.Asia } >= 2 &&
            bursts.any { it.app?.let { app -> Catalog.app(app).region == Region.Asia } == true }
    }
}

data class LiveEventWindow(val event: LiveEvent, val startAt: Long, val endAt: Long) {
    init {
        require(startAt >= 0 && endAt > startAt && endAt - startAt <= 7 * 86_400_000L)
    }

    fun active(now: Long) = now >= startAt && now < endAt
}

data class LiveTuning(
    val veteranOfflinePercentDelta: Int = 0,
    val eventBonusScalePermille: Int = 1_000,
    val event: LiveEventWindow? = null,
) {
    init {
        require(veteranOfflinePercentDelta in -1..1 && eventBonusScalePermille in 900..1_100)
    }

    fun eventAt(now: Long): LiveEventWindow? = event?.takeIf { it.active(now) }

    companion object {
        /** A fetched document replaces the last one. Each invalid or absent field falls back independently. */
        fun parse(json: String): LiveTuning {
            val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return LiveTuning()
            fun JsonObject.number(key: String) = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }
            val offline = root.number("veteranOfflinePercentDelta")?.intOrNull?.takeIf { it in -1..1 } ?: 0
            val scale = root.number("eventBonusScalePermille")?.intOrNull?.takeIf { it in 900..1_100 } ?: 1_000
            val window = (root["event"] as? JsonObject)?.let { obj ->
                val id = (obj["id"] as? JsonPrimitive)?.contentOrNull
                val event = LiveEvent.entries.firstOrNull { it.id == id } ?: return@let null
                val from = obj.number("startAt")?.longOrNull ?: return@let null
                val to = obj.number("endAt")?.longOrNull ?: return@let null
                if (from < 0 || to <= from || to - from > 7 * 86_400_000L) null else LiveEventWindow(event, from, to)
            }
            return LiveTuning(offline, scale, window)
        }
    }
}
