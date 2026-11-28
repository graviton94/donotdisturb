import com.graviton94.notificationcleaner.core.*
import java.io.File

private class OpeningAudit : DailyAudit {
    override val throughDay = 1
    var start = -1L
    private val seen = mutableSetOf<String>()
    val moments = sortedMapOf<Long, MutableList<String>>()

    override fun observe(engine: Engine, state: GameState, now: Long) {
        val first = start < 0
        if (first) start = now
        fun note(key: String) {
            if (seen.add(key) && !first) moments.getOrPut(now - start) { mutableListOf() } += key
        }
        for (level in 1..state.level) note("level:$level")
        for (app in Catalog.apps) {
            if (engine.unlocked(state, app.id)) note("unlock:${app.id}")
            if (engine.canBuy(state, app.id)) note("affordable:${app.id}")
        }
        for (card in EventCard.entries) {
            if (state.level >= card.unlockLevel) note("card:${card.name}")
            if (state.level >= card.unlockLevel && state.wallet >= engine.rules.cardPrice(card))
                note("card-affordable:${card.name}")
        }
        for (assist in Assist.entries) if (state.level >= assist.unlockLevel) note("assist:${assist.name}")
        for (app in Catalog.apps) if (app.id in state.appLevels && state.level >= engine.proLevel(state, app.id)) {
            note("pro:${app.id}")
            if (app.id in state.pro || state.wallet >= engine.proPrice(state, app.id)) note("pro-affordable:${app.id}")
        }
        for (slot in 1..engine.maxSlots(state)) note("slot:$slot")
    }

    override fun swipe(engine: Engine, before: GameState, outcome: SwipeOutcome, now: Long) = Unit
    override fun prestige(kind: String, before: GameState, after: GameState, now: Long, gain: Double, stalls: Int, sessions: Int) = Unit
    override fun endDay(day: Int, state: GameState) = Unit
}

fun opening(out: File, seeds: Int) {
    require(screenOnly)
    out.parentFile?.mkdirs()
    out.printWriter().use { csv ->
        csv.println("seed,elapsed_ms,events")
        for (seed in 1..seeds) {
            val audit = OpeningAudit()
            playDays(seed.toLong(), 1, DayPlan(sessions = 1, sessionMin = 60, manualFloorMs = 4_500),
                resets = false, audit = audit)
            for ((time, events) in audit.moments) csv.println("$seed,$time,${events.joinToString("|")}")
        }
    }
}
