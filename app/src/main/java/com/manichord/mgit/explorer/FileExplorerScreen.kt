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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

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
    modifier: Modifier = Modifier
) {
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
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
