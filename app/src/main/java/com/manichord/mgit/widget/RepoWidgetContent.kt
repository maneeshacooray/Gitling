package com.manichord.mgit.widget

import androidx.glance.color.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import me.sheimi.sgit.R

@Composable
@GlanceComposable
fun RepoWidgetContent(repos: List<RepoWidgetEntry>) {
    GlanceTheme {
        Scaffold(
            titleBar = {
                TitleBar(
                    startIcon = ImageProvider(R.drawable.ic_launcher_foreground),
                    title = "Gitling",
                    actions = {
                        // Padding must go on a wrapping Box, not CircleIconButton's own
                        // modifier -- passed directly, it shifts only the icon glyph inside
                        // the button, not the visible circular background.
                        Box(modifier = GlanceModifier.padding(end = 8.dp)) {
                            CircleIconButton(
                                imageProvider = ImageProvider(R.drawable.ic_sync),
                                contentDescription = "Refresh",
                                onClick = actionRunCallback<RefreshWidgetAction>()
                            )
                        }
                    }
                )
            }
        ) {
            if (repos.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No repos cloned yet",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(repos, itemId = { it.id.toLong() }) { repo ->
                        RepoWidgetRow(repo)
                    }
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun RepoWidgetRow(repo: RepoWidgetEntry) {
    // Each repo is its own rounded tile with a colored initial, matching the repo cards in the app
    Box(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(20.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(
                    actionRunCallback<OpenRepoWidgetAction>(
                        actionParametersOf(RepoIdParamKey to repo.id)
                    )
                ),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            RepoWidgetBadge(repo.displayName)

            Spacer(modifier = GlanceModifier.width(10.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = repo.displayName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )

                // Branch + last commit on one muted line, always shown
                val subtitle = listOfNotNull(
                    repo.branchName?.takeIf { it.isNotBlank() },
                    repo.lastCommitMsg?.takeIf { it.isNotBlank() }
                ).joinToString("  ")

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Status dot: tertiary when clean, error when there are uncommitted changes
            Box(
                modifier = GlanceModifier
                    .size(10.dp)
                    .cornerRadius(5.dp)
                    .background(
                        if (repo.isDirty) GlanceTheme.colors.error
                        else GlanceTheme.colors.tertiary
                    )
            ) {}
        }
    }
}

/** Hues spread around the color wheel, as on the app's repo cards (RepoCard's badge). */
private val widgetBadgeHues = listOf(140f, 210f, 30f, 280f, 175f, 350f, 55f, 245f)

/** The repo's initial on a colored rounded square. Same hue per repo as in the app; Glance can't
 * draw the app's Expressive shapes, so it's a rounded square here. */
@Composable
@GlanceComposable
private fun RepoWidgetBadge(name: String) {
    val key = name.lowercase().hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
    val hue = widgetBadgeHues[key % widgetBadgeHues.size]
    val container = ColorProvider(day = Color.hsl(hue, 0.60f, 0.88f), night = Color.hsl(hue, 0.35f, 0.30f))
    val content = ColorProvider(day = Color.hsl(hue, 0.65f, 0.25f), night = Color.hsl(hue, 0.70f, 0.88f))
    val initial = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#"
    Box(
        modifier = GlanceModifier.size(32.dp).cornerRadius(10.dp).background(container),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = TextStyle(color = content, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        )
    }
}
