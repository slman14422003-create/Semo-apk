package com.tomodachi.chat.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AdminDangerZoneTab(viewModel: AdminViewModel) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "حذف جميع رسائل الدردشة نهائياً",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "هذا الإجراء يحذف كل الرسائل في الدردشة الجماعية بشكل نهائي ولا يمكن التراجع عنه.",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف جميع الرسائل نهائياً", color = Color.White)
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("تأكيد الحذف الشامل") },
            text = { Text("هل أنت متأكد؟ سيتم حذف جميع رسائل الدردشة نهائياً ولا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; viewModel.deleteAllMessages() }) {
                    Text("تأكيد", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("إلغاء") } }
        )
    }
}
