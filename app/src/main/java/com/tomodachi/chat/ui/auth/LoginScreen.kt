package com.tomodachi.chat.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is LoginUiState.Success) onLoginSuccess(state.user)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (uiState is LoginUiState.CheckingSession) {
            CircularProgressIndicator(color = Color.White)
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎌", fontSize = 52.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Tomodachi",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (mode == AuthMode.LOGIN) "سجّل الدخول للانضمام إلى الدردشة الجماعية"
                else "أنشئ حسابك للانضمام إلى الدردشة الجماعية",
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {

                    // مبدّل تسجيل الدخول / إنشاء حساب
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

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; viewModel.resetError() },
                        label = { Text("اسم المستخدم") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.resetError() },
                        label = { Text("كلمة السر") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedContent(
                        targetState = mode == AuthMode.REGISTER,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "confirmPasswordField"
                    ) { showConfirm ->
                        if (showConfirm) {
                            Column {
                                Spacer(Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it; viewModel.resetError() },
                                    label = { Text("تأكيد كلمة السر") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                            Icon(
                                                if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    val errorMessage = (uiState as? LoginUiState.Error)?.message
                    if (errorMessage != null) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = friendlyErrorMessage(errorMessage),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (mode == AuthMode.LOGIN) viewModel.login(username, password)
                            else viewModel.register(username, password, confirmPassword)
                        },
                        enabled = uiState !is LoginUiState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState is LoginUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (mode == AuthMode.LOGIN) "دخول" else "إنشاء الحساب")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(vertical = 10.dp)
            .then(Modifier.clickableNoRipple(onClick)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    androidx.compose.foundation.clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
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
