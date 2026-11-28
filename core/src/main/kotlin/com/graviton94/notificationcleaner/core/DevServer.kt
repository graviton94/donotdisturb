package com.graviton94.notificationcleaner.core

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.net.URLDecoder

/**
 * Desktop stand-in for the Android bridge: serves the game page and answers the same calls from one [WebSession],
 * so the page can be played and screenshotted in a browser. `gradle -p core run --args="serve 8765 ../app/src/main/assets/game"`
 * Query `?seed=` on /api/new restarts with a fixed seed.
 */
fun serve(port: Int, root: File, initialSave: File? = null) {
    var session = WebSession(state = initialSave?.readText()?.let(SaveCodec::decode))
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
    server.createContext("/api/") { ex ->
        val q = (ex.requestURI.rawQuery ?: "").split("&").filter { "=" in it }
            .associate { it.substringBefore("=") to URLDecoder.decode(it.substringAfter("="), "UTF-8") }
        val now = q["now"]?.toLongOrNull() ?: System.currentTimeMillis()
        val body = when (ex.requestURI.path.removePrefix("/api/")) {
            "new" -> { val seed = q["seed"]?.toLongOrNull() ?: now; session = WebSession(state = Engine().newGame(now, seed), now = now); session.snapshot(now) }
            "snapshot" -> session.snapshot(now)
            "sync" -> session.sync(now)
            "meta" -> session.meta(now)
            "challengeStart" -> session.challengeStart(now)
            "challengeEnd" -> session.challengeEnd(now)
            "buyCloud" -> session.buyCloud(q.getValue("item"), now)
            "info" -> """{"version":"dev","package":"devserver"}"""
            "wipe" -> { session = WebSession(now = now); session.snapshot(now) }
            "swipe" -> session.swipe(q.getValue("id").toLong(), now)
            "auto" -> session.auto(now)
            "hint" -> session.hint(now)
            "panel" -> session.panel(now)
            "bm" -> session.bm(now)
            "buyData" -> session.buyData(q.getValue("pack"), "fake-" + System.nanoTime(), now)
            "buyStarter" -> session.buyStarter("fake-" + System.nanoTime(), now)
            "buyRookie" -> session.buyRookie("fake-" + System.nanoTime(), now)
            "claimRookie" -> session.claimRookie(now)
            "buyItem" -> session.buyItem(q.getValue("item"), now)
            "watchAd" -> session.watchAd(q.getValue("slot"), "fakead-" + System.nanoTime(), now)
            "buyPass" -> session.buyPass(q.getValue("kind"), "fake-" + System.nanoTime(), now)
            "premium" -> session.premium(q.getValue("app"), q["voucher"] == "1", now)
            "claimMissions" -> session.claimMissions(now)
            "attend" -> session.attend(now)
            "drill" -> session.tapDrill(now)
            "upgradeN" -> session.upgradeN(q["app"] ?: "", q["n"] ?: "1", now)
            "setDeck" -> session.setDeck((q["apps"] ?: "").split(",").filter { it.isNotBlank() }, now)
            "buyBackup" -> session.buyBackup(q.getValue("item"), now)
            "buyPro" -> session.buyPro(q.getValue("app"), now)
            "claimBook" -> session.claimBook(q.getValue("at").toInt(), now)
            "buyEvent" -> session.buyEvent(q.getValue("card"), now)
            "buyAssist" -> session.buyAssist(q.getValue("item"), now)
            "dnd" -> session.dnd(now)
            "setCards" -> session.setCards((q["cards"] ?: "").split(",").filter { it.isNotBlank() }, now)
            // dev only: put a special row on top (Call, Spam, Lotto or any EventCard / Alert name)
            "row" -> { val k = q["kind"] ?: "Call"; val st = session.state; val id = st.nextId
                val row = when {
                    k == "Call" -> Row(id, RowKind.Call); k == "Spam" -> Row(id, RowKind.Spam)
                    Alert.entries.any { it.name == k } -> Row(id, RowKind.Alert, alert = Alert.valueOf(k))
                    else -> Row(id, RowKind.Event, event = EventCard.valueOf(k)) }
                session = WebSession(state = st.copy(rows = listOf(row) + st.rows.drop(1), nextId = id + 1, lastEarned = st.lastEarned.max(java.math.BigInteger.TEN)), now = now); session.snapshot(now) }
            // dev only: a huge wallet (10^digits) to check the screen never turns amounts into floats
            "bigWallet" -> { val d = q["digits"]?.toInt() ?: 400
                session = WebSession(state = session.state.copy(wallet = java.math.BigInteger.TEN.pow(d) + java.math.BigInteger.valueOf(123_456_789)), now = now); session.snapshot(now) }
            // dev only: jump to a level with its run P (and wallet), to try the late panels
            "cheat" -> { val lv = q["level"]?.toInt() ?: 30; val r = session.engine.rules; val p = r.levelCeil(lv, 0)
                session = WebSession(state = session.state.copy(level = lv, runEarned = p, wallet = p * java.math.BigInteger.valueOf((q["wallet"] ?: "3").toLong()), appLevels = q["apps"]?.split(",")?.mapIndexed { i, v -> session.state.appLevels.keys.elementAtOrNull(i) to v.toInt() }?.filter { it.first != null }?.associate { it.first!! to it.second }?.let { session.state.appLevels + it } ?: session.state.appLevels), now = now); session.snapshot(now) }
            "buy" -> session.buy(q.getValue("app"), now)
            "upgrade" -> session.upgrade(q.getValue("app"), now)
            "reset" -> session.reset(now)
            "offline" -> session.offline(q.getValue("ms").toLong(), now)
            else -> "{}"
        }.toByteArray()
        ex.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        ex.sendResponseHeaders(200, body.size.toLong()); ex.responseBody.use { it.write(body) }
    }
    server.createContext("/") { ex ->
        val path = ex.requestURI.path.trimStart('/').ifEmpty { "index.html" }
        val f = File(root, path).canonicalFile
        if (!f.path.startsWith(root.canonicalPath) || !f.isFile) { ex.sendResponseHeaders(404, -1); ex.close(); return@createContext }
        val type = when (f.extension) { "html" -> "text/html; charset=utf-8"; "js" -> "text/javascript"; "json" -> "application/json"; "svg" -> "image/svg+xml"; "png" -> "image/png"; "ogg" -> "audio/ogg"; "css" -> "text/css"; "woff2" -> "font/woff2"; else -> "application/octet-stream" }
        ex.responseHeaders.add("Content-Type", type)
        val bytes = f.readBytes(); ex.sendResponseHeaders(200, bytes.size.toLong()); ex.responseBody.use { it.write(bytes) }
    }
    server.start()
    println("serving ${root.canonicalPath} on http://127.0.0.1:$port")
}
