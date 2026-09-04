package com.tomodachi.chat.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomodachi.chat.data.model.User

@Composable
fun AdminUsersTab(users: List<User>, viewModel: AdminViewModel) {
    var userForAction by remember { mutableStateOf<User?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(users, key = { it.usernameLower }) { user ->
            ListItem(
                headlineContent = { Text("${user.avatarEmoji} ${user.username}") },
                supportingContent = {
                    Column {
                        Text(statusLabel(user))
                        Text("تحذيرات: ${user.warningsCount}")
                    }
                },
                trailingContent = {
                    TextButton(onClick = { userForAction = user }) { Text("إدارة") }
                }
            )
            HorizontalDivider()
        }
    }

    val target = userForAction
    if (target != null) {
        UserActionsDialog(
            user = target,
            onDismiss = { userForAction = null },
            onBanPermanent = { viewModel.banPermanently(target.usernameLower, it); userForAction = null },
            onUnban = { viewModel.unban(target.usernameLower); userForAction = null },
            onUnbanTemp = { viewModel.unbanTemporary(target.usernameLower); userForAction = null },
            onWarn = { viewModel.sendWarning(target.usernameLower, it); userForAction = null },
            onPromote = { viewModel.promoteToAdmin(target.usernameLower); userForAction = null },
            onDeleteAccount = { viewModel.deleteUserAccount(target.usernameLower); userForAction = null }
        )
    }
}

private fun statusLabel(user: User): String = when {
    user.isBannedPermanently -> "محظور دائماً"
    user.isTemporarilyBanned -> "محظور مؤقتاً"
    user.isOnline -> "متصل الآن"
    else -> "غير متصل"
}

@Composable
private fun UserActionsDialog(
    user: User,
    onDismiss: () -> Unit,
    onBanPermanent: (String) -> Unit,
    onUnban: () -> Unit,
    onUnbanTemp: () -> Unit,
    onWarn: (String) -> Unit,
    onPromote: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة ${user.username}") },
        text = {
            Column {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistTextButton("حظر دائم") { onBanPermanent(reason) }
                    AssistTextButton("فك الحظر") { onUnban() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistTextButton("فك الحظر المؤقت") { onUnbanTemp() }
                    AssistTextButton("إرسال تحذير") { onWarn(reason) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistTextButton("ترقية لمسؤول") { onPromote() }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { confirmDelete = true }) {
                    Text("حذف الحساب نهائياً", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف الحساب نهائياً") },
            text = { Text("سيتم حذف حساب ${user.username} وكل رسائله نهائياً. هل أنت متأكد؟") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteAccount() }) {
                    Text("تأكيد", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun AssistTextButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, maxLines = 1)
    }
}
