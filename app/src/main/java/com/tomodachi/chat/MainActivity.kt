package com.tomodachi.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tomodachi.chat.data.repository.ServiceLocator
import com.tomodachi.chat.ui.navigation.TomodachiNavGraph
import com.tomodachi.chat.ui.theme.TomodachiTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* لا حاجة لأي إجراء إضافي هنا؛ النظام يتعامل مع الحالة */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // يفعّل وضع Edge-to-Edge صراحة على كل إصدارات أندرويد المدعومة (وليس فقط
        // أندرويد 15+ حيث يفرضه النظام تلقائياً) — بهذا يمتد محتوى شاشاتنا خلف
        // شريط الحالة وشريط التنقل بشكل مقصود ومتحكَّم به، بدل أن يظهر شريط الحالة
        // بخلفية النظام الافتراضية غير المتناسقة مع ألوان تطبيقنا. لون الأيقونات
        // (فاتح/داكن) يُضبط لاحقاً بشكل تفاعلي داخل TomodachiTheme حسب وضع المظهر.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            TomodachiRoot()
        }
    }
}

@Composable
private fun TomodachiRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = ServiceLocator.provideSessionManager(context)
    val darkModePref by sessionManager.darkModePref.collectAsState(initial = "system")
    val accentColorHex by sessionManager.accentColorHex.collectAsState(initial = "")

    TomodachiTheme(darkModePref = darkModePref, accentColorHex = accentColorHex) {
        Surface(modifier = Modifier.fillMaxSize()) {
            TomodachiNavGraph()
        }
    }
}
