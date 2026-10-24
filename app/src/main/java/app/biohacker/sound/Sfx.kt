package app.biohacker.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The lab's sounds, synthesised once into small WAV files: a relay click, the blip of a mod done, the
 * thud of a stamp, a parcel's tape torn off, the chime of an install and the soft sweep of readings saved.
 */
class Sfx(private val context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(6)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private var tap = 0
    private var blip = 0
    private var stamp = 0
    private var tear = 0
    private var chime = 0
    private var sweep = 0
    private var alarm = 0
    @Volatile var on = true

    fun load() {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        fun file(name: String, pcm: () -> ShortArray) = File(dir, "$name.wav").also { if (!it.exists()) Wav.write(it, pcm()) }.absolutePath
        tap = pool.load(file("tap1") { Synth.click() }, 1)
        blip = pool.load(file("blip1") { Synth.blip() }, 1)
        stamp = pool.load(file("stamp1") { Synth.stamp() }, 1)
        tear = pool.load(file("tear1") { Synth.tear() }, 1)
        chime = pool.load(file("chime1") { Synth.chime() }, 1)
        sweep = pool.load(file("sweep1") { Synth.sweep() }, 1)
        alarm = pool.load(file("alarm1") { Synth.alarm() }, 1)
    }

    private fun play(id: Int, vol: Float = .9f) { if (on && id != 0) pool.play(id, vol, vol, 1, 0, 1f) }

    fun tap() = play(tap, .5f)
    fun blip() = play(blip, .6f)
    fun stamp() = play(stamp)
    fun tear() = play(tear, .7f)
    fun chime() = play(chime, .7f)
    fun sweep() = play(sweep, .6f)
    fun alarm() = play(alarm, .5f)
}

object Synth {
    const val RATE = 22050

    private fun pcm(seconds: Double, f: (Int, Double) -> Double): ShortArray {
        val n = (RATE * seconds).toInt()
        return ShortArray(n) { i -> (f(i, i / RATE.toDouble()) * 30000).toInt().coerceIn(-32767, 32767).toShort() }
    }

    private class Band(f: Double, q: Double) {
        private val w = 2 * PI * f / RATE
        private val a = sin(w) / (2 * q)
        private val b0 = a / (1 + a); private val a1 = -2 * kotlin.math.cos(w) / (1 + a); private val a2 = (1 - a) / (1 + a)
        private var x1 = 0.0; private var x2 = 0.0; private var y1 = 0.0; private var y2 = 0.0
        fun next(x: Double): Double { val y = b0 * x - b0 * x2 - a1 * y1 - a2 * y2; x2 = x1; x1 = x; y2 = y1; y1 = y; return y }
    }

    /** A relay closing. */
    fun click(): ShortArray { val r = Random(1); val b = Band(3200.0, 3.0); return pcm(.04) { _, t -> b.next(r.nextDouble() * 2 - 1) * exp(-t * 180) * 2.5 } }

    /** Two quick rising tones: done. */
    fun blip(): ShortArray = pcm(.16) { _, t -> val f = if (t < .07) 1320.0 else 1760.0; sin(2 * PI * f * t) * .3 * exp(-(t % .07) * 30) }

    /** A rubber stamp on paper: a low thump and a bit of slap. */
    fun stamp(): ShortArray {
        val r = Random(4); val lo = Band(90.0, 1.0); val mid = Band(700.0, 1.5)
        return pcm(.35) { _, t -> val n = r.nextDouble() * 2 - 1; lo.next(n) * 6 * exp(-t * 22) + mid.next(n) * 1.4 * exp(-t * 60) + sin(2 * PI * 70 * t) * .5 * exp(-t * 18) }
    }

    /** Tape torn off a parcel: crackle getting denser. */
    fun tear(): ShortArray {
        val r = Random(6)
        var last = 0.0
        return pcm(.45) { _, t ->
            val crack = if (r.nextDouble() < .08 + t * .3) r.nextDouble() * 2 - 1 else 0.0
            last = last * .6 + crack * .4
            last * 1.6 * (if (t > .38) (.45 - t) / .07 else 1.0)
        }
    }

    /** An install: a little ascending arpeggio, like an old terminal's success tone. */
    fun chime(): ShortArray {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
        return pcm(.9) { _, t ->
            var s = 0.0
            for ((i, f) in notes.withIndex()) { val at = i * .09; if (t >= at) { val k = t - at; s += (sin(2 * PI * f * k) + .3 * sin(4 * PI * f * k)) * exp(-k * 4.5) * .16 } }
            s
        }
    }

    /** A scanner sweep: readings saved. */
    fun sweep(): ShortArray {
        var ph = 0.0
        return pcm(.5) { _, t -> ph += (400 + 1400 * t / .5) / RATE; sin(2 * PI * ph) * .18 * sin(PI * t / .5) }
    }

    /** Two low beeps: a glitch noticed. */
    fun alarm(): ShortArray = pcm(.5) { _, t -> val on = t < .14 || (t > .24 && t < .38); if (on) (if (sin(2 * PI * 440 * t) > 0) .12 else -.12) else 0.0 }
}

object Wav {
    fun write(f: File, pcm: ShortArray) {
        val data = ByteArrayOutputStream()
        for (s in pcm) { data.write(s.toInt() and 0xFF); data.write((s.toInt() shr 8) and 0xFF) }
        val bytes = data.toByteArray()
        DataOutputStream(f.outputStream().buffered()).use { o ->
            fun le32(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF); o.write((v shr 16) and 0xFF); o.write((v shr 24) and 0xFF) }
            fun le16(v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF) }
            o.writeBytes("RIFF"); le32(36 + bytes.size); o.writeBytes("WAVE")
            o.writeBytes("fmt "); le32(16); le16(1); le16(1); le32(Synth.RATE); le32(Synth.RATE * 2); le16(2); le16(16)
            o.writeBytes("data"); le32(bytes.size); o.write(bytes)
        }
    }
}
