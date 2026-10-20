@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.importrepo

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import android.os.Build
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.manichord.mgit.ui.components.rememberTextFieldStateFor
import com.manichord.mgit.ui.components.onUserTextChange
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo

/** The dialogs of the import flow, driven by [ImportRepoViewModel.state]. [onOpenRepo] opens
 * the imported repo from the result dialog. */
@Composable
fun ImportRepoDialogs(viewModel: ImportRepoViewModel, onOpenRepo: (Repo) -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        viewModel.onSourcePicked(it, isZip = false)
    }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.onSourcePicked(it, isZip = true)
    }

    when (val s = state) {
        ImportRepoViewModel.State.Hidden -> Unit
        ImportRepoViewModel.State.Intro -> IntroDialog(
            onChooseFolder = { folderPicker.launch(null) },
            // Some providers report ZIP files as application/x-zip-compressed or octet-stream
            onChooseZip = {
                zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed",
                    "application/x-zip", "application/octet-stream"))
            },
            onDismiss = viewModel::dismiss
        )
        is ImportRepoViewModel.State.Naming -> NameDialog(s, viewModel)
        is ImportRepoViewModel.State.Importing -> ProgressDialog(s, onCancel = viewModel::cancelImport)
        is ImportRepoViewModel.State.Done -> DoneDialog(
            s,
            onOpen = { viewModel.dismiss(); onOpenRepo(s.repo) },
            onDismiss = viewModel::dismiss
        )
        is ImportRepoViewModel.State.Failed -> {
            if (s.problem == RepoImporter.Problem.CANCELLED) {
                LaunchedEffect(s) {
                    Toast.makeText(context, R.string.toast_import_cancelled, Toast.LENGTH_LONG).show()
                    viewModel.dismiss()
                }
            } else {
                FailedDialog(s, onDismiss = viewModel::dismiss)
            }
        }
    }
}

/** What the user should know before copying a repo in, kept to a few short points. */
@Composable
private fun IntroDialog(onChooseFolder: () -> Unit, onChooseZip: () -> Unit, onDismiss: () -> Unit) {
    val points = buildList {
        add(R.string.dialog_import_repo_risk_copy)
        // Android 10 and older still let the picker open other apps' Android/data folders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(R.string.dialog_import_repo_risk_private)
        add(R.string.dialog_import_repo_risk_modes)
        add(R.string.dialog_import_repo_risk_credentials)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_import_repo_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.dialog_import_repo_intro))
                Spacer(Modifier.height(12.dp))
                points.forEach { Bullet(stringResource(it)) }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onChooseZip) { Text(stringResource(R.string.label_import_zip)) }
                TextButton(onClick = onChooseFolder) { Text(stringResource(R.string.label_import_folder)) }
            }
        },
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.label_cancel)) }
        }
    )
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NameDialog(state: ImportRepoViewModel.State.Naming, viewModel: ImportRepoViewModel) {
    val nameState = rememberTextFieldStateFor(state.name)
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text(stringResource(R.string.dialog_import_name_title)) },
        text = {
            OutlinedTextField(
                state = nameState,
                inputTransformation = onUserTextChange { viewModel.onNameChanged(it) },
                label = { Text(stringResource(R.string.dialog_import_name_hint)) },
                isError = state.error != null,
                supportingText = {
                    Text(state.error ?: stringResource(R.string.dialog_import_name_destination,
                        viewModel.destinationFor(state.name).absolutePath))
                },
                lineLimits = TextFieldLineLimits.SingleLine
            )
        },
        confirmButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = viewModel::startImport, enabled = state.error == null) {
                Text(stringResource(R.string.label_import))
            }
        },
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = viewModel::dismiss) { Text(stringResource(R.string.label_cancel)) }
        }
    )
}

/** Not dismissable by tapping outside or Back: only Cancel stops an import, so it can't be left
 * running unnoticed. */
@Composable
private fun ProgressDialog(state: ImportRepoViewModel.State.Importing, onCancel: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.dialog_importing_title, state.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.total > 0) {
                    LinearWavyProgressIndicator(
                        progress = { (state.done.toFloat() / state.total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.importing_progress,
                        Formatter.formatShortFileSize(context, state.done),
                        Formatter.formatShortFileSize(context, state.total)))
                } else {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.importing_progress_unknown,
                        Formatter.formatShortFileSize(context, state.done)))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onCancel) { Text(stringResource(R.string.label_cancel)) }
        }
    )
}

/** Says what was done, including anything Gitling changed in the copy. */
@Composable
private fun DoneDialog(state: ImportRepoViewModel.State.Done, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_import_done_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.dialog_import_done_message, state.result.dir.name,
                    Formatter.formatShortFileSize(context, state.result.bytes)))
                if (state.result.turnedOffFileModeOrSymlinks) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.dialog_import_done_modes))
                }
                if (state.result.skippedOutsideRepo > 0) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.dialog_import_done_skipped))
                }
            }
        },
        confirmButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onOpen) { Text(stringResource(R.string.label_open_repo)) }
        },
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.label_done)) }
        }
    )
}

@Composable
private fun FailedDialog(state: ImportRepoViewModel.State.Failed, onDismiss: () -> Unit) {
    val message = when (state.problem) {
        RepoImporter.Problem.NOT_A_REPO -> stringResource(R.string.error_import_not_a_repo)
        RepoImporter.Problem.PICKED_GIT_DIR -> stringResource(R.string.error_import_picked_git_dir)
        RepoImporter.Problem.BARE_REPO -> stringResource(R.string.error_import_bare)
        RepoImporter.Problem.GIT_FILE -> stringResource(R.string.error_import_git_file)
        RepoImporter.Problem.ZIP_NO_REPO -> stringResource(R.string.error_import_zip_no_repo)
        RepoImporter.Problem.ZIP_MULTIPLE_REPOS -> stringResource(R.string.error_import_zip_multiple)
        RepoImporter.Problem.ZIP_INVALID -> stringResource(R.string.error_import_zip_invalid)
        RepoImporter.Problem.NO_SPACE -> stringResource(R.string.error_import_no_space)
        RepoImporter.Problem.READ_FAILED, RepoImporter.Problem.CANCELLED ->
            stringResource(R.string.error_import_read_failed, state.detail ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_import_failed_title)) },
        text = { Text(message + "\n\n" + stringResource(R.string.dialog_import_failed_nothing)) },
        confirmButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.label_ok)) }
        }
    )
}
