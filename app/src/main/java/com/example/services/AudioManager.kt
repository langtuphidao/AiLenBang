package com.example.services

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.models.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class SoundEffectType {
    START_GAME,
    SUSPENSE_TICK,
    COMPLETE_FANFARE,
    REVEAL_WINNER,
    ACTION_SUCCESS,
    WARNING_ALERT
}

/**
 * Bộ quản lý âm thanh và hiệu ứng (AudioManager).
 *
 * Sử dụng bộ tổng hợp âm thanh PCM 16-bit nội bộ (Synthesized Chimes) 100% không bản quyền,
 * hoạt động hoàn toàn offline, không bao giờ lỗi thiếu file âm thanh và có cơ chế chống chồng âm
 * khi người dùng chạm liên tục.
 */
class AudioManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastPlayedTimestamp = 0L
    private var currentSettings = AppSettings()

    fun updateSettings(settings: AppSettings) {
        currentSettings = settings
    }

    fun play(effect: SoundEffectType) {
        val settings = currentSettings
        if (settings.quietTeachingMode || !settings.sfxEnabled || settings.volume <= 0.01f) {
            return
        }

        val now = System.currentTimeMillis()
        val minCooldownMs = when (effect) {
            SoundEffectType.SUSPENSE_TICK -> 65L
            else -> 160L
        }
        if (now - lastPlayedTimestamp < minCooldownMs) {
            return
        }
        lastPlayedTimestamp = now

        val volume = settings.volume.coerceIn(0f, 1f)
        scope.launch {
            runCatching {
                when (effect) {
                    SoundEffectType.START_GAME -> playSequence(
                        notesHz = listOf(523.25, 659.25, 783.99),
                        durationsMs = listOf(80, 80, 140),
                        volume = volume
                    )
                    SoundEffectType.SUSPENSE_TICK -> playSequence(
                        notesHz = listOf(880.0),
                        durationsMs = listOf(35),
                        volume = volume * 0.6f
                    )
                    SoundEffectType.COMPLETE_FANFARE -> playSequence(
                        notesHz = listOf(523.25, 659.25, 783.99, 1046.50),
                        durationsMs = listOf(90, 90, 90, 240),
                        volume = volume
                    )
                    SoundEffectType.REVEAL_WINNER -> playSequence(
                        notesHz = listOf(587.33, 783.99, 987.77, 1174.66),
                        durationsMs = listOf(80, 80, 100, 260),
                        volume = volume
                    )
                    SoundEffectType.ACTION_SUCCESS -> playSequence(
                        notesHz = listOf(659.25, 880.0),
                        durationsMs = listOf(70, 110),
                        volume = volume * 0.75f
                    )
                    SoundEffectType.WARNING_ALERT -> playSequence(
                        notesHz = listOf(330.0, 261.63),
                        durationsMs = listOf(110, 160),
                        volume = volume * 0.8f
                    )
                }
            }
        }
    }

    private fun playSequence(notesHz: List<Double>, durationsMs: List<Int>, volume: Float) {
        val sampleRate = 22050
        val totalMs = durationsMs.sum().coerceAtLeast(40)
        val totalSamples = (sampleRate * totalMs) / 1000
        val pcmBuffer = ShortArray(totalSamples)

        var offset = 0
        for (i in notesHz.indices) {
            val freq = notesHz[i]
            val durMs = durationsMs.getOrElse(i) { 80 }
            val count = ((sampleRate * durMs) / 1000).coerceAtMost(totalSamples - offset)
            for (s in 0 until count) {
                val env = when {
                    s < count * 0.1 -> s / (count * 0.1)
                    s > count * 0.7 -> (count - s) / (count * 0.3)
                    else -> 1.0
                }
                val angle = 2.0 * PI * s * freq / sampleRate
                val sampleVal = (sin(angle) * env * volume * Short.MAX_VALUE * 0.35).toInt()
                pcmBuffer[offset + s] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            offset += count
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcmBuffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcmBuffer, 0, pcmBuffer.size)
        track.play()
        Thread.sleep(totalMs.toLong() + 30L)
        track.stop()
        track.release()
    }
}
