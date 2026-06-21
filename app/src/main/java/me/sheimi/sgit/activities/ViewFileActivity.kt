package me.sheimi.sgit.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.IntentCompat
import com.manichord.mgit.ui.components.FragmentHost
import com.manichord.mgit.ui.theme.AppTheme
import com.manichord.mgit.viewfile.ViewFileScreen
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.CodeGuesser
import me.sheimi.android.utils.FsUtils
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.dialogs.ChooseLanguageDialog
import me.sheimi.sgit.fragments.CommitsFragment
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File

class ViewFileActivity : SheimiFragmentActivity() {

    companion object {
        const val TAG_FILE_NAME = "file_name"
        const val TAG_MODE = "mode"
        const val TAG_MODE_NORMAL: Short = 0
        const val TAG_MODE_SSH_KEY: Short = 1
        private const val JS_INTERFACE = "CodeLoader"
    }

    private var commitsFragment: CommitsFragment? = null
    private var activityMode: Short = TAG_MODE_NORMAL
    private lateinit var file: File
    private var webView: WebView? = null
    private var code: String? = null

    private var currentTab by mutableStateOf(0)
    private var searchActive by mutableStateOf(false)
    private var searchQuery by mutableStateOf("")
    private var isLoadingFile by mutableStateOf(true)

    override fun getThemeResource(): Int {
        return if (Profile.getTheme(this) == 1) {
            R.style.DarkAppTheme_NoActionBar
        } else {
            R.style.AppTheme_NoActionBar
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = IntentCompat.getSerializableExtra(intent, Repo.TAG, Repo::class.java)
        val extras = intent.extras!!
        val fileName = extras.getString(TAG_FILE_NAME)!!
        activityMode = extras.getShort(TAG_MODE, TAG_MODE_NORMAL)
        file = File(fileName)

        if (repo != null) {
            commitsFragment = CommitsFragment.newInstance(repo, FsUtils.getRelativePath(File(fileName), repo.dir))
        }

        val screenTitle = File(fileName).name

        setContent {
            AppTheme {
                ViewFileScreen(
                    title = screenTitle,
                    hasCommitsTab = commitsFragment != null,
                    activityMode = activityMode,
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    onBackClick = { finish() },
                    onEditClick = {
                        if (activityMode != TAG_MODE_SSH_KEY) {
                            FsUtils.openFile(this, file)
                        }
                    },
                    onChooseLanguageClick = {
                        if (activityMode != TAG_MODE_SSH_KEY) {
                            ChooseLanguageDialog().show(supportFragmentManager, "choose language")
                        }
                    },
                    onCopyAllClick = { copyAll() },
                    searchActive = searchActive,
                    onSearchActiveChange = { active ->
                        searchActive = active
                        if (!active) {
                            searchQuery = ""
                            commitsFragment?.setFilter(null)
                        }
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        commitsFragment?.setFilter(query)
                    },
                    fileContent = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { context -> WebView(context).also(::setupWebView) },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isLoadingFile) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    },
                    commitsContent = {
                        commitsFragment?.let { FragmentHost(supportFragmentManager, it) }
                    }
                )
            }
        }
    }

    private fun setupWebView(view: WebView) {
        if (webView != null) return // AndroidView's factory should only run once; guard anyway
        webView = view
        view.addJavascriptInterface(CodeLoader(), JS_INTERFACE)
        view.settings.javaScriptEnabled = true
        view.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Timber.d(
                    "%s -- From line %d of %s",
                    consoleMessage.message(), consoleMessage.lineNumber(), consoleMessage.sourceId()
                )
                return true
            }
        }
        view.setBackgroundColor(Color.TRANSPARENT)

        // AndroidView's factory runs before Compose's layout pass gives this WebView its real
        // size (it's 0x0 here) -- loading the page immediately bakes a 0-height viewport into
        // Chromium's CSS layout for vh/percentage-height rules (CodeMirror needs an explicit
        // non-zero height container), which never gets reconciled even once the View is later
        // resized. Deferring the load until the view actually has a size avoids this entirely.
        if (view.width > 0 && view.height > 0) {
            view.loadUrl("file:///android_asset/editor.html")
        } else {
            view.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                if (v.width > 0 && v.height > 0 && view.url == null) {
                    view.loadUrl("file:///android_asset/editor.html")
                }
            }
        }
    }

    fun setLanguage(lang: String) {
        webView?.loadUrl(CodeGuesser.wrapUrlScript("setLang('$lang')"))
    }

    private fun copyAll() {
        webView?.loadUrl(CodeGuesser.wrapUrlScript("copy_all();"))
    }

    private fun showUserError(e: Throwable, errorMessageId: Int) {
        Timber.e(e)
        runOnUiThread {
            showMessageDialog(R.string.dialog_error_title, getString(errorMessageId))
        }
    }

    private inner class CodeLoader {

        @JavascriptInterface
        fun getCode(): String? = code

        @JavascriptInterface
        fun copy_all(content: String) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("mgit", content))
        }

        @JavascriptInterface
        fun loadCode() {
            Thread {
                try {
                    code = FileUtils.readFileToString(file)
                } catch (e: java.io.IOException) {
                    showUserError(e, R.string.error_can_not_open_file)
                }
                display()
            }.start()
        }

        @JavascriptInterface
        fun getTheme(): String = Profile.getCodeMirrorTheme(this@ViewFileActivity)

        private fun display() {
            runOnUiThread {
                val lang = if (activityMode == TAG_MODE_SSH_KEY) null else CodeGuesser.guessCodeType(file.name)
                webView?.loadUrl(CodeGuesser.wrapUrlScript("setLang('$lang')"))
                isLoadingFile = false
                webView?.loadUrl(CodeGuesser.wrapUrlScript("display();"))
            }
        }
    }
}
