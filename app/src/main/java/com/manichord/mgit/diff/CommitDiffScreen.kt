@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.diff

import androidx.compose.material3.LoadingIndicator
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitDiffScreen(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShareClick, enabled = !isLoading) {
                        Icon(Icons.Default.Share, contentDescription = "Share diff")
                    }
                    IconButton(onClick = onSaveClick, enabled = !isLoading) {
                        Icon(Icons.Default.SaveAlt, contentDescription = "Save diff")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AndroidView(
                factory = { context -> WebView(context).also(onWebViewCreated) },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
