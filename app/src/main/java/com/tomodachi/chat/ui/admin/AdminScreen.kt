package com.tomodachi.chat.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User

private data class AdminTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    AdminTab("المستخدمون", Icons.Filled.People),
    AdminTab("الكلمات الممنوعة", Icons.Filled.Block),
    AdminTab("الستيكرات", Icons.Filled.EmojiEmotions),
    AdminTab("منطقة الخطر", Icons.Filled.WarningAmber)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    currentUser: User,
    onBack: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }

    val users by viewModel.users.collectAsStateWithLifecycle()
    val bannedWords by viewModel.bannedWords.collectAsStateWithLifecycle()
    val uploadedStickers by viewModel.uploadedStickers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة تحكم المسؤول") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                TABS.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = null) }
                    )
                }
            }
            when (selectedTab) {
                0 -> AdminUsersTab(users = users, viewModel = viewModel)
                1 -> AdminBannedWordsTab(bannedWords = bannedWords, currentUserUid = currentUser.uid, viewModel = viewModel)
                2 -> AdminStickersTab(stickers = uploadedStickers, viewModel = viewModel)
                3 -> AdminDangerZoneTab(viewModel = viewModel)
            }
        }
    }
}
