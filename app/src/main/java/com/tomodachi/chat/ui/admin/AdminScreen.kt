package com.tomodachi.chat.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User

private val TAB_TITLES = listOf("المستخدمون", "الكلمات الممنوعة", "الستيكرات", "منطقة الخطر")

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
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                TAB_TITLES.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
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
