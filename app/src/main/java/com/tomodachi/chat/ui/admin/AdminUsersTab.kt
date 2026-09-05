package com.tomodachi.chat.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomodachi.chat.data.model.User

@Composable
fun AdminUsersTab(users: List<User>, viewModel: AdminViewModel) {
    var userForAction by remember { mutableStateOf<User?>(null) }

    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا يوجد مستخدمون بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // مفتاح فريد آمن: يتجنّب أي تعارض لو فيه وثائق قديمة/تالفة بدون usernameLower
        items(users, key = { it.usernameLower.ifBlank { it.username.ifBlank { "u${it.hashCode()}" } } }) { user ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { userForAction = user }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(user.avatarEmoji, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            user.username.ifBlank { "(بدون اسم)" },
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            statusLabel(user),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor(user)
                        )
                        if (user.warningsCount > 0) {
                            Text(
                                "تحذيرات: ${user.warningsCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (user.isAdmin) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "أدمن",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
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
private fun statusColor(user: User) = when {
    user.isBanned -> MaterialTheme.colorScheme.error
    user.isOnline -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                FlowRowActions {
                    AssistTextButton("حظر دائم") { onBanPermanent(reason) }
                    AssistTextButton("فك الحظر") { onUnban() }
                    AssistTextButton("فك الحظر المؤقت") { onUnbanTemp() }
                    AssistTextButton("إرسال تحذير") { onWarn(reason) }
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
private fun FlowRowActions(content: @Composable () -> Unit) {
    // ترتيب بسيط بعمود واحد بدل صفوف تنكسر أحياناً على شاشات ضيقة
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
}

@Composable
private fun AssistTextButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, maxLines = 1)
    }
}
