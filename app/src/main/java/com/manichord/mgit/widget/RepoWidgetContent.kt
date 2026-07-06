package com.manichord.mgit.widget

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.GlanceComposable
import androidx.compose.runtime.Composable

@Composable
@GlanceComposable
fun RepoWidgetContent(repos: List<RepoWidgetEntry>) {
    // Hide commit message and branch when the widget is too short to show them legibly.
    // A 1-cell-high widget is ~58dp; two lines per repo need at least ~80dp total height.
    val compact = LocalSize.current.height < 80.dp
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Refresh button — top right, no header text
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "↻",
                    style = TextStyle(
                        color = GlanceTheme.colors.secondary,
                        fontSize = 14.sp
                    ),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshWidgetAction>())
                )
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            if (repos.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No repos cloned yet",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(repos, itemId = { it.id.toLong() }) { repo ->
                        RepoWidgetRow(repo, compact = compact)
                    }
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun RepoWidgetRow(repo: RepoWidgetEntry, compact: Boolean) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 4.dp else 7.dp)
            .clickable(
                actionRunCallback<OpenRepoWidgetAction>(
                    actionParametersOf(RepoIdParamKey to repo.id)
                )
            )
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Status circle — large enough to see at a glance
            Box(
                modifier = GlanceModifier
                    .size(12.dp)
                    .cornerRadius(6.dp)
                    .background(
                        if (repo.isDirty) GlanceTheme.colors.error
                        else GlanceTheme.colors.primary
                    )
            ) {}
            Spacer(modifier = GlanceModifier.width(8.dp))

            // Repo name
            Text(
                text = repo.displayName,
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )

            // Branch name — right-aligned; hidden in compact mode to save horizontal space
            if (!compact) {
                repo.branchName?.let { branch ->
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = branch,
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        // Last commit message — hidden in compact mode (not enough vertical space)
        if (!compact && !repo.lastCommitMsg.isNullOrBlank()) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Spacer(modifier = GlanceModifier.width(20.dp))
                Text(
                    text = repo.lastCommitMsg,
                    style = TextStyle(
                        color = GlanceTheme.colors.secondary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}
