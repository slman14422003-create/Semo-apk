package com.tomodachi.chat.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.ui.components.BrandMark
import com.tomodachi.chat.ui.components.BrandWordmark
import com.tomodachi.chat.ui.theme.GradientOrange
import com.tomodachi.chat.ui.theme.GradientPink
import com.tomodachi.chat.ui.theme.GradientPurple
import kotlinx.coroutines.delay

// منحنى "back-ease" لطيف (يتجاوز الحجم النهائي بقليل ثم يستقرّ) لحركة ظهور
// الشعار — مُعرَّف يدوياً بدل EaseOutBack كي لا يعتمد على إصدار مكتبة حركة
// معيّن، ويبقى يعمل مع أي إصدار Compose يدعم CubicBezierEasing (كل الإصدارات).
private val BackOutEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * شاشة الإقلاع: تتولى كل عمليات تهيئة التطبيق (حالياً: محاولة استرجاع الجلسة
 * السابقة تلقائياً) عبر [SplashViewModel]، ثم تنتقل بحركة انتقالية سلسة إمّا
 * مباشرة إلى الواجهة الأساسية (إن كان المستخدم مسجّلاً دخوله فعلاً) أو إلى
 * شاشة تسجيل الدخول (إن احتاج لذلك) — دون أي وميض أو قفزة مفاجئة بين الشاشتين.
 */
@Composable
fun SplashScreen(
    onNeedsLogin: () -> Unit,
    onAlreadyLoggedIn: (User) -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    val logoScale = remember { Animatable(0.55f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    var screenAlpha by remember { mutableStateOf(1f) }

    // نبضة توهّج خفيفة ومستمرة خلف الشعار — تمنح شعوراً "حيّاً" أثناء التحميل.
    val glowTransition = rememberInfiniteTransition(label = "splashGlow")
    val glowScale by glowTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // دوران نقاط "جاري التحميل" الثلاث بأسلوب تتابعي بسيط.
    val dotsTransition = rememberInfiniteTransition(label = "splashDots")
    val dotsProgress by dotsTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotsProgress"
    )

    // حركة الدخول: الشعار يظهر ويتّسع بأسلوب "back-ease" أنيق، ثم يتبعه النص
    // الفرعي وحالة التحميل بعد لحظة قصيرة (تسلسل بصري مريح بدل ظهور كل شيء دفعة واحدة).
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        logoScale.animateTo(1f, animationSpec = tween(650, easing = BackOutEasing))
        contentAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
    }

    // متى ما استقرّت الوجهة (انتهت التهيئة)، نُشغّل خروجاً بتلاشٍ ناعم قبل الانتقال
    // الفعلي، بدل قطع الشاشة فجأة — ما يمنح إحساساً بالربط الجميل بين السبلاش
    // والشاشة التالية سواء كانت تسجيل الدخول أو الدردشة.
    LaunchedEffect(destination) {
        val result = destination ?: return@LaunchedEffect
        screenAlpha = 1f
        val steps = 16
        repeat(steps) { step ->
            delay(12)
            screenAlpha = 1f - (step + 1) / steps.toFloat()
        }
        when (result) {
            is SplashDestination.LoggedIn -> onAlreadyLoggedIn(result.user)
            SplashDestination.NeedsLogin -> onNeedsLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface,
                        GradientPurple.copy(alpha = 0.10f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // هالة توهّج متدرّجة خلف الشعار
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(glowScale)
                        .alpha(glowAlpha * logoAlpha.value)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    GradientPurple.copy(alpha = 0.35f),
                                    GradientPink.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                BrandMark(
                    size = 96.dp,
                    modifier = Modifier
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                )
            }

            Spacer(Modifier.height(18.dp))

            BrandWordmark(
                fontSize = 32.sp,
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .scale(0.9f + 0.1f * logoScale.value.coerceAtMost(1f))
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "دردشة جماعية بسيطة وسريعة",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha.value)
            )

            Spacer(Modifier.height(36.dp))

            AnimatedVisibility(
                visible = contentAlpha.value > 0.4f,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(150))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingDots(progress = dotsProgress)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = statusLabel(destination),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun statusLabel(destination: SplashDestination?): String = when (destination) {
    null -> "جاري التحقق من الجلسة..."
    is SplashDestination.LoggedIn -> "تم تسجيل الدخول، جاري الانتقال..."
    SplashDestination.NeedsLogin -> "جاهز..."
}

/** ثلاث نقاط تُضيء بالتتابع لتوحي بأن التطبيق يعمل على تهيئة نفسه في الخلفية. */
@Composable
private fun LoadingDots(progress: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val distance = kotlin.math.abs(progress - index)
            val highlighted = distance < 0.5f
            val dotAlpha = if (highlighted) 1f else 0.3f
            val dotScale = if (highlighted) 1.15f else 0.85f
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(dotScale)
                    .alpha(dotAlpha)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(GradientPurple, GradientPink, GradientOrange)
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}
