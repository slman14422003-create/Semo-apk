package com.tomodachi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.app.viewmodel.LoginState

@Composable
fun LoginScreen(
    loginState: LoginState,
    onLogin: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💬", fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Semo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "دردشة جماعية مباشرة",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("اسمك") },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (username.isNotBlank()) onLogin(username) }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (loginState is LoginState.Error) {
                Text(
                    loginState.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = { onLogin(username) },
                enabled = loginState !is LoginState.LoggingIn && username.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (loginState is LoginState.LoggingIn) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("دخول / إنشاء حساب")
                }
            }
        }
    }
}
