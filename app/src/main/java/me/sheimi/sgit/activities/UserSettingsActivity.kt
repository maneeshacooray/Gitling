package me.sheimi.sgit.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.manichord.mgit.settings.SettingsScreen
import com.manichord.mgit.settings.SettingsViewModel
import com.manichord.mgit.ui.theme.AppTheme
import com.manichord.mgit.util.resolvePrimaryVolumePath
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.explorer.PrivateKeyManageActivity

class UserSettingsActivity : SheimiFragmentActivity() {

    override fun getThemeResource(): Int {
        return if (Profile.getTheme(this) == 1) {
            R.style.DarkAppTheme_NoActionBar
        } else {
            R.style.AppTheme_NoActionBar
        }
    }

    private val viewModel: SettingsViewModel by viewModels()

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            val path = resolvePrimaryVolumePath(it)
            if (path != null) {
                viewModel.setRepoRoot(path)
            } else {
                showToastMessage("That location isn't supported -- please pick a folder on internal storage.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh accounts in case the user completed GitHub OAuth in the browser
        viewModel.refreshAccounts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("settings") }

            AppTheme {
                androidx.compose.animation.Crossfade(targetState = currentScreen) { screen ->
                    when (screen) {
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onBackClick = { finish() },
                            onManageAccountsClick = { currentScreen = "accounts" },
                            onManageSshKeysClick = {
                                startActivity(Intent(this, PrivateKeyManageActivity::class.java))
                            },
                            onFeedbackClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.feedback_url)))
                                startActivity(intent)
                            },
                            onRepoRootClick = {
                                folderPickerLauncher.launch(null)
                            }
                        )
                        "accounts" -> com.manichord.mgit.settings.AccountsScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = "settings" }
                        )
                    }
                }
            }
        }
    }
}
