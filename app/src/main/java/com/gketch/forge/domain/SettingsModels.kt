package com.gketch.forge.domain

import org.json.JSONObject

enum class LibrarySort(val label: String) {
    NAME("Name"),
    DATE("Date"),
    SIZE("Size"),
    DURATION("Duration"),
}

enum class SubtitleColor(val label: String, val argb: Int) {
    WHITE("White", android.graphics.Color.WHITE),
    YELLOW("Yellow", android.graphics.Color.YELLOW),
    CYAN("Cyan", android.graphics.Color.CYAN),
    GREEN("Green", android.graphics.Color.GREEN),
    RED("Red", 0xFFFF5252.toInt()),
}

enum class SubtitleBackground(val label: String) {
    NONE("None"),
    SEMI("Semi"),
    BLACK("Black"),
}

enum class SubtitlePosition(val label: String, val bottomFraction: Float) {
    BOTTOM("Bottom", 0.08f),
    MID("Mid", 0.22f),
    HIGH("High", 0.40f),
}

enum class AccentPreset(val label: String, val color: Long, val soft: Long) {
    EMBER("Ember", 0xFFFF6B35, 0xFFFF8F66),
    AMBER("Amber", 0xFFFFB020, 0xFFFFC85A),
    LIME("Lime", 0xFFB8E63B, 0xFFD4F06A),
    TEAL("Teal", 0xFF2EC4B6, 0xFF7EE0D6),
    SKY("Sky", 0xFF4DA3FF, 0xFF8AC4FF),
    VIOLET("Violet", 0xFFB388FF, 0xFFD0B3FF),
    ROSE("Rose", 0xFFFF5D8F, 0xFFFF8FB0),
}

enum class SleepEndAction(val label: String) {
    PAUSE("Pause"),
    STOP("Stop"),
}

enum class GestureSensitivity(val label: String, val multiplier: Float) {
    LOW("Low", 0.55f),
    NORMAL("Normal", 1.0f),
    HIGH("High", 1.65f),
}

enum class AppLanguage(val label: String) {
    SYSTEM("System"),
    ENGLISH("English"),
    HINDI("हिन्दी"),
}

enum class MinClipLength(val label: String, val seconds: Int) {
    OFF("Off", 0),
    SEC_15("15s", 15),
    SEC_30("30s", 30),
    SEC_60("60s", 60),
}

enum class CrossfadeDuration(val label: String, val seconds: Int) {
    OFF("Off", 0),
    SEC_1("1s", 1),
    SEC_2("2s", 2),
    SEC_3("3s", 3),
}


enum class ResumeBehavior(val label: String) {
    ASK("Ask every time"),
    ALWAYS_CONTINUE("Always continue"),
    ALWAYS_START_OVER("Always start over"),
}


enum class SkipIntroSeconds(val label: String, val seconds: Int) {
    OFF("Off", 0),
    SEC_30("30s", 30),
    SEC_60("60s", 60),
    SEC_90("90s", 90),
}

enum class HoldToSpeed(val label: String, val speed: Float) {
    X1_5("1.5×", 1.5f),
    X2("2×", 2.0f),
    X2_5("2.5×", 2.5f),
    X3("3×", 3.0f),
}

enum class SubtitleOutline(val label: String) {
    OUTLINE("Outline"),
    STRONG("Strong"),
    SHADOW("Shadow"),
}


enum class AudioFocusBehavior(val label: String) {
    PAUSE("Pause others"),
    DUCK("Duck others"),
}

enum class ChromeHideDelay(val label: String, val delayMs: Long?) {
    SEC_3("3s", 3_000L),
    SEC_5_5("5.5s", 5_500L),
    SEC_8("8s", 8_000L),
    NEVER("Never", null),
}


data class AppSettings(
    val seekSeconds: Int = 10,
    val autoplayNext: Boolean = true,
    val librarySort: LibrarySort = LibrarySort.NAME,
    val subtitleColor: SubtitleColor = SubtitleColor.WHITE,
    val subtitleBackground: SubtitleBackground = SubtitleBackground.SEMI,
    val subtitlePosition: SubtitlePosition = SubtitlePosition.BOTTOM,
    val subtitleSizeSp: Float = 20f,
    val accentPreset: AccentPreset = AccentPreset.EMBER,
    val dynamicColor: Boolean = false,
    val sleepFadeEnabled: Boolean = false,
    val sleepFadeSeconds: Int = 10,
    val sleepEndAction: SleepEndAction = SleepEndAction.PAUSE,
    val defaultPlaybackSpeed: Float = 1.0f,
    val lastLibraryTab: String = "VIDEO",
    val gestureSensitivity: GestureSensitivity = GestureSensitivity.NORMAL,
    val minClipLength: MinClipLength = MinClipLength.OFF,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val streamUserAgent: String = "",
    val streamTimeoutSec: Int = 20,
    val gaplessPlayback: Boolean = true,
    val crossfade: CrossfadeDuration = CrossfadeDuration.OFF,
    val loudnessNormalize: Boolean = false,
    val chromeHideDelay: ChromeHideDelay = ChromeHideDelay.SEC_5_5,
    val resumeBehavior: ResumeBehavior = ResumeBehavior.ASK,
    val seriesAutoNext: Boolean = false,
    val skipIntroSeconds: SkipIntroSeconds = SkipIntroSeconds.OFF,
    val pauseOnHeadsetUnplug: Boolean = true,
    val showRemainingTime: Boolean = false,
    val holdToSpeed: HoldToSpeed = HoldToSpeed.X2,
    val playerGesturesEnabled: Boolean = true,
    val invertGestureSides: Boolean = false,
    val subtitleOutline: SubtitleOutline = SubtitleOutline.OUTLINE,
    val swipeDownToClose: Boolean = true,
    val doubleTapToLock: Boolean = false,
    val audioFocusBehavior: AudioFocusBehavior = AudioFocusBehavior.PAUSE,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("seekSeconds", seekSeconds)
        .put("autoplayNext", autoplayNext)
        .put("librarySort", librarySort.name)
        .put("subtitleColor", subtitleColor.name)
        .put("subtitleBackground", subtitleBackground.name)
        .put("subtitlePosition", subtitlePosition.name)
        .put("subtitleSizeSp", subtitleSizeSp.toDouble())
        .put("accentPreset", accentPreset.name)
        .put("dynamicColor", dynamicColor)
        .put("sleepFadeEnabled", sleepFadeEnabled)
        .put("sleepFadeSeconds", sleepFadeSeconds)
        .put("sleepEndAction", sleepEndAction.name)
        .put("defaultPlaybackSpeed", defaultPlaybackSpeed.toDouble())
        .put("lastLibraryTab", lastLibraryTab)
        .put("gestureSensitivity", gestureSensitivity.name)
        .put("minClipLength", minClipLength.name)
        .put("appLanguage", appLanguage.name)
        .put("streamUserAgent", streamUserAgent)
        .put("streamTimeoutSec", streamTimeoutSec)
        .put("gaplessPlayback", gaplessPlayback)
        .put("crossfade", crossfade.name)
        .put("loudnessNormalize", loudnessNormalize)
        .put("chromeHideDelay", chromeHideDelay.name)
        .put("resumeBehavior", resumeBehavior.name)
        .put("seriesAutoNext", seriesAutoNext)
        .put("skipIntroSeconds", skipIntroSeconds.name)
        .put("pauseOnHeadsetUnplug", pauseOnHeadsetUnplug)
        .put("showRemainingTime", showRemainingTime)
        .put("holdToSpeed", holdToSpeed.name)
        .put("playerGesturesEnabled", playerGesturesEnabled)
        .put("invertGestureSides", invertGestureSides)
        .put("subtitleOutline", subtitleOutline.name)
        .put("swipeDownToClose", swipeDownToClose)
        .put("doubleTapToLock", doubleTapToLock)
        .put("audioFocusBehavior", audioFocusBehavior.name)
}
