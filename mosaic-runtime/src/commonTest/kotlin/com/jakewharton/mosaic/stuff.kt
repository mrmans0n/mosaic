package com.jakewharton.mosaic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.jakewharton.mosaic.layout.IntrinsicMeasurable
import com.jakewharton.mosaic.layout.Measurable
import com.jakewharton.mosaic.layout.MeasurePolicy
import com.jakewharton.mosaic.layout.MeasureResult
import com.jakewharton.mosaic.layout.MeasureScope
import com.jakewharton.mosaic.layout.MosaicNode
import com.jakewharton.mosaic.layout.Placeable
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Filler
import com.jakewharton.mosaic.ui.Layout
import com.jakewharton.mosaic.ui.unit.Constraints
import com.jakewharton.mosaic.ui.unit.IntOffset
import com.jakewharton.mosaic.ui.unit.IntSize
import com.jakewharton.mosaic.ui.unit.constrain
import kotlin.math.max

const val s = " "

const val TestChar = 'X'

fun <T> snapshotStateListOf(vararg values: T): SnapshotStateList<T> {
	return SnapshotStateList<T>().apply { addAll(values) }
}

internal val MosaicNode.size: IntSize
	get() = IntSize(width, height)

internal val MosaicNode.position: IntOffset
	get() = IntOffset(x, y)

internal fun mosaicNodesWithMeasureAndPlace(content: @Composable () -> Unit): MosaicNode {
	return mosaicNodes(content).apply {
		measureAndPlace()
	}
}

@Composable
inline fun TestFiller(modifier: Modifier = Modifier) {
	Filler(TestChar, modifier = modifier)
}

@Composable
fun Container(
	modifier: Modifier = Modifier,
	alignment: Alignment = Alignment.Center,
	expanded: Boolean = false,
	constraints: Constraints = Constraints(),
	width: Int? = null,
	height: Int? = null,
	content: @Composable () -> Unit = {},
) {
	Layout(content, modifier, debugInfo = { "Container()" }) { measurables, incomingConstraints ->
		val containerConstraints = incomingConstraints.constrain(
			Constraints(constraints.value)
				.copy(
					width ?: constraints.minWidth,
					width ?: constraints.maxWidth,
					height ?: constraints.minHeight,
					height ?: constraints.maxHeight,
				),
		)
		val childConstraints = containerConstraints.copy(minWidth = 0, minHeight = 0)
		var placeable: Placeable? = null
		val containerWidth = if ((containerConstraints.hasFixedWidth || expanded) &&
			containerConstraints.hasBoundedWidth
		) {
			containerConstraints.maxWidth
		} else {
			placeable = measurables.firstOrNull()?.measure(childConstraints)
			max(placeable?.width ?: 0, containerConstraints.minWidth)
		}
		val containerHeight = if ((containerConstraints.hasFixedHeight || expanded) &&
			containerConstraints.hasBoundedHeight
		) {
			containerConstraints.maxHeight
		} else {
			if (placeable == null) {
				placeable = measurables.firstOrNull()?.measure(childConstraints)
			}
			max(placeable?.height ?: 0, containerConstraints.minHeight)
		}
		layout(containerWidth, containerHeight) {
			val p = placeable ?: measurables.firstOrNull()?.measure(childConstraints)
			p?.let {
				val position = alignment.align(
					IntSize(it.width, it.height),
					IntSize(containerWidth, containerHeight),
				)
				it.place(position.x, position.y)
			}
		}
	}
}

fun testIntrinsics(
	vararg layouts: @Composable () -> Unit,
	test: ((Int) -> Int, (Int) -> Int, (Int) -> Int, (Int) -> Int) -> Unit,
) {
	layouts.forEach { layout ->
		renderMosaic {
			val measurePolicy = object : MeasurePolicy {
				override fun MeasureScope.measure(
					measurables: List<Measurable>,
					constraints: Constraints,
				): MeasureResult {
					val measurable = measurables.first()
					test(
						{ h -> measurable.minIntrinsicWidth(h) },
						{ w -> measurable.minIntrinsicHeight(w) },
						{ h -> measurable.maxIntrinsicWidth(h) },
						{ w -> measurable.maxIntrinsicHeight(w) },
					)

					return layout(0, 0) {}
				}

				override fun minIntrinsicWidth(
					measurables: List<IntrinsicMeasurable>,
					height: Int,
				) = 0

				override fun minIntrinsicHeight(
					measurables: List<IntrinsicMeasurable>,
					width: Int,
				) = 0

				override fun maxIntrinsicWidth(
					measurables: List<IntrinsicMeasurable>,
					height: Int,
				) = 0

				override fun maxIntrinsicHeight(
					measurables: List<IntrinsicMeasurable>,
					width: Int,
				) = 0
			}
			Layout(
				content = layout,
				measurePolicy = measurePolicy,
			)
		}
	}
}
