package com.tomodachi.chat.ui.theme

import androidx.compose.ui.graphics.Color

// تدرّج العلامة المستوحى من انستقرام (بنفسجي ← وردي ← برتقالي ← أصفر)
val GradientPurple = Color(0xFF833AB4)
val GradientPink = Color(0xFFFD1D8D)
val GradientRed = Color(0xFFFD1D1D)
val GradientOrange = Color(0xFFF77737)
val GradientYellow = Color(0xFFFCAF45)

val BrandGradient = listOf(GradientPurple, GradientPink, GradientOrange, GradientYellow)
val BrandGradientSoft = listOf(GradientPurple, GradientRed, GradientOrange)

// تدرّج مطابق للون أيقونة التطبيق الفعلية على الجهاز (أزرق فاتح ← نيلي غامق) —
// مُستخرَج مباشرة من ألوان mipmap/ic_launcher_background_gen. يُستخدم حصرياً في
// شاشتي السبلاش وتسجيل الدخول ليكون مظهرهما امتداداً بصرياً مباشراً لأيقونة
// التطبيق كما يراها المستخدم على شاشته الرئيسية، بدل تدرّج انستقرام القديم.
val IconBlueBright = Color(0xFF1877F2)
val IconBlueMid = Color(0xFF3D5FE8)
val IconBlueDeep = Color(0xFF5528E3)

val BrandGradientBlue = listOf(IconBlueBright, IconBlueMid, IconBlueDeep)

// أزرق الأزرار/الروابط بأسلوب انستقرام
val IgBlue = Color(0xFF0095F6)
val IgBlueDark = Color(0xFF4DABF7)

// درجات محايدة — الوضع الفاتح
val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF262626)
val LightOnSurfaceVariant = Color(0xFF8E8E8E)
val LightDivider = Color(0xFFDBDBDB)
val LightFieldFill = Color(0xFFEFEFEF)
val LightBubbleOther = Color(0xFFEFEFEF)

// درجات محايدة — الوضع الداكن
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF121212)
val DarkOnSurface = Color(0xFFFAFAFA)
val DarkOnSurfaceVariant = Color(0xFFA8A8A8)
val DarkDivider = Color(0xFF262626)
val DarkFieldFill = Color(0xFF1E1E1E)
val DarkBubbleOther = Color(0xFF262626)

// أسماء قديمة أُبقيت للتوافق مع أي كود آخر يشير إليها
val TomodachiPrimaryLight = IgBlue
val TomodachiPrimaryDark = IgBlueDark
val TomodachiSecondary = GradientPurple
