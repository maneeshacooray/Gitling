package com.manichord.mgit.repolist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.manichord.mgit.clone.CloneViewModel
import me.sheimi.sgit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneView(
    viewModel: CloneViewModel,
    onCloneClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // remoteUrl is a plain String in the VM for now, we sync it manually
    var remoteUrl by remember { mutableStateOf(viewModel.remoteUrl) }
    val localRepoName by viewModel.localRepoName.observeAsState("")
    val initLocal by viewModel.initLocal.observeAsState(false)
    val cloneRecursively by remember { mutableStateOf(viewModel.cloneRecursively) }

    val remoteUrlError by viewModel.remoteUrlError.observeAsState()
    val localRepoNameError by viewModel.localRepoNameError.observeAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp) // Extra padding for bottom sheet
    ) {
        Text(
            text = if (initLocal) stringResource(id = R.string.dialog_clone_neutral_label) else stringResource(id = R.string.title_clone_repo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!initLocal) {
            OutlinedTextField(
                value = remoteUrl,
                onValueChange = {
                    remoteUrl = it
                    viewModel.remoteUrl = it  // sync to VM
                },
                label = { Text(stringResource(id = R.string.label_remote_url)) },
                placeholder = { Text("https://github.com/user/repo.git") },
                modifier = Modifier.fillMaxWidth(),
                isError = !remoteUrlError.isNullOrEmpty(),
                supportingText = {
                    if (!remoteUrlError.isNullOrEmpty()) {
                        Text(text = remoteUrlError!!)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = localRepoName,
            onValueChange = { viewModel.localRepoName.value = it },
            label = { Text(stringResource(id = R.string.dialog_clone_local_path_hint)) },
            modifier = Modifier.fillMaxWidth(),
            isError = !localRepoNameError.isNullOrEmpty(),
            supportingText = {
                if (!localRepoNameError.isNullOrEmpty()) {
                    Text(text = localRepoNameError!!)
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // New Folder Picker Section
        val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            uri?.let { viewModel.cloneLocation.value = it.toString() }
        }

        val cloneLocation by viewModel.cloneLocation.observeAsState("")

        OutlinedTextField(
            value = cloneLocation,
            onValueChange = { viewModel.cloneLocation.value = it },
            label = { Text("Clone Location") },
            placeholder = { Text("Default storage") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                }
            },
            shape = MaterialTheme.shapes.medium,
            readOnly = true // User should use the browse button
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Options
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = initLocal,
                        onCheckedChange = { viewModel.initLocal.value = it }
                    )
                    Text(
                        text = stringResource(id = R.string.dialog_clone_neutral_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (!initLocal) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = cloneRecursively,
                            onCheckedChange = { viewModel.cloneRecursively = it }
                        )
                        Text(
                            text = stringResource(id = R.string.dialog_clone_recursive),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(stringResource(id = R.string.label_cancel))
            }
            Button(
                onClick = onCloneClick,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(
                    if (initLocal) stringResource(id = R.string.label_init)
                    else stringResource(id = R.string.label_clone)
                )
            }
        }
    }
}
