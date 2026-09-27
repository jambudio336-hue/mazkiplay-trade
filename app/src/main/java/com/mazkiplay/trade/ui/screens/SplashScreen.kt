package com.mazkiplay.trade.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazkiplay.trade.R
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.util.AppConstants
import com.mazkiplay.trade.util.Greetings
import kotlinx.coroutines.delay

/**
 * First screen the user sees.
 *
 * The hero artwork is produced during the build from a vector source
 * (tools/generate-icons.sh), so the repository holds no bitmaps while every device
 * still receives an illustration at the right resolution. The greeting is drawn at
 * random on every launch, which is the "sapaan berganti" behaviour in the brief.
 */
@Composable
fun SplashScreen(
    prefs: UserPreferences,
    onFinished: () -> Unit,
    holdMillis: Long = 2600L
) {
    // Drawn once per launch and kept stable across recompositions.
    val greeting = remember { Greetings.random(prefs.language) }
    var visible by remember { mutableStateOf(false) }
    val fade by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(900),
        label = "splashFade"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(holdMillis)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Generated banner artwork. ContentScale.Crop keeps it edge-to-edge on any
        // aspect ratio; the gradient below guarantees contrast for the text even
        // before the bitmap has decoded.
        Image(
            painter = painterResource(R.drawable.splash_hero),
            contentDescription = "Nusantara Forex",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.85f)
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Image(
                painter = painterResource(R.drawable.logo_nusantara),
                contentDescription = "Logo Nusantara Forex",
                modifier = Modifier.size(112.dp).clip(RoundedCornerShape(26.dp))
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = AppConstants.BRAND,
                color = Gold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = AppConstants.APP_NAME,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 11.dp)
                    .alpha(fade)
            ) {
                Text(
                    text = greeting,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = AppConstants.SIGNATURE,
                color = Bull,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(26.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = Bull,
                strokeWidth = 2.5.dp
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}
