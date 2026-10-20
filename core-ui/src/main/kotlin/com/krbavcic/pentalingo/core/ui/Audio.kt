package com.krbavcic.pentalingo.core.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import org.json.JSONObject

/**
 * Plays the course's recorded pronunciations.
 *
 * The clips were generated ahead of time with ElevenLabs and ship inside the app as MP3s, listed
 * in `assets/audio/manifest.json` by item id. Nothing is synthesised on the phone: no API key in
 * the APK, no network, and a clip sounds the same every time it is played, which matters when the
 * point is to learn to imitate it.
 */
class Voice private constructor(private val context: Context) {

    private val manifest: Map<String, String> by lazy {
        try {
            val text = context.assets.open("audio/manifest.json").bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            json.keys().asSequence().associateWith { json.getString(it) }
        } catch (e: java.io.IOException) {
            emptyMap()
        }
    }

    private var player: MediaPlayer? = null

    fun has(id: String?): Boolean = id != null && id in manifest

    /** The clip file an item plays, for tester reports. */
    fun fileFor(id: String?): String? = id?.let { manifest[it] }

    fun play(id: String?, slow: Boolean = false) {
        val file = id?.let { manifest[it] } ?: return
        stop()
        try {
            val fd = context.assets.openFd("audio/$file")
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
                setOnPreparedListener { mp ->
                    // Setting a speed starts playback on some versions and not others.
                    if (slow) mp.playbackParams = mp.playbackParams.setSpeed(0.7f)
                    if (!mp.isPlaying) mp.start()
                }
                setOnCompletionListener { mp -> if (player === mp) stop() }
                prepareAsync()
            }
        } catch (e: java.io.IOException) {
            stop()
        }
    }

    fun stop() {
        player?.release()
        player = null
    }

    companion object {
        @Volatile
        private var instance: Voice? = null

        fun of(context: Context): Voice = instance ?: synchronized(this) {
            instance ?: Voice(context.applicationContext).also { instance = it }
        }
    }
}

@Composable
fun rememberVoice(): Voice {
    val context = LocalContext.current
    return remember(context) { Voice.of(context) }
}

/** The app's few sound effects: plucked harp strings, quiet enough for a train carriage. */
interface Sfx {
    fun tap()
    fun correct()
    fun near()
    fun wrong()
    fun complete()
    fun award()
}

private object SilentSfx : Sfx {
    override fun tap() = Unit
    override fun correct() = Unit
    override fun near() = Unit
    override fun wrong() = Unit
    override fun complete() = Unit
    override fun award() = Unit
}

private class PooledSfx(context: Context) : Sfx {
    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ready = mutableSetOf<Int>()
    private val tapId = pool.load(context, R.raw.sfx_tap, 1)
    private val correctId = pool.load(context, R.raw.sfx_correct, 1)
    private val nearId = pool.load(context, R.raw.sfx_near, 1)
    private val wrongId = pool.load(context, R.raw.sfx_wrong, 1)
    private val completeId = pool.load(context, R.raw.sfx_complete, 1)
    private val awardId = pool.load(context, R.raw.sfx_award, 1)

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready += id }
    }

    private fun play(id: Int, volume: Float) {
        if (id in ready) pool.play(id, volume, volume, 1, 0, 1f)
    }

    override fun tap() = play(tapId, 0.3f)
    override fun correct() = play(correctId, 0.6f)
    override fun near() = play(nearId, 0.55f)
    override fun wrong() = play(wrongId, 0.5f)
    override fun complete() = play(completeId, 0.7f)
    override fun award() = play(awardId, 0.75f)

    fun release() = pool.release()
}

@Composable
fun rememberSfx(enabled: Boolean): Sfx {
    val context = LocalContext.current
    val pool = remember(context) { PooledSfx(context) }
    DisposableEffect(pool) { onDispose { pool.release() } }
    return if (enabled) pool else SilentSfx
}

/** Three things touch has to say: a tap landed, an answer was right, an answer was not. */
class Haptics(private val view: View) {
    fun tap() = play(HapticFeedbackConstants.CLOCK_TICK)

    fun correct() = play(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
    )

    fun wrong() = play(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
    )

    private fun play(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
