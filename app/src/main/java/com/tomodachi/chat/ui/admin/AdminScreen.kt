package com.tomodachi.chat.ui.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomodachi.chat.data.model.User
import com.tomodachi.chat.ui.theme.BrandGradient

private data class AdminTab(val title: String, val shortTitle: String, val icon: ImageVector)

private val TABS = listOf(
    AdminTab("المستخدمون", "المستخدمون", Icons.Filled.People),
    AdminTab("الكلمات الممنوعة", "الكلمات", Icons.Filled.Block),
    AdminTab("الستيكرات", "الستيكرات", Icons.Filled.EmojiEmotions),
    AdminTab("منطقة الخطر", "الخطر", Icons.Filled.WarningAmber)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(BrandGradient))
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "لوحة تحكم المسؤول",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "رجوع", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // محتوى التبويب الحالي — نترك مساحة سفلية كافية كي لا يختفي المحتوى
            // خلف الشريط العائم.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 84.dp)
            ) {
                when (selectedTab) {
                    0 -> AdminUsersTab(users = users, viewModel = viewModel)
                    1 -> AdminBannedWordsTab(bannedWords = bannedWords, currentUserUid = currentUser.uid, viewModel = viewModel)
                    2 -> AdminStickersTab(stickers = uploadedStickers, viewModel = viewModel)
                    3 -> AdminDangerZoneTab(viewModel = viewModel)
                }
            }

            FloatingAdminNavBar(
                selectedIndex = selectedTab,
                onSelected = { selectedTab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

/**
 * شريط تبويبات عائم بأسلوب One UI 8.5: كبسولة واحدة تطفو فوق المحتوى بدل شريط
 * تبويبات علوي تقليدي، مع مؤشر خلفي متحرك يتبع العنصر المختار ويعرض تسميته
 * فقط عند التحديد، لتوفير مساحة وأناقة بصرية على شاشات الهاتف الضيقة.
 */
@Composable
private fun FloatingAdminNavBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TABS.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            AdminNavItem(
                tab = tab,
                isSelected = isSelected,
                onClick = { onSelected(index) },
                modifier = if (isSelected) Modifier.weight(1.6f) else Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AdminNavItem(
    tab: AdminTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "admin_nav_bg"
    )
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val horizontalPadding by animateDpAsState(if (isSelected) 14.dp else 0.dp, label = "admin_nav_padding")

    Row(
        modifier = modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(tab.icon, contentDescription = tab.title, tint = contentColor, modifier = Modifier.size(20.dp))
        if (isSelected) {
            Spacer(Modifier.width(6.dp))
            Text(
                tab.shortTitle,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
