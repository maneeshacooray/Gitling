package com.manichord.mgit.widget

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Compact header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "GITLING",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "↻",
                    style = TextStyle(
                        color = GlanceTheme.colors.secondary,
                        fontSize = 16.sp
                    ),
                    modifier = GlanceModifier
                        .padding(start = 8.dp)
                        .clickable(actionRunCallback<RefreshWidgetAction>())
                )
            }

            // Divider under header
            Spacer(modifier = GlanceModifier.height(5.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlanceTheme.colors.onBackground)
            ) {}

            Spacer(modifier = GlanceModifier.height(4.dp))

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
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
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

            // Branch name — right-aligned
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

        // Last commit message, indented to align under the repo name
        if (!repo.lastCommitMsg.isNullOrBlank()) {
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
