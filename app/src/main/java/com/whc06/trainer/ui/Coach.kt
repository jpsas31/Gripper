package com.whc06.trainer.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class Coach(ctx: Context) {

    private val tag = "Coach"
    private val appCtx = ctx.applicationContext
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appCtx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false
    private val pendingQueue = mutableListOf<Pair<String, Boolean>>()
    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 90)
    } catch (e: RuntimeException) {
        Log.w(tag, "ToneGenerator unavailable: ${e.message}")
        null
    }
    var enabled: Boolean = true
    var hapticEnabled: Boolean = true

    init {
        Log.d(tag, "init — creating TTS")
        tts = TextToSpeech(appCtx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts
                val locale = Locale.getDefault()
                val avail = engine?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                val chosen = when {
                    avail >= TextToSpeech.LANG_AVAILABLE -> locale
                    engine?.isLanguageAvailable(Locale.US) ?: -1 >= TextToSpeech.LANG_AVAILABLE -> Locale.US
                    else -> null
                }
                if (chosen != null) {
                    engine?.language = chosen
                    engine?.setSpeechRate(1.05f)
                    ttsReady = true
                    Log.d(tag, "TTS ready, locale=$chosen, default=${tts?.defaultEngine}")
                    drainPending()
                } else {
                    Log.w(tag, "TTS no supported language; will be silent")
                }
            } else {
                Log.w(tag, "TTS init FAILED status=$status")
            }
        }
    }

    private fun drainPending() {
        synchronized(pendingQueue) {
            pendingQueue.forEach { (t, urgent) -> speakNow(t, urgent) }
            pendingQueue.clear()
        }
    }

    fun say(text: String, urgent: Boolean = false) {
        if (!enabled) return
        if (!ttsReady) {
            synchronized(pendingQueue) {
                if (pendingQueue.size < 8) pendingQueue.add(text to urgent)
            }
            return
        }
        speakNow(text, urgent)
    }

    private fun speakNow(text: String, urgent: Boolean) {
        val res = tts?.speak(
            text,
            if (urgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
            null,
            text.hashCode().toString()
        )
        if (res != TextToSpeech.SUCCESS) Log.w(tag, "speak failed res=$res text=$text")
    }

    fun pulse(strong: Boolean = false) {
        if (!hapticEnabled) return
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (strong) {
                VibrationEffect.createOneShot(150L, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(if (strong) 150L else 50L)
        }
    }

    fun startWork(label: String) {
        pulse(strong = true)
        beep(ToneGenerator.TONE_CDMA_HIGH_PBX_L, 350)
        say(label, urgent = true)
    }

    fun startRest() {
        pulse(strong = false)
        beep(ToneGenerator.TONE_CDMA_LOW_PBX_L, 250)
        say("Rest", urgent = true)
    }

    fun countdown(num: Int) {
        pulse(strong = false)
        beep(ToneGenerator.TONE_PROP_BEEP, 120)
        say(num.toString(), urgent = false)
    }

    fun phaseEnd() {
        pulse(strong = true)
        beep(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
    }

    private fun beep(toneType: Int, durationMs: Int) {
        if (!enabled) return
        tone?.startTone(toneType, durationMs)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        tone?.release()
    }
}
