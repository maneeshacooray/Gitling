package com.manichord.mgit.repodetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revplot.AbstractPlotRenderer
import org.eclipse.jgit.revplot.PlotCommit
import org.eclipse.jgit.revplot.PlotLane

/** Mirrors AbstractPlotRenderer's own private LANE_WIDTH/LEFT_PAD (not accessible to us). */
private const val LANE_WIDTH_UNITS = 14
private const val LEFT_PAD_UNITS = 2
// Lazygit-style dense lanes: tight enough that several lanes still read as thin terminal-style
// pipes rather than the wide, spaced-out canvas lines this used to draw.
private const val DP_PER_UNIT = 0.55f

private val LaneColors = listOf(
    Color(0xFF4CAF50), // green
    Color(0xFFE91E63), // pink
    Color(0xFF2196F3), // blue
    Color(0xFFFF9800), // orange
    Color(0xFF9C27B0), // purple
    Color(0xFF00BCD4), // cyan
    Color(0xFFFFC107), // amber
)

fun graphWidthUnits(maxLanePosition: Int): Int =
    LEFT_PAD_UNITS + LANE_WIDTH_UNITS * (maxLanePosition + 2)

/**
 * Renders one commit's row of the graph using JGit's own AbstractPlotRenderer (the same
 * graph-layout engine EGit uses) -- it computes all passing/merging/forking lane geometry
 * internally (those fields are package-private to org.eclipse.jgit.revplot, so subclassing
 * is the only way to use them) and calls back into the draw* methods below, which we point
 * at a Compose DrawScope set immediately before each paint() call.
 */
private class ComposePlotRenderer : AbstractPlotRenderer<PlotLane, Color>() {
    lateinit var scope: DrawScope
    var unitPx: Float = 1f
    private var currentNodeColor: Color = LaneColors[0]

    fun paint(commit: PlotCommit<PlotLane>, drawScope: DrawScope, unitPxValue: Float) {
        scope = drawScope
        unitPx = unitPxValue
        currentNodeColor = laneColor(commit.getLane())
        paintCommit(commit, (drawScope.size.height / unitPxValue).toInt())
    }

    override fun laneColor(myLane: PlotLane?): Color {
        val pos = myLane?.position ?: 0
        return LaneColors[((pos % LaneColors.size) + LaneColors.size) % LaneColors.size]
    }

    override fun drawLine(color: Color, x1: Int, y1: Int, x2: Int, y2: Int, width: Int) {
        if (x1 == x2 && y1 == y2) return
        scope.drawLine(
            color = color,
            start = Offset(x1 * unitPx, y1 * unitPx),
            end = Offset(x2 * unitPx, y2 * unitPx),
            strokeWidth = (width * unitPx).coerceAtLeast(1.5f)
        )
    }

    override fun drawCommitDot(x: Int, y: Int, w: Int, h: Int) {
        scope.drawOval(
            color = currentNodeColor,
            topLeft = Offset(x * unitPx, y * unitPx),
            size = Size(w * unitPx, h * unitPx)
        )
    }

    override fun drawBoundaryDot(x: Int, y: Int, w: Int, h: Int) {
        scope.drawOval(
            color = currentNodeColor,
            topLeft = Offset(x * unitPx, y * unitPx),
            size = Size(w * unitPx, h * unitPx),
            style = Stroke(width = 1.5f * unitPx)
        )
    }

    // Decorations (branch/tag chips) and the message are rendered as Compose Text alongside
    // the canvas instead, so these are no-ops.
    override fun drawLabel(x: Int, y: Int, ref: Ref?): Int = 0
    override fun drawText(msg: String?, x: Int, y: Int) {}
}

@Composable
fun CommitGraphCanvas(
    commit: PlotCommit<PlotLane>,
    graphWidthUnits: Int,
    modifier: Modifier = Modifier
) {
    val renderer = remember { ComposePlotRenderer() }
    Canvas(modifier = modifier.width((graphWidthUnits * DP_PER_UNIT).dp).fillMaxHeight()) {
        val unitPx = size.width / graphWidthUnits
        renderer.paint(commit, this, unitPx)
    }
}
