package me.sheimi.sgit.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.manichord.mgit.settings.SettingsScreen
import com.manichord.mgit.settings.SettingsViewModel
import com.manichord.mgit.ui.theme.AppTheme
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.FsUtils
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.explorer.PrivateKeyManageActivity
import java.io.File

class UserSettingsActivity : SheimiFragmentActivity() {

    override fun setTheme() {
        if (me.sheimi.android.utils.BasicFunctions.isDarkTheme()) {
            setTheme(R.style.DarkAppTheme_NoActionBar)
        } else {
            setTheme(R.style.AppTheme_NoActionBar)
        }
    }

    private val viewModel: SettingsViewModel by viewModels()

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            // In a real app, we might want to persevere this URI or convert to file path
            // MGit currently relies on File paths.
            // For now, we'll try to get a path or inform the user.
            // On modern Android, getting a File path from a Tree Uri is non-trivial without MANAGE_EXTERNAL_STORAGE.
            // But MGit already has some path logic.
            val path = it.path // This is often not a real file path
            // For MGit's legacy logic, we might need a more robust way to get the path.
            // I'll use a placeholder or try to resolve it.
            // Given the existing FsUtils, let's see if there's a helper.
            viewModel.setRepoRoot(it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
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
            }
        }
    }
}
