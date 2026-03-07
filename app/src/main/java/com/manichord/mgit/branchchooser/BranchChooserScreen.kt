package com.manichord.mgit.branchchooser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchChooserScreen(
    viewModel: BranchChooserViewModel,
    onBackClick: () -> Unit,
    onBranchClick: (String) -> Unit,
    onRenameClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    val branches by viewModel.branches.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val currentFilter by viewModel.filter.observeAsState(BranchChooserViewModel.BranchFilter.ALL)

    var showOptionsSheet by remember { mutableStateOf<BranchChooserViewModel.BranchItem?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dialog_choose_branch_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BranchChooserViewModel.BranchFilter.values().forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.name.lowercase().capitalize()) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && branches.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(branches) { item ->
                            BranchListItem(
                                item = item,
                                onClick = { onBranchClick(item.name) },
                                onLongClick = { showOptionsSheet = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOptionsSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = null },
            sheetState = sheetState
        ) {
            BranchOptionsContent(
                item = showOptionsSheet!!,
                onRename = {
                    onRenameClick(it.name)
                    showOptionsSheet = null
                },
                onDelete = {
                    onDeleteClick(it.name)
                    showOptionsSheet = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BranchListItem(
    item: BranchChooserViewModel.BranchItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val icon = when (item.type) {
        Repo.COMMIT_TYPE_TAG -> Icons.Default.Tag
        else -> Icons.Default.AccountTree
    }

    ListItem(
        headlineContent = { Text(item.displayName, fontWeight = FontWeight.Medium) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    )
}

@Composable
fun BranchOptionsContent(
    item: BranchChooserViewModel.BranchItem,
    onRename: (BranchChooserViewModel.BranchItem) -> Unit,
    onDelete: (BranchChooserViewModel.BranchItem) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            item.displayName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )
        Divider()
        ListItem(
            headlineContent = { Text("Rename") },
            leadingContent = { Icon(Icons.Default.Edit, null) },
            modifier = Modifier.clickable { onRename(item) }
        )
        ListItem(
            headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            modifier = Modifier.clickable { onDelete(item) }
        )
    }
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
