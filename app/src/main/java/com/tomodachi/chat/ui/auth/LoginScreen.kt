package com.tomodachi.chat.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.ui.components.BrandMark
import com.tomodachi.chat.ui.components.BrandWordmark
import com.tomodachi.chat.ui.theme.BrandGradient
import com.tomodachi.chat.ui.theme.GradientPurple
import com.tomodachi.chat.ui.theme.GradientRed

@Composable
fun LoginScreen(
    onLoginSuccess: (User) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    // حركة دخول ناعمة (تلاشٍ + انزلاق للأعلى) عند وصول الشاشة، لتبدو استمراراً
    // طبيعياً وأنيقاً لحركة الخروج من السبلاش بدل ظهور مفاجئ.
    val entryProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        entryProgress.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is LoginUiState.Success) onLoginSuccess(state.user)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface,
                        GradientPurple.copy(alpha = 0.08f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .alpha(entryProgress.value)
                .offset(y = (24 * (1f - entryProgress.value)).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(28.dp))
            BrandMark(size = 72.dp)
            Spacer(Modifier.height(14.dp))
            BrandWordmark(fontSize = 30.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (mode == AuthMode.LOGIN) "سجّل الدخول للانضمام إلى الدردشة الجماعية"
                else "أنشئ حسابك للانضمام إلى الدردشة الجماعية",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(28.dp))

            // بطاقة النموذج: سطح مرتفع بحواف دائرية وظلّ خفيف، يفصل بصرياً بين
            // خلفية التدرّج والحقول، بأسلوب عصري أقرب لتطبيقات التواصل الحديثة.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp)
            ) {
                // مبدّل تسجيل الدخول / إنشاء حساب — شكل شرائح بأسلوب انستقرام
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    AuthModeTab(
                        text = "تسجيل الدخول",
                        selected = mode == AuthMode.LOGIN,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.setMode(AuthMode.LOGIN)
                        confirmPassword = ""
                    }
                    AuthModeTab(
                        text = "إنشاء حساب",
                        selected = mode == AuthMode.REGISTER,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.setMode(AuthMode.REGISTER)
                    }
                }

                Spacer(Modifier.height(22.dp))

                LoginField(
                    value = username,
                    onValueChange = { username = it; viewModel.resetError() },
                    placeholder = "اسم المستخدم",
                    leadingIcon = Icons.Filled.Person
                )

                Spacer(Modifier.height(12.dp))

                LoginField(
                    value = password,
                    onValueChange = { password = it; viewModel.resetError() },
                    placeholder = "كلمة السر",
                    leadingIcon = Icons.Filled.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible }
                )

                AnimatedContent(
                    targetState = mode == AuthMode.REGISTER,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "confirmPasswordField"
                ) { showConfirm ->
                    if (showConfirm) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            LoginField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; viewModel.resetError() },
                                placeholder = "تأكيد كلمة السر",
                                leadingIcon = Icons.Filled.Lock,
                                isPassword = true,
                                passwordVisible = confirmVisible,
                                onTogglePasswordVisibility = { confirmVisible = !confirmVisible }
                            )
                        }
                    }
                }

                val errorMessage = (uiState as? LoginUiState.Error)?.message
                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = friendlyErrorMessage(errorMessage),
                            color = GradientRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                val isLoading = uiState is LoginUiState.Loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = if (isLoading) {
                                val disabled = MaterialTheme.colorScheme.surfaceVariant
                                Brush.horizontalGradient(listOf(disabled, disabled))
                            } else {
                                Brush.horizontalGradient(BrandGradient)
                            }
                        )
                        .clickable(enabled = !isLoading) {
                            if (mode == AuthMode.LOGIN) viewModel.login(username, password)
                            else viewModel.register(username, password, confirmPassword)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            if (mode == AuthMode.LOGIN) "دخول" else "إنشاء الحساب",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
            {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthModeTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

private fun friendlyErrorMessage(raw: String): String = when {
    raw == "empty_username" -> "الرجاء إدخال اسم مستخدم صالح"
    raw == "username_too_long" -> "اسم المستخدم طويل جداً (24 حرفاً كحد أقصى)"
    raw == "invalid_username_chars" -> "اسم المستخدم يجب أن يحتوي أحرفاً وأرقاماً فقط"
    raw == "short_password" -> "كلمة السر يجب أن تكون 6 أحرف على الأقل"
    raw == "password_mismatch" -> "كلمتا السر غير متطابقتين"
    raw == "username_taken" -> "اسم المستخدم هذا محجوز مسبقاً"
    raw == "user_not_found" -> "لا يوجد حساب بهذا الاسم، جرّب إنشاء حساب جديد"
    raw == "wrong_password" -> "كلمة السر غير صحيحة"
    raw == "too_many_requests" -> "محاولات كثيرة، حاول مرة أخرى بعد قليل"
    raw.startsWith("permanent:") -> "هذا الحساب محظور بشكل دائم."
    raw.startsWith("temporary:") -> {
        val parts = raw.removePrefix("temporary:").split(":")
        "هذا الحساب موقوف مؤقتاً. الوقت المتبقي: ${parts.getOrElse(0) { "0" }} دقيقة ${parts.getOrElse(1) { "0" }} ثانية"
    }
    else -> "حدث خطأ: $raw"
}
