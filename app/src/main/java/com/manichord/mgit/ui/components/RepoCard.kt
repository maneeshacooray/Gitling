@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.ui.components

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.sheimi.sgit.database.models.Repo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepoCard(
    repo: Repo,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isProcessing = !repo.getRepoStatus().isNullOrEmpty()

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = if (isProcessing) ({}) else onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RepoBadge(name = repo.getDiaplayName() ?: "?", dimmed = isProcessing)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repo.getDiaplayName() ?: "Unknown Repository",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = repo.getRemoteURL() ?: "Local-only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (repo.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (isProcessing) {
                    IconButton(onClick = onCancelClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Operation",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(12.dp))
                val status = repo.getRepoStatus()
                val percent = statusPercent(status)
                if (percent != null) {
                    // A known percentage (clone, fetch, pull report one): show how far along it is
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress by animateFloatAsState(percent / 100f, label = "repo progress")
                    LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LoadingIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Last Commit Info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = repo.getLastCommitMsg() ?: "No commits yet",
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = repo.getLastCommitter() ?: "—",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = repo.getLastCommitDate()?.toString() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** The "(35%)" at the end of a task's status text (see CloneTask's progress monitor), if any. */
private fun statusPercent(status: String): Int? =
    Regex("""\((\d{1,3})%\)\s*$""").find(status)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100)

/** Hues spread around the color wheel so neighbouring repos rarely look alike. */
private val badgeHues = listOf(140f, 210f, 30f, 280f, 175f, 350f, 55f, 245f)

private val badgeShapes = listOf(
    MaterialShapes.Cookie6Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny,
    MaterialShapes.Pentagon, MaterialShapes.Gem, MaterialShapes.SoftBurst,
    MaterialShapes.Flower, MaterialShapes.PuffyDiamond, MaterialShapes.Cookie4Sided, MaterialShapes.Arch
)

/**
 * The repo's initial in an Expressive shape. Shape and color come from the name, so a repo looks
 * the same every time and repos are easy to tell apart. Colors are the theme's container colors,
 * so it follows light/dark mode and wallpaper colors.
 */
@Composable
private fun RepoBadge(name: String, dimmed: Boolean) {
    val key = name.lowercase().hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
    val scheme = MaterialTheme.colorScheme
    // The theme's container colors are all close in hue with the brand palette, so badges use
    // their own set of distinct hues, tuned for light or dark surfaces
    val dark = scheme.surface.luminance() < 0.5f
    val hue = badgeHues[key % badgeHues.size]
    val (container, content) = when {
        dimmed -> scheme.surfaceVariant to scheme.outline
        dark -> Color.hsl(hue, 0.35f, 0.30f) to Color.hsl(hue, 0.70f, 0.88f)
        else -> Color.hsl(hue, 0.60f, 0.88f) to Color.hsl(hue, 0.65f, 0.25f)
    }
    val initial = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(badgeShapes[(key / badgeHues.size) % badgeShapes.size].toShape())
            .background(container)
    ) {
        Text(initial, style = MaterialTheme.typography.titleMediumEmphasized, color = content)
    }
}
