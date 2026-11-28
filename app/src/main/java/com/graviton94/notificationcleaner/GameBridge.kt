package com.graviton94.notificationcleaner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.webkit.JavascriptInterface
import com.graviton94.notificationcleaner.core.AwayClock
import com.graviton94.notificationcleaner.core.SaveCodec
import com.graviton94.notificationcleaner.core.WebSession
import com.graviton94.notificationcleaner.core.account
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.graviton94.notificationcleaner.platform.Account
import com.graviton94.notificationcleaner.platform.Ads
import com.graviton94.notificationcleaner.platform.Billing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.graviton94.notificationcleaner.platform.Telemetry
import org.json.JSONObject
import java.io.File

/**
 * window.Game for the game page (assets/game): every call runs the core engine through [WebSession] and answers JSON.
 * Calls arrive on the WebView's bridge thread; the lock also covers saves from the UI thread.
 */
class GameBridge(private val context: Context) {
    private val debugTransactions = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val saveFile = File(context.filesDir, "save.json")
    private val prefs = context.getSharedPreferences("clock", 0)
    private val lock = Any()
    // var: 설정의 "게임 데이터 초기화" replaces the whole session (privacy policy §데이터 삭제)
    private var session: WebSession = synchronized(lock) {
        val saved = runCatching { SaveCodec.decode(saveFile.readText()) }.getOrNull()
        WebSession(state = saved, now = now())
    }
    // Remote Config knobs (platform/Tuning): applied to every session this bridge creates, never saved.
    private var tuning = "{}"
    /** Calls WebSession.applyTuning(json) once the balance session's core has it (v1.6 §5-1); a no-op before. */
    private fun tune(s: WebSession) {
        if (tuning == "{}") return
        runCatching { s.javaClass.getMethod("applyTuning", String::class.java).invoke(s, tuning) }
    }
    fun applyTuning(json: String) = synchronized(lock) { tuning = json; tune(session) }

    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private fun now() = System.currentTimeMillis()

    // An empty receipt is rejected by the reward ledgers.
    private fun debugReceipt(kind: String): String = if (debugTransactions) "$kind-${System.nanoTime()}" else ""

    @JavascriptInterface
    fun call(method: String, argsJson: String): String = synchronized(lock) {
        val a = runCatching { JSONObject(argsJson) }.getOrDefault(JSONObject())
        val t = now()
        val previousPassDay = session.state.account.passDay
        val out = when (method) {
            "snapshot" -> session.snapshot(t)
            "sync" -> session.sync(t)
            "meta" -> session.meta(t)
            "challengeStart" -> session.challengeStart(t)
            "challengeEnd" -> session.challengeEnd(t)
            "buyCloud" -> session.buyCloud(a.optString("item"), t)
            "info" -> JSONObject().put("billing", billing?.ready == true).put("version", runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "").put("package", context.packageName).put("adPrivacy", ads?.privacyRequired == true).toString()
            "wipe" -> { saveFile.delete(); File(saveFile.path + ".tmp").delete(); prefs.edit().clear().apply(); session = WebSession(now = t); tune(session); session.snapshot(t) }
            "swipe" -> session.swipe(a.optString("id").toLong(), t)
            "auto" -> session.auto(t)
            "hint" -> session.hint(t)
            "buy" -> session.buy(a.optString("app"), t)
            "upgrade" -> session.upgrade(a.optString("app"), t)
            "reset" -> session.reset(t)
            "panel" -> session.panel(t)
            "bm" -> session.bm(t)
            // Play purchases go through Game.purchase. The fallback is restricted to debug builds.
            "buyData" -> if (billing?.ready == true) session.snapshot(t) else session.buyData(a.optString("pack"), debugReceipt("fake"), t)
            "buyStarter" -> if (billing?.ready == true) session.snapshot(t) else session.buyStarter(debugReceipt("fake"), t)
            "buyRookie" -> if (billing?.ready == true) session.snapshot(t) else session.buyRookie(debugReceipt("fake"), t)
            "claimRookie" -> session.claimRookie(t)
            "buyItem" -> session.buyItem(a.optString("item"), t)
            "watchAd" -> {
                val receipt = if (session.shop.passActive(session.state, t)) "pass-" + java.util.UUID.randomUUID()
                    else adRewards.removeFirstOrNull() ?: debugReceipt("fakead")
                session.watchAd(a.optString("slot"), receipt, t)
            }
            "buyPass" -> if (billing?.ready == true) session.snapshot(t) else session.buyPass(a.optString("kind"), debugReceipt("fake"), t)
            "premium" -> session.premium(a.optString("app"), a.optString("voucher") == "1", t)
            "claimMissions" -> session.claimMissions(t)
            "attend" -> session.attend(t)
            "drill" -> session.tapDrill(t)
            "upgradeN" -> session.upgradeN(a.optString("app"), a.optString("n", "1"), t)
            "setDeck" -> session.setDeck(a.optString("apps").split(",").filter { it.isNotBlank() }, t)
            "buyBackup" -> session.buyBackup(a.optString("item"), t)
            "buyPro" -> session.buyPro(a.optString("app"), t)
            "claimBook" -> session.claimBook(a.optInt("at"), t)
            "buyEvent" -> session.buyEvent(a.optString("card"), t)
            "buyAssist" -> session.buyAssist(a.optString("item"), t)
            "dnd" -> session.dnd(t)
            "setCards" -> session.setCards(a.optString("cards").split(",").filter { it.isNotBlank() }, t)
            else -> "{}"
        }
        val changesState = when (method) {
            "snapshot", "hint", "panel", "drill", "bm" -> false
            else -> true
        }
        if (changesState || previousPassDay != session.state.account.passDay) save()
        out
    }

    /** Set by MainActivity: the rewarded-ad host, billing, sign-in and a way to call back into the page. */
    var ads: Ads? = null
    var js: ((String) -> Unit)? = null
    var activity: ComponentActivity? = null
    var billing: Billing? = null
    var account: Account? = null
    private val signingIn = java.util.concurrent.atomic.AtomicBoolean(false)
    /** While a sign-in is out to KakaoTalk / the Google sheet, coming back is not "returning to the game". */
    val isSigningIn get() = signingIn.get()
    /** Reload the page after the save was replaced by the cloud copy. */
    var reload: (() -> Unit)? = null
    private var cloudPending: Account.Cloud? = null
    /** Rewards AdMob confirmed and the page has not cashed in yet (one watchAd each). */
    private val adRewards = ArrayDeque<String>()

    private fun cb(fn: String, vararg args: Any?) {
        val a = args.joinToString(",") { v -> when (v) { null -> "null"; is String -> JSONObject.quote(v); is Boolean, is Number -> v.toString(); is JSONObject -> v.toString(); else -> JSONObject.quote(v.toString()) } }
        js?.invoke("window.$fn && window.$fn($a)")
    }

    // ---- Play Billing: the page asks, Play charges, the core grants once per claim id
    /** Called by [Billing] for every paid purchase; true when the core granted it now. */
    fun grant(p: Billing.Product, claim: String): Boolean = synchronized(lock) {
        val t = now()
        val out = when (p.kind) {
            "data" -> session.buyData(p.coreId, claim, t)
            "starter" -> session.buyStarter(claim, t)
            "rookie" -> session.buyRookie(claim, t)
            "pass" -> session.buyPass(p.coreId, claim, t)
            else -> return false
        }
        save()
        runCatching { JSONObject(out).optBoolean("ok", false) }.getOrDefault(false)
    }

    /** {ready, prices: {playId: "₩5,500"}} — the page shows store prices when billing is up. */
    @JavascriptInterface
    fun store(): String = JSONObject().put("ready", billing?.ready == true).put("prices", billing?.prices() ?: JSONObject()).toString()

    /** Buy a Play product; answer: window.onPurchase(id, ok, reason). */
    @JavascriptInterface
    fun purchase(id: String, playId: String) {
        val act = activity; val b = billing
        if (act == null || b == null) return cb("onPurchase", id, false, "unavailable")
        act.runOnUiThread { b.buy(act, playId) { ok, reason -> cb("onPurchase", id, ok, reason) } }
    }

    // ---- account: Google / Kakao sign-in, cloud save, deletion
    @JavascriptInterface
    fun account(): String = (account?.status() ?: JSONObject().put("enabled", false)).toString()

    /** provider google|kakao; answer: window.onAccount(id, ok, reason, info). reason "conflict" carries both saves. */
    @JavascriptInterface
    fun signIn(id: String, provider: String) {
        val act = activity ?: return cb("onAccount", id, false, "unavailable", null)
        val acc = account?.takeIf { it.enabled } ?: return cb("onAccount", id, false, "unavailable", null)
        // one flow at a time: a second Kakao/Google flow on top of a running one cancels it (the "bounce" testers saw)
        if (!signingIn.compareAndSet(false, true)) return cb("onAccount", id, false, "busy", null)
        act.lifecycleScope.launch {
            try {
                // the provider sheet itself has no limit (the player may be typing a password); the server part does
                if (provider == "kakao") acc.signInKakao(act) else acc.signInGoogle(act)
                kotlinx.coroutines.withTimeout(30_000) { afterSignIn(id, acc) }
            } catch (e: CancellationException) {
                if (e is kotlinx.coroutines.TimeoutCancellationException) cb("onAccount", id, false, "error", JSONObject().put("type", "Timeout")) else throw e
            } catch (e: Exception) {
                val cancel = e.javaClass.simpleName.contains("Cancel") || (e.message ?: "").contains("cancel", true)
                android.util.Log.w("Account", "sign-in $provider failed: ${e.javaClass.simpleName} ${e.message}")
                cb("onAccount", id, false, if (cancel) "cancel" else "error", JSONObject().put("type", e.javaClass.simpleName))
            } finally { signingIn.set(false) }
        }
    }

    private suspend fun afterSignIn(id: String, acc: Account) {
        val cloud = runCatching { acc.download() }.getOrNull()
        val (local, level, fresh) = synchronized(lock) { Triple(session.save(), session.state.level, session.state.level <= 2 && session.state.swipes < 30) }
        when {
            cloud == null -> { acc.upload(local, level); cb("onAccount", id, true, "uploaded", JSONObject(acc.status().toString())) }
            fresh -> { useCloud(cloud); cb("onAccount", id, true, "restored", JSONObject(acc.status().toString())) }
            else -> {
                cloudPending = cloud
                val info = JSONObject(acc.status().toString()).put("local", JSONObject().put("level", level)).put("cloud", JSONObject().put("level", cloud.level).put("savedAt", cloud.savedAt))
                cb("onAccount", id, true, "conflict", info)
            }
        }
    }

    /** After a "conflict": keep this phone's game (upload it) or take the cloud one (replace and reload). */
    @JavascriptInterface
    fun resolveSave(choice: String) {
        val acc = account ?: return
        val act = activity ?: return
        val cloud = cloudPending; cloudPending = null
        act.lifecycleScope.launch {
            if (choice == "cloud" && cloud != null) useCloud(cloud)
            else { val (save, lv) = synchronized(lock) { session.save() to session.state.level }; acc.upload(save, lv) }
        }
    }

    private fun useCloud(cloud: Account.Cloud) {
        val st = runCatching { SaveCodec.decode(cloud.save) }.getOrNull() ?: return
        synchronized(lock) { session = WebSession(state = st, now = now()); tune(session); save() }
        reload?.invoke()
    }

    @JavascriptInterface
    fun signOut(): String { account?.signOut(); return account() }

    /** Two taps in the page, then here: server data and the Firebase user go; this phone keeps playing as a guest. */
    @JavascriptInterface
    fun deleteAccount(id: String) {
        val act = activity ?: return
        val acc = account ?: return cb("onAccount", id, false, "unavailable", null)
        act.lifecycleScope.launch {
            try { acc.deleteAccount(); cb("onAccount", id, true, "deleted", JSONObject(acc.status().toString())) }
            catch (e: CancellationException) { throw e } catch (e: Exception) { cb("onAccount", id, false, "error", JSONObject().put("type", e.javaClass.simpleName)) }
        }
    }

    /** Weekly ranking: answers come back as window.onRank(id, ok, json). board = "<ISO week>-L<league>". */
    @JavascriptInterface
    fun rankSubmit(id: String, board: String, scoreLog: Double, scoreShort: String) {
        val act = activity ?: return cb("onRank", id, false, "unavailable", null)
        val acc = account?.takeIf { it.enabled && it.uid != null } ?: return cb("onRank", id, false, "guest", null)
        act.lifecycleScope.launch {
            try { acc.submitWeekly(board, scoreLog, scoreShort); cb("onRank", id, true, "ok", null) }
            catch (e: CancellationException) { throw e } catch (e: Exception) { cb("onRank", id, false, "error", JSONObject().put("type", e.javaClass.simpleName)) }
        }
    }

    @JavascriptInterface
    fun rankResult(id: String, board: String) {
        val act = activity ?: return cb("onRank", id, false, "unavailable", null)
        val acc = account?.takeIf { it.enabled && it.uid != null } ?: return cb("onRank", id, false, "guest", null)
        act.lifecycleScope.launch {
            try { cb("onRank", id, true, "ok", acc.weeklyResult(board)) }
            catch (e: CancellationException) { throw e } catch (e: Exception) { cb("onRank", id, false, "error", JSONObject().put("type", e.javaClass.simpleName)) }
        }
    }

    @JavascriptInterface
    fun rankTop(id: String, board: String) {
        val act = activity ?: return cb("onRank", id, false, "unavailable", null)
        val acc = account?.takeIf { it.enabled } ?: return cb("onRank", id, false, "unavailable", null)
        act.lifecycleScope.launch {
            try { cb("onRank", id, true, "ok", acc.topWeekly(board)) }
            catch (e: CancellationException) { throw e } catch (e: Exception) { cb("onRank", id, false, "error", JSONObject().put("type", e.javaClass.simpleName)) }
        }
    }

    /** Cloud backup of the save while signed in (MainActivity: on pause and every few minutes). */
    fun backup() {
        val acc = account ?: return
        if (acc.uid == null) return
        val (save, lv) = synchronized(lock) { session.save() to session.state.level }
        activity?.lifecycleScope?.launch { acc.upload(save, lv) }
    }

    /** Page asks for a rewarded ad; the answer comes back as window.onAdResult(id, earned, reason). */
    @JavascriptInterface
    fun showAd(id: String) {
        val q = JSONObject.quote(id)
        val a = ads ?: return run { js?.invoke("window.onAdResult && window.onAdResult($q, false, 'none')") }
        a.show { earned, reason ->
            if (earned) synchronized(lock) { adRewards.add("ad-" + java.util.UUID.randomUUID()) }
            js?.invoke("window.onAdResult && window.onAdResult($q, $earned, ${JSONObject.quote(reason)})")
        }
    }

    /** Settings › 광고 개인정보 설정: the UMP privacy options form. */
    @JavascriptInterface
    fun adPrivacy() { ads?.showPrivacy() }

    /** Analytics event from the page (no-op without Firebase). */
    @JavascriptInterface
    fun log(name: String, paramsJson: String) = Telemetry.event(name, paramsJson)

    @JavascriptInterface
    fun buzz(ms: Int) {
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(ms.toLong().coerceIn(5, 80), VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    /** On return: pay the time away (clock-tamper safe) and claim the daily attendance; null when nothing to show. */
    fun resume(): String? = synchronized(lock) {
        val lastWall = prefs.getLong("wall", 0L)
        if (lastWall <= 0) { save(); return null }
        val away = AwayClock.awayMs(lastWall, now(), prefs.getLong("elapsed", 0L), SystemClock.elapsedRealtime(), sameBoot = prefs.getInt("boot", -2) == bootCount())
        if (away < 60_000) return null
        val out = session.offline(away, now())
        save()
        out
    }

    fun save() = synchronized(lock) {
        val tmp = File(saveFile.path + ".tmp")
        tmp.writeText(session.save())
        tmp.renameTo(saveFile)
        prefs.edit().putLong("wall", now()).putLong("elapsed", SystemClock.elapsedRealtime()).putInt("boot", bootCount()).apply()
    }

    private fun bootCount() = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(-1)
}
