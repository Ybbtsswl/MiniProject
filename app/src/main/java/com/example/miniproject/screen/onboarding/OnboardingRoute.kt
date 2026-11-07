package com.example.miniproject.screen.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Cream = Color(0xFFFFFBF2)
private val Yellow = Color(0xFFFFD447)
private val PaleYellow = Color(0xFFFFF1C2)
private val Ink = Color(0xFF202020)
private val Muted = Color(0xFF777777)

@Composable
fun OnboardingRoute(onFinished: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }

    if (page == 0) {
        SplashScreen(
            onFinished = { page = 1 }
        )
    } else {
        OnboardingScreen(
            page = page,
            onNext = {
                if (page == 3) {
                    onFinished()
                } else {
                    page++
                }
            }
        )
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SplashWaveBackground(
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 74.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WalletIllustration()

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "monee",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your Smart\nFinance Companion",
                color = Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SplashWaveBackground(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(185.dp)
    ) {
        val firstWave = Path().apply {
            moveTo(0f, size.height * 0.22f)
            cubicTo(
                size.width * 0.20f,
                size.height * 0.02f,
                size.width * 0.36f,
                size.height * 0.42f,
                size.width * 0.58f,
                size.height * 0.38f
            )
            cubicTo(
                size.width * 0.78f,
                size.height * 0.34f,
                size.width * 0.88f,
                size.height * 0.18f,
                size.width,
                size.height * 0.28f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = firstWave,
            color = Color(0xFFFFF8DE)
        )

        val secondWave = Path().apply {
            moveTo(0f, size.height * 0.48f)
            cubicTo(
                size.width * 0.22f,
                size.height * 0.28f,
                size.width * 0.38f,
                size.height * 0.68f,
                size.width * 0.62f,
                size.height * 0.59f
            )
            cubicTo(
                size.width * 0.80f,
                size.height * 0.52f,
                size.width * 0.90f,
                size.height * 0.42f,
                size.width,
                size.height * 0.50f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = secondWave,
            color = PaleYellow
        )

        drawArc(
            color = Color(0xFFC9A728),
            startAngle = 35f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(size.width / 2f - 9.dp.toPx(), size.height - 38.dp.toPx()),
            size = Size(18.dp.toPx(), 18.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun OnboardingScreen(
    page: Int,
    onNext: () -> Unit
) {
    val content = when (page) {
        1 -> OnboardingContent(
            title = "Scan Receipts,\nTrack Expenses",
            description = "Simply take a photo and\nlet monee do the work."
        )

        2 -> OnboardingContent(
            title = "Manage Your Money\nSmarter",
            description = "Track spending, set budgets\nand reach your goals."
        )

        else -> OnboardingContent(
            title = "Split Bills with Ease",
            description = "Share expenses and\nsettle debts easily."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (page) {
                1 -> ReceiptIllustration()
                2 -> MoneyIllustration()
                else -> SplitBillIllustration()
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = content.title,
                color = Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = content.description,
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        PageIndicator(currentPage = page)

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Yellow,
                contentColor = Ink
            )
        ) {
            Text(
                text = if (page == 3) "Get Started" else "Next",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class OnboardingContent(
    val title: String,
    val description: String
)

@Composable
private fun PageIndicator(currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (page in 1..3) {
            Box(
                modifier = Modifier
                    .size(if (page == currentPage) 7.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (page == currentPage) Yellow else Color(0xFFE3E3E3))
            )
        }
    }
}

@Composable
private fun WalletIllustration() {
    Box(
        modifier = Modifier.size(86.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Yellow)
        )
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 14.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFFC72C))
        )
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 18.dp)
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFFFFB900))
        ) {
            Text(
                text = "•",
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ReceiptIllustration() {
    Row(
        modifier = Modifier.height(130.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        CoinIllustration()

        Box(
            modifier = Modifier
                .size(width = 78.dp, height = 116.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF7F7F3))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("RECEIPT", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("MONEE SHOP", fontSize = 5.sp, color = Muted)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
                Text("Coffee      4.50", fontSize = 5.sp, color = Muted)
                Text("Lunch      12.00", fontSize = 5.sp, color = Muted)
                Text("Total      16.50", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
        }

        CoinIllustration()
    }
}

@Composable
private fun CoinIllustration() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Yellow),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
    }
}

@Composable
private fun MoneyIllustration() {
    Canvas(modifier = Modifier.size(width = 180.dp, height = 135.dp)) {
        val center = Offset(size.width * 0.38f, size.height * 0.42f)
        val radius = size.minDimension * 0.28f

        drawCircle(
            color = Color(0xFFFFE9A4),
            radius = radius,
            center = center
        )
        drawArc(
            color = Yellow,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
        drawArc(
            color = Color(0xFFEAB83A),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        val barBottom = size.height * 0.88f
        val barWidth = size.width * 0.11f
        val barGap = size.width * 0.06f
        val barStart = size.width * 0.48f

        drawRoundRect(
            color = Yellow,
            topLeft = Offset(barStart, barBottom - 32.dp.toPx()),
            size = Size(barWidth, 32.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = Yellow,
            topLeft = Offset(barStart + barWidth + barGap, barBottom - 58.dp.toPx()),
            size = Size(barWidth, 58.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = Yellow,
            topLeft = Offset(barStart + (barWidth + barGap) * 2, barBottom - 84.dp.toPx()),
            size = Size(barWidth, 84.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
    }
}

@Composable
private fun SplitBillIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            PersonIllustration(shirtColor = Color(0xFFFFD447))
            Text("↔", color = Yellow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            PersonIllustration(shirtColor = Color(0xFF8EB4C6))
        }

        Spacer(modifier = Modifier.height(8.dp))
        CoinIllustration()
    }
}

@Composable
private fun PersonIllustration(shirtColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(27.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD2A8))
        )
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 34.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(shirtColor)
        )
    }
}
