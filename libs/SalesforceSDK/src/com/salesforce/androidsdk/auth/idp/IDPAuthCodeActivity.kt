package com.salesforce.androidsdk.auth.idp

/**
 * Activity showing web view through which IDP gets auth code for SP app
 */
import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.salesforce.androidsdk.app.SalesforceSDKManager
import com.salesforce.androidsdk.util.LogUtil
import com.salesforce.androidsdk.util.SalesforceSDKLogger
import com.salesforce.androidsdk.auth.idp.interfaces.IDPAuthCodeActivity as IDPAuthCodeActivityInterface

class IDPAuthCodeActivity : ComponentActivity(), IDPAuthCodeActivityInterface {

    companion object {
        private val TAG: String = IDPAuthCodeActivity::class.java.simpleName
    }

    private lateinit var _webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Protects against screenshots.
        window.setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE)

        // TODO fix theming
//        // Set theme
//        val isDarkTheme = SalesforceSDKManager.getInstance().isDarkTheme
//        setTheme(if (isDarkTheme) R.style.SalesforceSDK_Dark_Login else R.style.SalesforceSDK)
//        SalesforceSDKManager.getInstance().setViewNavigationVisibility(this)

        setContent {
            IDPAuthCodeScreen(intent = intent)
        }
    }

    @Composable
    fun IDPAuthCodeScreen(intent: android.content.Intent) {
        val context = LocalContext.current
        val idpManager = SalesforceSDKManager.getInstance().idpManager as? IDPManager
        var isLoading by remember { mutableStateOf(true) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.width(64.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        _webView = this

                        settings.apply {
                            javaScriptEnabled = true
                            useWideViewPort = true
                            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                        }

                        // Initially invisible
                        visibility = View.INVISIBLE

                        // Don't draw scrollbars
                        isHorizontalScrollBarEnabled = false
                        isVerticalScrollBarEnabled = false

                        // Set WebViewClient and pass `isLoading` state
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Hide the spinner when the page finishes loading
                                isLoading = false
                                // Show web view
                                view?.visibility = View.VISIBLE
                            }
                        }

                        idpManager?.let { manager ->
                            val spAppPackageName = manager.getSrcAppPackageName(intent)
                            manager.attachToActiveFlow(context as IDPAuthCodeActivity, context, spAppPackageName)
                            manager.onReceive(context, intent)
                        } ?: run {
                            SalesforceSDKLogger.d(
                                TAG,
                                "No IDP manager to handle ${LogUtil.intentToString(intent)}"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(16.dp)
            )
        }
    }

    override val webView: WebView
        get() = _webView
}
