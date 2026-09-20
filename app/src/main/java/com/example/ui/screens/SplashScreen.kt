package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "logo_alpha"
    )

    var progress by remember { mutableFloatStateOf(0.1f) }

    LaunchedEffect(Unit) {
        startAnimation = true
        // Animate progress smoothly
        for (i in 1..10) {
            delay(120)
            progress = (i * 0.1f).coerceAtMost(1f)
        }
        delay(200)
        onFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF090D16)
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .scale(scaleAnim.value)
        ) {
            // Kamick by Raza Logo Badge
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.kamick_logo),
                        contentDescription = "Kamick by Raza Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "KAMICK",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFFDC2626),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "BY RAZA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tachiyomi Extension Engine • Offline Comic & Manga Reader",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Loading Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFDC2626),
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Initializing comic sources & extensions...",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // Quick Skip in top corner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, end = 20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            TextButton(onClick = onFinish) {
                Text(
                    text = "Skip",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
