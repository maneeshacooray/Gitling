@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.sheimi.sgit.activities.explorer

import androidx.compose.material3.ButtonDefaults
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import com.manichord.mgit.MainActivity
import com.manichord.mgit.ssh.PrivateKeyGenerate
import me.sheimi.android.utils.FsUtils
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.ViewFileActivity
import me.sheimi.sgit.dialogs.EditKeyPasswordDialog
import me.sheimi.sgit.dialogs.RenameKeyDialog
import me.sheimi.sgit.ssh.PrivateKeyUtils
import timber.log.Timber
import java.io.File

class PrivateKeyManageActivity(
    mainActivity: MainActivity
) : FileExplorerActivity(
    mainActivity,
    PrivateKeyUtils.getPrivateKeyFolder(),
    null,
    mainActivity.getString(R.string.title_activity_private_key_manage)
) {
    private var showDeleteConfirm by mutableStateOf(false)

    /** Copies the key the user picked through the system file picker (a content Uri, since the
     * app can't reach shared storage by path) into the private-key folder, keeping its name. */
    fun importKeyFromUri(uri: Uri) {
        val name = queryDisplayName(uri) ?: "imported_key"
        val newKey = File(currentDir, name)
        try {
            mainActivity.contentResolver.openInputStream(uri)?.use { input ->
                newKey.outputStream().use { output -> input.copyTo(output) }
            } ?: throw java.io.IOException("Could not open $uri")
        } catch (e: Exception) {
            Timber.e(e, "Failed to import private key")
            mainActivity.showToastMessage(R.string.error_import_key_failed)
            return
        }
        mainActivity.showToastMessage(R.string.success_import_key)
        refreshList()
    }

    private fun queryDisplayName(uri: Uri): String? =
        mainActivity.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    override fun onFileClick(file: File) {
        if (file.isDirectory) {
            setCurrentDir(file)
            return
        }
        val intent = Intent(this, ViewFileActivity::class.java)
        intent.putExtra(ViewFileActivity.TAG_FILE_NAME, PrivateKeyUtils.getPublicKeyEnsure(file).absolutePath)
        intent.putExtra(ViewFileActivity.TAG_MODE, ViewFileActivity.TAG_MODE_SSH_KEY)
        startActivity(intent)
    }

    override fun onFileLongClick(file: File) {
        selectedFile = file
    }

    @Composable
    override fun TopBarActions() {
        IconButton(onClick = {
            mainActivity.launchImportKey()
        }) {
            Icon(Icons.Default.SaveAlt, contentDescription = getString(R.string.action_import_private_key))
        }
        IconButton(onClick = {
            PrivateKeyGenerate().show(getSupportFragmentManager(), "generate-key")
            refreshList()
        }) {
            Icon(Icons.Default.Add, contentDescription = getString(R.string.action_generate_private_key))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Overlay() {
        val file = selectedFile ?: return

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(getString(R.string.dialog_key_delete)) },
                text = { Text("${getString(R.string.dialog_key_delete_msg)} $file") },
                confirmButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                        FsUtils.deleteFile(file)
                        FsUtils.deleteFile(PrivateKeyUtils.getPublicKey(file))
                        showDeleteConfirm = false
                        selectedFile = null
                        refreshList()
                    }) { Text(getString(R.string.label_delete)) }
                },
                dismissButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = { showDeleteConfirm = false }) {
                        Text(getString(R.string.label_cancel))
                    }
                }
            )
            return
        }

        ModalBottomSheet(
            onDismissRequest = { selectedFile = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            ActionRow(getString(R.string.action_mode_rename_key)) {
                val args = android.os.Bundle()
                args.putString(RenameKeyDialog.FROM_PATH, file.absolutePath)
                selectedFile = null
                val dialog = RenameKeyDialog()
                dialog.arguments = args
                dialog.show(getSupportFragmentManager(), "rename-dialog")
            }
            ActionRow(getString(R.string.action_mode_show_private_key)) {
                val intent = Intent(this@PrivateKeyManageActivity, ViewFileActivity::class.java)
                intent.putExtra(ViewFileActivity.TAG_FILE_NAME, file.absolutePath)
                intent.putExtra(ViewFileActivity.TAG_MODE, ViewFileActivity.TAG_MODE_SSH_KEY)
                selectedFile = null
                startActivity(intent)
            }
            ActionRow(getString(R.string.action_mode_show_public_key)) {
                val intent = Intent(this@PrivateKeyManageActivity, ViewFileActivity::class.java)
                intent.putExtra(
                    ViewFileActivity.TAG_FILE_NAME,
                    PrivateKeyUtils.getPublicKeyEnsure(file).absolutePath
                )
                intent.putExtra(ViewFileActivity.TAG_MODE, ViewFileActivity.TAG_MODE_SSH_KEY)
                selectedFile = null
                startActivity(intent)
            }
            ActionRow(getString(R.string.action_mode_edit_key_password)) {
                val args = android.os.Bundle()
                args.putString(EditKeyPasswordDialog.KEY_FILE_EXTRA, file.absolutePath)
                selectedFile = null
                val dialog = EditKeyPasswordDialog()
                dialog.arguments = args
                dialog.show(getSupportFragmentManager(), "rename-dialog")
            }
            ActionRow(getString(R.string.label_delete)) {
                showDeleteConfirm = true
            }
        }
    }

    @Composable
    private fun ActionRow(label: String, onClick: () -> Unit) {
        ListItem(
            headlineContent = { Text(label) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        )
    }
}
