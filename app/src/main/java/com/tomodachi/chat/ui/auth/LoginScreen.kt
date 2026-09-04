package com.tomodachi.chat.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
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
    val remembered by viewModel.rememberedUsername.collectAsStateWithLifecycle(initialValue = null)
    var username by remember { mutableStateOf("") }
    var attemptedAutoLogin by remember { mutableStateOf(false) }

    LaunchedEffect(remembered) {
        if (!attemptedAutoLogin && !remembered.isNullOrBlank()) {
            attemptedAutoLogin = true
            username = remembered!!
            viewModel.login(remembered!!)
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎌", fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Tomodachi",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "أدخل اسم مستخدم للانضمام إلى الدردشة الجماعية",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            viewModel.resetError()
                        },
                        label = { Text("اسم المستخدم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val errorMessage = (uiState as? LoginUiState.Error)?.message
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = friendlyErrorMessage(errorMessage),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.login(username) },
                        enabled = uiState !is LoginUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState is LoginUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("دخول")
                        }
                    }
                }
            }
        }
    }
}

private fun friendlyErrorMessage(raw: String): String = when {
    raw == "empty" -> "الرجاء إدخال اسم مستخدم صالح"
    raw.startsWith("permanent:") -> "هذا الحساب محظور بشكل دائم."
    raw.startsWith("temporary:") -> {
        val parts = raw.removePrefix("temporary:").split(":")
        "هذا الحساب موقوف مؤقتاً. الوقت المتبقي: ${parts.getOrElse(0) { "0" }} دقيقة ${parts.getOrElse(1) { "0" }} ثانية"
    }
    else -> "حدث خطأ: $raw"
}
