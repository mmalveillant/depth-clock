package com.alijassib.depthclock

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import com.alijassib.depthclock.ui.DepthTheme
import com.alijassib.depthclock.ui.EditorScreen
import com.alijassib.depthclock.ui.EditorViewModel

class MainActivity : ComponentActivity() {

    private val vm: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // الواجهة داكنة دائمًا، فأيقونات الشريط فاتحة حتى لو التلفون بالوضع الفاتح
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { DepthTheme { EditorScreen(vm) } }
        // بعد تدوير الشاشة لا نعيد قص نفس الصورة
        if (savedInstanceState == null) handleSharedImage(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedImage(intent)
    }

    /** «مشاركة» أو «تعيين كـ» من الاستوديو. */
    private fun handleSharedImage(intent: Intent) {
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_ATTACH_DATA -> intent.data
            else -> null
        }
        if (uri != null) vm.onPhotoPicked(uri)
    }
}
