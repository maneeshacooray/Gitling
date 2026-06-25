package com.manichord.mgit.viewfile

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.eclipse.jgit.blame.BlameResult
import java.util.Date

data class BlameGroupState(
    val shortHash: String,
    val authorName: String,
    val date: Date,
    val startLine: Int,
    val lines: List<String>
)

sealed class BlameState {
    data object Loading : BlameState()
    data class Loaded(val groups: List<BlameGroupState>) : BlameState()
    data class Error(val message: String) : BlameState()
}

/** Walks each blamed line, grouping consecutive lines attributed to the same commit. */
fun BlameResult.toGroups(): List<BlameGroupState> {
    val contents = resultContents ?: return emptyList()
    val groups = mutableListOf<BlameGroupState>()
    var currentCommitName: String? = null
    var currentStartLine = 0
    var currentLines = mutableListOf<String>()

    fun flush() {
        val commit = getSourceCommit(currentStartLine) ?: return
        groups.add(
            BlameGroupState(
                shortHash = commit.name.take(7),
                authorName = commit.authorIdent.name,
                date = commit.authorIdent.`when`,
                startLine = currentStartLine + 1,
                lines = currentLines
            )
        )
    }

    for (i in 0 until contents.size()) {
        val commitName = getSourceCommit(i)?.name
        if (commitName != currentCommitName) {
            if (currentLines.isNotEmpty()) flush()
            currentCommitName = commitName
            currentStartLine = i
            currentLines = mutableListOf()
        }
        currentLines.add(contents.getString(i))
    }
    if (currentLines.isNotEmpty()) flush()

    return groups
}

@Composable
fun BlameContent(state: BlameState, modifier: Modifier = Modifier) {
    when (state) {
        is BlameState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is BlameState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        is BlameState.Loaded -> {
            if (state.groups.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No blame information", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = modifier.fillMaxSize()) {
                    items(state.groups, key = { it.startLine }) { group ->
                        BlameGroupRow(group)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BlameGroupRow(group: BlameGroupState) {
    val context = LocalContext.current
    val dateFormatter = DateFormat.getDateFormat(context)

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        // Metadata column (rendered once per group, not per line)
        Column(modifier = Modifier.width(96.dp)) {
            Text(
                text = group.shortHash,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = group.authorName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateFormatter.format(group.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Code lines, with line numbers
        Column(modifier = Modifier.weight(1f)) {
            group.lines.forEachIndexed { index, line ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = (group.startLine + index).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
