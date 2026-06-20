package com.manichord.mgit.repodetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.eclipse.jgit.revplot.PlotCommit
import org.eclipse.jgit.revplot.PlotLane
import org.eclipse.jgit.revwalk.RevCommit

sealed class CommitRowState {
    data object Loading : CommitRowState()
    data class Item(val position: Int, val commit: RevCommit, val selected: Boolean) : CommitRowState()
}

@Composable
fun CommitsListContent(
    rows: List<CommitRowState>,
    onItemClick: (Int) -> Unit,
    onItemLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showBranchScopeToggle: Boolean = false,
    allBranches: Boolean = false,
    onAllBranchesChange: (Boolean) -> Unit = {}
) {
    val lastItemIndex = rows.indexOfLast { it is CommitRowState.Item }
    val maxLanePosition = if (rows.isEmpty()) 0 else rows.maxOf { row ->
        (row as? CommitRowState.Item)?.commit?.let { (it as? PlotCommit<*>)?.getLane()?.position } ?: 0
    }
    val graphWidthUnits = graphWidthUnits(maxLanePosition)

    Column(modifier = modifier.fillMaxSize()) {
        if (showBranchScopeToggle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                FilterChip(
                    selected = !allBranches,
                    onClick = { onAllBranchesChange(false) },
                    label = { Text("Current branch") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = allBranches,
                    onClick = { onAllBranchesChange(true) },
                    label = { Text("All branches") }
                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(
                rows,
                key = { index, row -> if (row is CommitRowState.Item) row.commit.name else "loading-$index" }
            ) { index, row ->
                when (row) {
                    is CommitRowState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is CommitRowState.Item -> {
                        CommitRow(
                            commit = row.commit,
                            selected = row.selected,
                            isFirst = index == 0,
                            isLast = index == lastItemIndex,
                            graphWidthUnits = graphWidthUnits,
                            onClick = { onItemClick(row.position) },
                            onLongClick = { onItemLongClick(row.position) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A reflog-style graph node: a vertical lane line running the full height of the row, with a
 * ring-and-dot marker at the row's vertical center for the commit itself. [isFirst]/[isLast]
 * trim the line tail so it doesn't dangle past the first/last commit in the list. Used when
 * the commit list has no real lane/topology data (e.g. the per-file commit history).
 */
@Composable
private fun CommitGraphNode(isFirst: Boolean, isLast: Boolean, modifier: Modifier = Modifier) {
    val nodeColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.width(18.dp).fillMaxHeight()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = 4.dp.toPx()
        val innerRadius = 1.8.dp.toPx()
        val strokeWidth = 1.5.dp.toPx()
        val gap = outerRadius + 1.5.dp.toPx()

        if (!isFirst) {
            drawLine(
                color = nodeColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, centerY - gap),
                strokeWidth = strokeWidth
            )
        }
        if (!isLast) {
            drawLine(
                color = nodeColor,
                start = Offset(centerX, centerY + gap),
                end = Offset(centerX, size.height),
                strokeWidth = strokeWidth
            )
        }
        drawCircle(
            color = nodeColor,
            radius = outerRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = nodeColor,
            radius = innerRadius,
            center = Offset(centerX, centerY)
        )
    }
}

/** Lazygit-style: one compact line per commit -- graph, short hash, message, no avatar/author/date. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommitRow(
    commit: RevCommit,
    selected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    graphWidthUnits: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .height(IntrinsicSize.Min)
    ) {
        @Suppress("UNCHECKED_CAST")
        val plotCommit = commit as? PlotCommit<PlotLane>
        if (plotCommit != null) {
            CommitGraphCanvas(commit = plotCommit, graphWidthUnits = graphWidthUnits)
        } else {
            CommitGraphNode(isFirst = isFirst, isLast = isLast)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = commit.name.take(7),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = commit.shortMessage,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
