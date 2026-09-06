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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.blur
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
import com.tomodachi.chat.ui.theme.IconBlueBright
import com.tomodachi.chat.ui.theme.IconBlueDeep
import com.tomodachi.chat.ui.theme.IconBlueMid
import kotlinx.coroutines.delay

// منحنى "back-ease" لطيف (يتجاوز الحجم النهائي بقليل ثم يستقرّ) لحركة ظهور
// الشعار — مُعرَّف يدوياً بدل EaseOutBack كي لا يعتمد على إصدار مكتبة حركة
// معيّن، ويبقى يعمل مع أي إصدار Compose يدعم CubicBezierEasing (كل الإصدارات).
private val BackOutEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * شاشة الإقلاع — أعيد تصميمها بالكامل بخلفية متدرّجة كاملة الامتداد بنفس
 * هوية أيقونة التطبيق (أزرق فاتح ← نيلي غامق)، مع كُرَتَي ضوء ناعمتين
 * متحركتين خلف الشعار لإحساس أكثر حيوية وعمقاً بدل خلفية مسطّحة واحدة،
 * وبطاقة زجاجية شفافة (glass) تحتضن الشعار بدل عرضه عائماً فوق الخلفية مباشرة.
 *
 * تتولى الشاشة كل عمليات تهيئة التطبيق (حالياً: محاولة استرجاع الجلسة
 * السابقة تلقائياً) عبر [SplashViewModel]، ثم تنتقل بحركة تلاشٍ سلسة إمّا
 * مباشرة إلى الواجهة الأساسية أو إلى شاشة تسجيل الدخول، دون أي وميض مفاجئ.
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

    // حركة عائمة بطيئة لكرتَي الضوء خلف البطاقة — تمنح خلفية السبلاش عمقاً
    // وحيوية بدل خلفية متدرّجة ثابتة تماماً.
    val blobTransition = rememberInfiniteTransition(label = "splashBlobs")
    val blobOneOffset by blobTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "blobOne"
    )
    val blobTwoOffset by blobTransition.animateFloat(
        initialValue = 16f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(tween(6100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "blobTwo"
    )

    // نبضة توهّج خفيفة ومستمرة خلف الشعار — تمنح شعوراً "حيّاً" أثناء التحميل.
    val glowTransition = rememberInfiniteTransition(label = "splashGlow")
    val glowScale by glowTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
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
    // الفعلي، بدل قطع الشاشة فجأة.
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
                    colors = listOf(IconBlueBright, IconBlueMid, IconBlueDeep)
                )
            )
    ) {
        // كرتا ضوء ناعمتان عائمتان خلف كل شيء لإضفاء عمق على الخلفية المتدرّجة.
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopStart)
                .offset(x = (-70 + blobOneOffset).dp, y = (-60 + blobOneOffset).dp)
                .blur(60.dp)
                .background(Color.White.copy(alpha = 0.16f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (60 + blobTwoOffset).dp, y = (70 + blobTwoOffset).dp)
                .blur(60.dp)
                .background(IconBlueDeep.copy(alpha = 0.4f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // هالة توهّج بيضاء خلف الشعار
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .scale(glowScale)
                            .alpha(glowAlpha * logoAlpha.value)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.5f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    // بطاقة زجاجية شفافة تحتضن الشعار — بدل عرضه عائماً مباشرة
                    // على الخلفية المتدرّجة، لعمق بصري أقرب لواجهات 2026 الحديثة.
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .scale(logoScale.value)
                            .alpha(logoAlpha.value)
                            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(34.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        BrandMark(size = 88.dp)
                    }
                }

                Spacer(Modifier.height(22.dp))

                BrandWordmarkOnGradient(
                    fontSize = 34.sp,
                    modifier = Modifier.alpha(logoAlpha.value)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "دردشة جماعية بسيطة وسريعة",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(contentAlpha.value)
                )
            }

            Spacer(Modifier.height(52.dp))

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
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/** نص العلامة أبيض ثابت (لا يتبدّل بتدرّج اللوغو) ليقرأ بوضوح فوق الخلفية الزرقاء الغامقة. */
@Composable
private fun BrandWordmarkOnGradient(fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    Text(
        "Tomodachi",
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    )
}

private fun statusLabel(destination: SplashDestination?): String = when (destination) {
    null -> "جاري التحقق من الجلسة..."
    is SplashDestination.LoggedIn -> "تم تسجيل الدخول، جاري الانتقال..."
    SplashDestination.NeedsLogin -> "جاهز..."
}

/** ثلاث نقاط بيضاء تُضيء بالتتابع لتوحي بأن التطبيق يعمل على تهيئة نفسه في الخلفية. */
@Composable
private fun LoadingDots(progress: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val distance = kotlin.math.abs(progress - index)
            val highlighted = distance < 0.5f
            val dotAlpha = if (highlighted) 1f else 0.35f
            val dotScale = if (highlighted) 1.15f else 0.85f
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(dotScale)
                    .alpha(dotAlpha)
                    .background(Color.White, CircleShape)
            )
        }
    }
}
