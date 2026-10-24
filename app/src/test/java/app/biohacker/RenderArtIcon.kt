package app.biohacker

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The launcher icon's two layers flattened into docs/logo.png (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArtIcon {
    @Test fun logo() {
        val c = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        for (id in intArrayOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            val d = ContextCompat.getDrawable(c, id)!!
            d.setBounds(0, 0, 512, 512); d.draw(canvas)
        }
        File("../docs/logo.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
