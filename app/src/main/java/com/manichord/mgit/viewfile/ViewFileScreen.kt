package com.manichord.mgit.viewfile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

const val TAG_MODE_SSH_KEY: Short = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewFileScreen(
    title: String,
    hasCommitsTab: Boolean,
    hasBlameTab: Boolean,
    activityMode: Short,
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onChooseLanguageClick: () -> Unit,
    onCopyAllClick: () -> Unit,
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    fileContent: @Composable () -> Unit,
    commitsContent: @Composable () -> Unit,
    blameContent: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            if (searchActive && currentTab == 1) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search commits") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            onSearchActiveChange(false)
                            onSearchQueryChange("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    }
                )
            } else {
                var showOverflow by remember { mutableStateOf(false) }
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (currentTab == 0) {
                            if (activityMode == TAG_MODE_SSH_KEY) {
                                IconButton(onClick = onCopyAllClick) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy all")
                                }
                            } else {
                                IconButton(onClick = onEditClick) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit in other app")
                                }
                                IconButton(onClick = { showOverflow = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null) },
                                        text = { Text("Choose language") },
                                        onClick = {
                                            showOverflow = false
                                            onChooseLanguageClick()
                                        }
                                    )
                                }
                            }
                        }
                        if (hasCommitsTab && currentTab == 1) {
                            IconButton(onClick = { onSearchActiveChange(true) }) {
                                Icon(Icons.Default.Search, contentDescription = "Search commits")
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (hasCommitsTab || hasBlameTab) {
                TabRow(selectedTabIndex = currentTab) {
                    Tab(selected = currentTab == 0, onClick = { onTabSelected(0) }, text = { Text("File") })
                    if (hasCommitsTab) {
                        Tab(selected = currentTab == 1, onClick = { onTabSelected(1) }, text = { Text("Commits") })
                    }
                    if (hasBlameTab) {
                        Tab(selected = currentTab == 2, onClick = { onTabSelected(2) }, text = { Text("Blame") })
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    0 -> fileContent()
                    1 -> commitsContent()
                    else -> blameContent()
                }
            }
        }
    }
}
