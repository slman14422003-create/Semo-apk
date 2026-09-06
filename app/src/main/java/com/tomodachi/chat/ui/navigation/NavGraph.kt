package com.tomodachi.chat.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomodachi.chat.data.repository.ServiceLocator
import com.tomodachi.chat.ui.AppViewModel
import com.tomodachi.chat.ui.admin.AdminScreen
import com.tomodachi.chat.ui.auth.LoginScreen
import com.tomodachi.chat.ui.chat.ChatScreen
import com.tomodachi.chat.ui.profile.ProfileScreen
import com.tomodachi.chat.ui.settings.SettingsScreen
import com.tomodachi.chat.ui.splash.SplashScreen
import kotlinx.coroutines.launch

// مدة موحّدة للانتقالات بين الشاشات — تلاشٍ ناعم مع تكبير خفيف بدل الانزلاق
// الافتراضي الحاد، لربط بصري أنيق بين السبلاش وشاشة تسجيل الدخول والواجهة الأساسية.
private const val TRANSITION_DURATION_MS = 380

@Composable
fun TomodachiNavGraph(navController: NavHostController = rememberNavController()) {
    val appViewModel: AppViewModel = viewModel()
    val currentUser by appViewModel.currentUser.collectAsStateWithLifecycle()
    val darkModePref by appViewModel.darkModePref.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = ServiceLocator.provideSessionManager(context)
    val favoriteStickers by sessionManager.favoriteStickers.collectAsState(initial = emptySet())
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            fadeIn(tween(TRANSITION_DURATION_MS)) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(TRANSITION_DURATION_MS)
            )
        },
        exitTransition = { fadeOut(tween(TRANSITION_DURATION_MS / 2)) },
        popEnterTransition = {
            fadeIn(tween(TRANSITION_DURATION_MS)) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(TRANSITION_DURATION_MS)
            )
        },
        popExitTransition = { fadeOut(tween(TRANSITION_DURATION_MS / 2)) }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNeedsLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onAlreadyLoggedIn = { user ->
                    // المستخدم كان مسجّلاً دخوله فعلاً — ننتقل مباشرة للواجهة
                    // الأساسية دون المرور بشاشة تسجيل الدخول إطلاقاً.
                    appViewModel.setCurrentUser(user)
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { user ->
                    appViewModel.setCurrentUser(user)
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Chat.route) {
            val user = currentUser
            if (user != null) {
                val bubbleShapePref by appViewModel.bubbleShapePref.collectAsStateWithLifecycle()
                val fontScalePref by appViewModel.fontScalePref.collectAsStateWithLifecycle()
                ChatScreen(
                    currentUser = user,
                    favoriteStickers = favoriteStickers,
                    bubbleShapeStyle = com.tomodachi.chat.ui.theme.BubbleShapeStyle.fromId(bubbleShapePref),
                    fontScale = fontScalePref,
                    onToggleFavoriteSticker = { id ->
                        coroutineScope.launch { sessionManager.toggleFavoriteSticker(id) }
                    },
                    onOpenProfile = { navController.navigate(Screen.Profile.route) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenAdmin = { navController.navigate(Screen.Admin.route) }
                )
            }
        }

        composable(Screen.Profile.route) {
            val user = currentUser
            if (user != null) {
                ProfileScreen(
                    currentUser = user,
                    onUserUpdated = { appViewModel.refreshCurrentUser() },
                    onLogout = {
                        appViewModel.logout {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Settings.route) {
            val accentColorHex by appViewModel.accentColorHex.collectAsStateWithLifecycle()
            val bubbleShapePref by appViewModel.bubbleShapePref.collectAsStateWithLifecycle()
            val fontScalePref by appViewModel.fontScalePref.collectAsStateWithLifecycle()
            SettingsScreen(
                darkModePref = darkModePref,
                onDarkModePrefChanged = { appViewModel.setDarkModePref(it) },
                accentColorHex = accentColorHex,
                onAccentColorChanged = { appViewModel.setAccentColorHex(it) },
                bubbleShapePref = bubbleShapePref,
                onBubbleShapeChanged = { appViewModel.setBubbleShapePref(it) },
                fontScalePref = fontScalePref,
                onFontScaleChanged = { appViewModel.setFontScalePref(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Admin.route) {
            val user = currentUser
            if (user != null && user.isAdmin) {
                AdminScreen(
                    currentUser = user,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
