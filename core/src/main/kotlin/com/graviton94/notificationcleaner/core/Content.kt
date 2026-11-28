package com.graviton94.notificationcleaner.core

/**
 * Reach, in layers: stock apps, three regional markets, then the global giants. Region synergy counts apps of the same
 * region in the deck; stock apps get none. Tiers follow the layers: stock 1-2, regional 3-8, global 9-10.
 */
enum class Region(val label: String, val id: String) {
    Stock("기본 앱", "stock"), Asia("아시아권", "asia"), SoutheastAsia("동남아권", "southeast_asia"),
    West("서양권", "west"), Global("글로벌", "global"),
}

/** App category. Category synergy counts apps of the same category in the deck. */
enum class Category(val label: String) { Social("소통"), Work("업무"), Daily("생활"), Media("미디어"), Money("금융") }

/**
 * One parody app (docs/content/notification-catalog.html). Tiers follow real-world reach: Android's stock apps at
 * tiers 1-2, the world's biggest platforms at tier 10. Names and icons parody the real app; never identical. [order] is its place inside the tier and
 * sets its price step. Unique effects are catalogued but not part of rules v3 (planned for v3.1).
 */
data class AppDef(
    val id: String,
    val name: String,
    val tier: Int,
    val order: Int,
    val region: Region,
    val category: Category,
    val unlockLevel: Int,
    /** Factory resets needed before the shop offers it: late apps are run-count goals as well as level goals. */
    val minResets: Int = 0,
)

/** Event cards are bought with P and equipped in their own slots. Only equipped cards appear on the board. */
enum class EventCard(val label: String, val unlockLevel: Int, val price: Long, val everySwipes: Int) {
    /** Swiping it upgrades one active app by one level for free. */
    Update("업데이트", 4, 800, 40),

    /** The next five arriving rows are A·B·A·B·A, a double sandwich. */
    AllHands("@모두 공지", 14, 30_000, 30),

    /** Swiping it within [RuleSet.otpLifeSwipes] pays 10× the previous swipe; then it expires. */
    Otp("인증번호", 16, 60_000, 25),

    /** The next app bought costs half. Price 0 = priced from the level curve (RuleSet.cardPrice). */
    Sale("앱스토어 세일", 22, 0, 35),

    /** Every app row on the board gets one more notification: many rows jump to 3/4 or burst on the next touch. */
    GroupText("단체 문자", 27, 0, 30),

    /** Every app in the deck gains one upgrade level. */
    Firmware("펌웨어 업데이트", 37, 0, 60),

    /** Pays the previous swipe × 0 to 60 (half the time nothing): a gamble. */
    Lotto("행운 알림", 61, 0, 45),

    /** Arrives once a day on the first visit; swiping it pays ten minutes of recent income. */
    Birthday("생일 축하", 5, 0, 0),

    /** A 30-second drill: every tap on the panel raises all income by 1%, up to ×2, while it runs. */
    Drill("알림훈련", 15, 0, 60),

    /** Not a row: switched on up to 3 times a day for 20 s without interruptions and with auto twice as fast. */
    Dnd("방해 금지", 13, 0, 0),
}

/**
 * Board assists bought with P inside a run. [caps] raises the number of steps by level: later levels open
 * higher caps as content. Backup items raise their starting values.
 */
enum class Assist(val label: String, val unlockLevel: Int, val caps: List<Pair<Int, Int>>, val base: Long, val growth: Long) {
    Overload("과부하 깊이", 8, listOf(8 to 6, 19 to 7, 54 to 8, 83 to 9), 2_000, 400),
    Sandwich("샌드위치", 10, listOf(10 to 5, 24 to 6, 47 to 8), 20_000, 15),
    Board("넓은 알림창", 12, listOf(12 to 2, 39 to 3), 500_000, 200),
    Wild("와일드 알림", 20, listOf(20 to 5, 69 to 6, 70 to 7, 71 to 8), 1_000_000, 30),
    ;

    fun max(level: Int): Int = caps.lastOrNull { level >= it.first }?.second ?: 0
}

/** Backup shop. Bought with backup files from factory resets; never reset. */
/**
 * Backup shop. Bought with backup files from factory resets; never reset. [minResets] spreads the shop over
 * the first resets so every reset opens something new.
 */
enum class BackupItem(val label: String, val max: Int, val base: Long, val growthPermille: Int, val minResets: Int) {
    Value("알림 가치 ×1.25", 50, 3, 1_450, 1),
    FastBoot("빠른 부팅 · 레벨 기준 P −10%", 20, 2, 1_500, 1),
    StartFunds("시작 자금", 10, 1, 1_600, 1),
    Offline("오프라인 정산 +2%p · +1시간", 10, 4, 1_500, 1),
    Sandwich("샌드위치 +2%p", 10, 3, 1_500, 2),
    ChainBoost("연쇄 증폭 +0.1", 10, 8, 1_800, 2),
    AutoClean("자동 정리 −0.3초", 10, 4, 1_500, 3),
    Synergy("시너지 +10%", 10, 6, 1_600, 3),
    Milestone("마일스톤 +0.1", 10, 10, 1_900, 4),
    RememberedApps("기억된 앱", 8, 5, 2_000, 4),
    Battery("배터리 +2 · 충전 −0.2초", 5, 5, 2_000, 5),
    DeepOverload("시작 과부하 깊이 +1", 3, 20, 3_000, 6),
    ExtraSlot("앱 슬롯 +1", 2, 15, 3_000, 8),
    Board("넓은 알림창 +1", 2, 12, 3_000, 10),
    Wild("와일드 알림 +1%p", 10, 4, 1_500, 12),
    EventSlot("이벤트 슬롯 +1", 1, 25, 1_000, 15),
}

/**
 * Cloud shop (2nd prestige, 클라우드 동기화). Bought with GB from syncs; survives everything. [minSyncs] spreads it
 * over the first syncs so every sync opens something new.
 */
enum class CloudItem(val label: String, val max: Int, val base: Long, val growthPermille: Int, val minSyncs: Int, val pack: ContentPack? = null) {
    Compression("압축 백업 · 백업파일 ×1.5", 30, 1, 2_300, 1),
    SyncValue("동기화 가치 · 알림 가치 ×2", 30, 2, 2_600, 1),
    /** Raises the 알림 가치 backup item's max by 10: each sync moves the first layer's ceiling up. */
    ValueCap("알림 가치 한도 +10", 20, 2, 1_600, 1),
    Discount("백업 상점 −10%", 5, 3, 2_000, 2),
    KeepShop("백업 상점 단계 유지 +3", 5, 2, 2_500, 2),
    CloudSlot("앱 슬롯 +1", 1, 10, 1_000, 3),
    CloudOffline("오프라인 상한 +1시간", 4, 3, 2_000, 4),
    CloudEventSlot("이벤트 슬롯 +1", 1, 20, 1_000, 5),
    CloudAuto("자동 정리 −0.5초", 3, 8, 2_000, 6),
    CloudChain("연쇄 증폭 +0.2", 5, 10, 2_000, 8),
    /** Ships with update 1: a fresh growth axis for veterans (value ×3 per level), one level per 3 factory resets after the update. */
    AiBoost("AI 가속 · 알림 가치 ×3", 10, 5, 2_400, 0, ContentPack.AiTier),
}

/** A post-launch content pack. */
enum class ContentPack(val label: String) { AiTier("업데이트 1 · 11티어 AI") }

object Catalog {
    private fun app(id: String, name: String, tier: Int, order: Int, r: Region, c: Category, lv: Int, resets: Int = 0) = AppDef(id, name, tier, order, r, c, lv, resets)
    private val ST = Region.Stock
    private val AS = Region.Asia
    private val SE = Region.SoutheastAsia
    private val WE = Region.West
    private val GL = Region.Global

    private val launchApps = listOf(
        // 1티어
        app("alarm", "시계", 1, 0, ST, Category.Work, 1), // 기본 시계
        app("weather", "날씨", 1, 1, ST, Category.Daily, 1), // 기본 날씨
        app("memo", "메모", 1, 2, ST, Category.Work, 1), // 기본 메모
        app("flash", "손전등", 1, 3, ST, Category.Daily, 2), // 기본 손전등
        app("calc", "계산기", 1, 4, ST, Category.Money, 3), // 기본 계산기
        app("recorder", "음성 녹음", 1, 5, ST, Category.Daily, 4), // 기본 녹음기
        // 2티어
        app("phone", "전화", 2, 0, ST, Category.Social, 5), // 기본 전화
        app("sms", "메시지", 2, 1, ST, Category.Social, 6), // 기본 메시지
        app("camera", "카메라", 2, 2, ST, Category.Media, 7), // 기본 카메라
        app("gallery", "갤러리", 2, 3, ST, Category.Media, 8), // 기본 갤러리
        app("contacts", "연락처", 2, 4, ST, Category.Social, 9), // 기본 연락처
        app("settings", "설정", 2, 5, ST, Category.Daily, 11), // 기본 설정
        // 3티어
        app("music", "메롱", 3, 0, AS, Category.Media, 12), // Melon 패러디
        app("papago", "마마고", 3, 1, AS, Category.Work, 13), // Papago 패러디
        app("zalo", "잘로로", 3, 2, SE, Category.Social, 14), // Zalo 패러디
        app("momo", "모모모", 3, 3, SE, Category.Money, 16), // MoMo 패러디
        app("airbnb", "에어방앤방", 3, 4, WE, Category.Daily, 17), // Airbnb 패러디
        app("duo", "듀오링구", 3, 5, WE, Category.Work, 19), // Duolingo 패러디
        // 4티어
        app("ctaxi", "코코아T", 4, 0, AS, Category.Daily, 20), // Kakao T 패러디
        app("pay", "토스트", 4, 1, AS, Category.Money, 22), // Toss 패러디
        app("glints", "글린츠츠", 4, 2, SE, Category.Work, 23), // Glints 패러디
        app("bigo", "비고고", 4, 3, SE, Category.Media, 26), // Bigo Live 패러디
        app("pin", "핀트레스트", 4, 4, WE, Category.Media, 28), // Pinterest 패러디
        app("meet", "주움", 4, 5, WE, Category.Work, 29), // Zoom 패러디
        // 5티어
        app("food", "배다른민족", 5, 0, AS, Category.Daily, 25), // 배달의민족 패러디
        app("edit", "컵컷", 5, 1, AS, Category.Media, 33), // CapCut 패러디
        app("travel", "트래블로까", 5, 2, SE, Category.Daily, 35), // Traveloka 패러디
        app("joox", "쥬욱스", 5, 3, SE, Category.Media, 37), // JOOX 패러디
        app("reddit", "래딧", 5, 4, WE, Category.Social, 39), // Reddit 패러디
        app("career", "링크드아웃", 5, 5, WE, Category.Work, 41), // LinkedIn 패러디
        // 6티어
        app("mall", "쿠빵", 6, 0, AS, Category.Money, 72), // 쿠팡 패러디
        app("portal", "메이버", 6, 1, AS, Category.Work, 73), // Naver 패러디
        app("michat", "미챗챗", 6, 2, SE, Category.Social, 76), // MiChat 패러디
        app("agoda", "아고다다", 6, 3, SE, Category.Daily, 78), // Agoda 패러디
        app("paypal", "페이팔팔", 6, 4, WE, Category.Money, 80), // PayPal 패러디
        app("match", "튄더", 6, 5, WE, Category.Social, 82), // Tinder 패러디
        // 7티어
        app("superapp", "위잿", 7, 0, AS, Category.Money, 73), // WeChat 패러디
        app("line", "라잉", 7, 1, AS, Category.Social, 74), // LINE 패러디
        app("jobs", "잡스트리트", 7, 2, SE, Category.Work, 75), // JobStreet 패러디
        app("lazada", "라자다다", 7, 3, SE, Category.Money, 76), // Lazada 패러디
        app("ride", "우바", 7, 4, WE, Category.Daily, 77), // Uber 패러디
        app("robin", "로빈훗훗", 7, 5, WE, Category.Money, 78), // Robinhood 패러디
        // 8티어
        app("dance", "틱특", 8, 0, AS, Category.Media, 74), // TikTok 패러디
        app("chat", "코코아톡", 8, 1, AS, Category.Social, 75), // 카카오톡 패러디
        app("grab", "그래브", 8, 2, SE, Category.Daily, 76), // Grab 패러디
        app("shopee", "쇼피피", 8, 3, SE, Category.Money, 77), // Shopee 패러디
        app("beat", "스팟티파이", 8, 4, WE, Category.Media, 78), // Spotify 패러디
        app("why", "와이", 8, 5, WE, Category.Social, 79), // X 패러디
        // 9티어
        app("mega", "아마좀", 9, 0, GL, Category.Money, 79), // Amazon 패러디
        app("ott", "넷플러그", 9, 1, GL, Category.Media, 89), // Netflix 패러디
        app("secret", "텔레그림", 9, 2, GL, Category.Money, 92), // Telegram 패러디
        app("mail", "쥐메일", 9, 3, GL, Category.Work, 95), // Gmail 패러디
        app("maps", "큐글맵스", 9, 4, GL, Category.Daily, 98), // Google Maps 패러디
        app("store", "큐글플레이", 9, 5, GL, Category.Money, 101), // Google Play 패러디
        // 10티어
        app("tube", "너튜브", 10, 0, GL, Category.Media, 97), // YouTube 패러디
        app("photo", "인싸그램", 10, 1, GL, Category.Media, 108), // Instagram 패러디
        app("feed", "테이크북", 10, 2, GL, Category.Social, 112), // Facebook 패러디
        app("green", "왔썹", 10, 3, GL, Category.Social, 116), // WhatsApp 패러디
        app("search", "큐글", 10, 4, GL, Category.Work, 120), // Google 패러디 · 영문명 Qoogle
        app("ai", "챗GTP", 10, 5, GL, Category.Work, 124), // ChatGPT 패러디
    )
    val starter = listOf("alarm", "memo", "weather")

    /** Previous gates that were raised in rules v5. Keep the app unlocks the player had earned. */
    internal val legacyAppUnlockLevels = mapOf(
        "mall" to 43, "portal" to 45, "michat" to 47, "agoda" to 49, "paypal" to 51, "match" to 53,
        "superapp" to 55, "line" to 57, "jobs" to 59, "lazada" to 61, "ride" to 63, "robin" to 65,
        "dance" to 67, "chat" to 69, "grab" to 71, "shopee" to 73, "beat" to 75, "why" to 77,
    )

    /** Level at which each app's pro version (×2 value) opens: 25 levels after the app itself. */
    private val launchPro: Map<String, Int> = mapOf(
        "alarm" to 26, "weather" to 26, "memo" to 26, "flash" to 27, "calc" to 28, "recorder" to 29,
        "phone" to 30, "sms" to 31, "camera" to 32, "gallery" to 33, "contacts" to 34, "settings" to 36,
        "music" to 37, "papago" to 38, "zalo" to 39, "momo" to 41, "airbnb" to 42, "duo" to 44,
        "ctaxi" to 45, "pay" to 47, "glints" to 48, "bigo" to 51, "pin" to 53, "meet" to 54,
        "food" to 50, "edit" to 58, "travel" to 60, "joox" to 62, "reddit" to 64, "career" to 66,
        "mall" to 97, "portal" to 98, "michat" to 101, "agoda" to 103, "paypal" to 105, "match" to 107,
        "superapp" to 98, "line" to 99, "jobs" to 100, "lazada" to 101, "ride" to 102, "robin" to 103,
        "dance" to 99, "chat" to 100, "grab" to 101, "shopee" to 102, "beat" to 103, "why" to 104,
        "mega" to 104, "ott" to 114, "secret" to 117, "mail" to 120, "maps" to 123, "store" to 126,
        "tube" to 122, "photo" to 133, "feed" to 137, "green" to 141, "search" to 145, "ai" to 149,
    )
    // Live updates ---------------------------------------------------------------------------

    /**
     * Content packs added after launch (docs/bible/14-live-updates.md). New tiers go on top, so early
     * curves stay untouched. The game sets [packs] from its content version; the simulator switches
     * packs on mid-run to test an update against veteran saves.
     */
    private val packApps: Map<ContentPack, List<AppDef>> = mapOf(
        ContentPack.AiTier to listOf(
            app("gemini", "재미나이", 11, 0, GL, Category.Work, 126), // Gemini 패러디
            app("claude", "클로디", 11, 1, GL, Category.Social, 129), // Claude 패러디
            app("copilot", "코파일롯", 11, 2, GL, Category.Work, 132), // Copilot 패러디
            app("perplexity", "퍼플렉씨", 11, 3, GL, Category.Daily, 135), // Perplexity 패러디
            app("deepseek", "딥씨크", 11, 4, GL, Category.Money, 138), // DeepSeek 패러디
            app("midjourney", "미드저니어", 11, 5, GL, Category.Media, 141), // Midjourney 패러디
        ),
    )

    var packs: Set<ContentPack> = emptySet()
        set(value) { field = value; rebuild() }

    var apps: List<AppDef> = launchApps
        private set
    var proLevel: Map<String, Int> = launchPro
        private set
    private var byId: Map<String, AppDef> = launchApps.associateBy { it.id }

    private fun rebuild() {
        val extra = packs.sortedBy { it.ordinal }.flatMap { packApps.getValue(it) }
        apps = launchApps + extra
        proLevel = launchPro + extra.associate { it.id to it.unlockLevel + 25 }
        byId = apps.associateBy { it.id }
    }

    fun hasApp(id: String): Boolean = id in byId

    fun app(id: String): AppDef = byId[id] ?: error("unknown app $id")
}
