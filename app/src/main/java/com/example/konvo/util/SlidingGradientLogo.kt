import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.konvo.R
import com.example.konvo.ui.util.AnimatedGradient

@Composable
fun KonvoLogo(
    modifier: Modifier = Modifier,
    cycleMs: Int = 10000              // 10‑second loop; tweak as you like
) {
    val brush = AnimatedGradient()

    Image(
        painter = painterResource(R.drawable.logo_konvo_white),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(260.dp)             // size it however you wish
            .aspectRatio(4f)           // ← keep original aspect
            .drawWithCache {
                // 1️⃣ cache the current brush so we don’t re‑allocate each frame
                onDrawWithContent {
                    // 2️⃣ draw the PNG first  ➜ becomes the **destination**
                    drawContent()
                    // 3️⃣ paint the gradient on top with SrcIn
                    //     → keeps ONLY the pixels where the destination had alpha
                    drawRect(brush = brush, blendMode = BlendMode.SrcIn)
                }
            }
    )
}
