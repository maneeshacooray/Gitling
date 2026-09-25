package com.manichord.mgit.explorer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

import me.sheimi.sgit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    title: String,
    currentPath: String,
    files: List<File>,
    showUpRow: Boolean,
    onUpClick: () -> Unit,
    onBackClick: () -> Unit,
    onItemClick: (File) -> Unit,
    onItemLongClick: (File) -> Unit = {},
    selectedFile: File? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = actions
            )
        }
    ) { paddingValues ->
        FileListContent(
            currentPath = currentPath,
            files = files,
            showUpRow = showUpRow,
            onUpClick = onUpClick,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
            selectedFile = selectedFile,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

/**
 * The path breadcrumb + file/folder list, with no Scaffold/TopAppBar of its own -- reusable
 * both as [FileExplorerScreen]'s body and as a tab's content within another screen's Scaffold
 * (e.g. RepoDetailActivity's Files tab) where an extra TopAppBar would duplicate the one
 * already there.
 */
@Composable
fun FileListContent(
    currentPath: String,
    files: List<File>,
    showUpRow: Boolean,
    onUpClick: () -> Unit,
    onItemClick: (File) -> Unit,
    onItemLongClick: (File) -> Unit = {},
    selectedFile: File? = null,
    modifier: Modifier = Modifier,
    /** When non-null, shows each row's result of this (typically a path relative to the repo
     * root) as a subtitle beneath its name -- used for flat, cross-directory results (e.g. a
     * recursive filename search) where the bare name alone wouldn't disambiguate same-named
     * files in different folders. */
    displayPath: ((File) -> String)? = null,
    onPathSubmit: ((String) -> Unit)? = null
) {
    var showPathDialog by remember { mutableStateOf(false) }
    var pathDraft by remember { mutableStateOf(currentPath) }

    if (showPathDialog) {
        AlertDialog(
            onDismissRequest = { showPathDialog = false },
            title = { Text(stringResource(R.string.dialog_path_title)) },
            text = {
                OutlinedTextField(
                    value = pathDraft,
                    onValueChange = { pathDraft = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.dialog_path_label)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPathDialog = false
                    onPathSubmit?.invoke(pathDraft)
                }) {
                    Text(stringResource(R.string.label_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPathDialog = false }) {
                    Text(stringResource(R.string.label_cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = currentPath,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (onPathSubmit != null) {
                        pathDraft = currentPath
                        showPathDialog = true
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (showUpRow) {
                item {
                    FileRow(name = "..", isDirectory = true, onClick = onUpClick)
                }
            }
            items(files, key = { it.absolutePath }) { file ->
                FileRow(
                    name = file.name,
                    path = displayPath?.invoke(file),
                    isDirectory = file.isDirectory,
                    selected = file == selectedFile,
                    onClick = { onItemClick(file) },
                    onLongClick = { onItemLongClick(file) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    name: String,
    isDirectory: Boolean,
    path: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (path != null) {
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
