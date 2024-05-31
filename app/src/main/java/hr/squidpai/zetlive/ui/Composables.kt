package hr.squidpai.zetlive.ui

import androidx.collection.IntList
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.squidpai.zetlive.orLoading
import kotlin.math.floor
import kotlin.math.max

/**
 * [Material Design standard icon button](https://m3.material.io/components/icon-button/overview)
 *
 * Icon buttons help people take supplementary actions with a single tap.
 * They’re used when a compact button is required, such as in a toolbar or image list.
 *
 * [Standard icon button image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-button.png)
 *
 * @param icon [ImageVector] to draw inside
 * @param contentDescription text used by accessibility services to describe what this icon represents.
 * This should always be provided unless this icon is used for decorative purposes, and
 * does not represent a meaningful action that a user can take.
 * This text should be localized, such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * button in different states. See [IconButtonDefaults.iconButtonColors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this icon button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behaviour of this icon button in different states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconButton(
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  onClick: () -> Unit,
) = TooltipBox(
  positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
  tooltip = {
    PlainTooltip { Text(contentDescription) }
  },
  state = rememberTooltipState(),
) {
  IconButton(onClick, modifier, enabled, colors, interactionSource) {
    Icon(icon, contentDescription)
  }
}

/**
 * Draws a [CircularProgressIndicator] wrapped in the center of
 * a [Box] that takes up as much space as possible, via [Modifier].[fillMaxSize].
 *
 * @param modifier the modifier to be applied to the Box
 */
@Composable
fun CircularLoadingBox(modifier: Modifier = Modifier) =
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }

/**
 * Used to select the direction of a route.
 *
 * @param commonHeadsign the labels to be displayed as the directions' headsign
 * @param direction index representing the selected direction (0 is the first direction
 * and 1 is the opposite direction)
 * @param setDirection the callback that is triggered when the direction is updated by
 * this function
 * @param modifier the modifier to be applied to this layout
 */
@Composable
fun ColumnScope.DirectionRow(
  commonHeadsign: Pair<String, String>?,
  direction: Int,
  setDirection: (Int) -> Unit,
  isRoundRoute: Boolean,
  modifier: Modifier = Modifier,
) = Row(modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
  val firstSign = commonHeadsign?.first.orLoading()

  if (isRoundRoute) {
    Text(firstSign, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Box(Modifier.minimumInteractiveComponentSize()) {
      Icon(Symbols._360, null, tint = MaterialTheme.colorScheme.secondary)
    }
    Text(firstSign, maxLines = 1, overflow = TextOverflow.Ellipsis)
  } else {
    // putting an else block instead of a return in the if block because an exception is thrown from Compose otherwise

    val secondSign = commonHeadsign?.second.orLoading()

    val leftSign: String
    val rightSign: String

    if (direction == 1) {
      leftSign = firstSign
      rightSign = secondSign
    } else {
      leftSign = secondSign
      rightSign = firstSign
    }

    Text(
      leftSign,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.End,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    IconButton(
      Symbols.SwapHorizontal,
      contentDescription = "Zamijeni smjer",
      colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    ) { setDirection(1 - direction) }
    Text(
      rightSign,
      modifier = Modifier.weight(1f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Displays a track representing how far a route has reached in its trip.
 *
 * The whole part of [value] is the index of the last reached stop, and the
 * fractional part of [value] is how close the route is to the next stop.
 *
 * The slider is made out of [stopCount] points connected by a track.
 * The part of the track that is before or at [value] is colored
 * [passedTrackColor], and the part of the track that is after [value] is
 * colored [notPassedTrackColor].
 * Points which are before [value] are colored [passedStopColor], the point
 * that is right after [value] is colored [nextStopColor], and points after
 * that are colored [notPassedStopColor].
 * The track is of width [trackWidth], and points have a radius of [pointRadius],
 * except for the point right after [value] which has a radius of [nextPointRadius].
 *
 * If [value] < 0, then the first point is the next point, and if [value] >= [stopCount],
 * then there is no next point.
 *
 * The distance between each point is proportional to its weight value relative to
 * other weight values. If [weights] is `null`, the points are spaced equally apart.
 *
 * Example: `o====o====o=---O----o` (weights = `null`)
 *
 * Example: `o=o=====o=--O--o----o` (weights = `[1f, 5f, 3f, 2f, 4f]`)
 *
 * @param value the value of the track (how far the route has reached in its trip)
 * @param stopCount the number of stops (points)
 * @param weights the relative sizes of track pieces if not `null`
 * @param modifier the modifier to be applied to the layout
 * @param trackWidth the width of the track
 * @param pointRadius the radius of each point (except for the one right after [value])
 * @param nextPointRadius the radius of the point right after [value]
 * @param passedTrackColor the color of the track that is before or at [value]
 * @param notPassedTrackColor the color of the track that is after [value]
 * @param passedStopColor the color of points before [value]
 * @param notPassedStopColor the color of points after [value] (except for the one right after)
 * @param nextStopColor the color of the points right after [value]
 */
@Composable
fun RouteSlider(
  value: Float,
  stopCount: Int,
  modifier: Modifier = Modifier,
  weights: IntArray? = null,
  trackWidth: Dp = 3.dp,
  pointRadius: Dp = 2.dp,
  nextPointRadius: Dp = 4.dp,
  passedTrackColor: Color = MaterialTheme.colorScheme.primary,
  notPassedTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
  passedStopColor: Color = MaterialTheme.colorScheme.contentColorFor(passedTrackColor),
  notPassedStopColor: Color = MaterialTheme.colorScheme.contentColorFor(notPassedTrackColor),
  nextStopColor: Color = MaterialTheme.colorScheme.onSurface,
) = Canvas(
  modifier.defaultMinSize(
    minWidth = nextPointRadius * stopCount,
    minHeight = nextPointRadius,
  )
) {
  if (stopCount <= 0)
    return@Canvas

  val pointRadiusPx = pointRadius.toPx()
  val nextPointRadiusPx = nextPointRadius.toPx()

  if (stopCount == 1) {
    drawCircle(
      color = if (value >= 0f) passedStopColor else nextStopColor,
      radius = if (value >= 0f) pointRadiusPx else nextPointRadiusPx,
    )
    return@Canvas
  }

  val (width, height) = size
  val centerHeight = height / 2f

  val trackWidthPx = trackWidth.toPx()
  val totalTrackLength = width - nextPointRadiusPx * 2

  var passedTrackLength: Float
  val weightRatios: FloatArray?

  if (weights != null) {
    require(weights.size == stopCount - 1) {
      "weights.size != stopCount - 1, weights.size = ${weights.size}, stopCount = $stopCount"
    }

    val weightSum = weights.sum().toFloat()
    weightRatios = FloatArray(stopCount - 1) { weights[it] / weightSum }
    passedTrackLength = 0f //totalTrackLength * value / (stopCount - 1)
    for ((i, ratio) in weightRatios.withIndex()) {
      passedTrackLength += if (i + 1 <= value) ratio
      else if (i < value) value % 1 * ratio
      else break
    }
    passedTrackLength *= totalTrackLength
  } else {
    passedTrackLength = totalTrackLength * value / (stopCount - 1)
    weightRatios = null
  }

  drawLine(
    color = notPassedTrackColor,
    start = Offset(nextPointRadiusPx + passedTrackLength, centerHeight),
    end = Offset(nextPointRadiusPx + totalTrackLength, centerHeight),
    strokeWidth = trackWidthPx,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = passedTrackColor,
    start = Offset(nextPointRadiusPx, centerHeight),
    end = Offset(nextPointRadiusPx + passedTrackLength, centerHeight),
    strokeWidth = trackWidthPx,
    cap = StrokeCap.Round,
  )

  val valueInt = floor(value).toInt()
  var currentPointPosition = nextPointRadiusPx
  val pointSpacing = totalTrackLength / (stopCount - 1)

  for (i in 0..valueInt) {
    drawCircle(
      color = passedStopColor,
      radius = pointRadiusPx,
      center = Offset(currentPointPosition, centerHeight),
      alpha = 0.28f,
    )
    currentPointPosition += totalTrackLength * (weightRatios?.get(i) ?: pointSpacing)
  }
  drawCircle(
    color = nextStopColor,
    radius = nextPointRadiusPx,
    center = Offset(currentPointPosition, centerHeight),
  )
  if (valueInt + 1 >= stopCount - 1)
    return@Canvas
  currentPointPosition += totalTrackLength * (weightRatios?.get(max(valueInt + 1, 0)) ?: pointSpacing)
  for (i in max(valueInt + 2, 1)..<stopCount) {
    drawCircle(
      color = notPassedStopColor,
      radius = pointRadiusPx,
      center = Offset(currentPointPosition, centerHeight),
      alpha = 0.38f,
    )
    if (i == stopCount - 1) continue
    currentPointPosition += totalTrackLength * (weightRatios?.get(i) ?: pointSpacing)
  }
}

/**
 * Displays a [RouteSlider] where `stopCount = `[departures]`.size`
 * and distance between each point is proportional to the time it
 * takes the route to travel between them.
 *
 * @see RouteSlider
 */
@Composable
fun RouteSlider(
  value: Float,
  departures: IntList,
  modifier: Modifier = Modifier,
  trackWidth: Dp = 3.dp,
  pointRadius: Dp = 2.dp,
  nextPointRadius: Dp = 4.dp,
  passedTrackColor: Color = MaterialTheme.colorScheme.primary,
  notPassedTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
  passedStopColor: Color = MaterialTheme.colorScheme.contentColorFor(passedTrackColor),
  notPassedStopColor: Color = MaterialTheme.colorScheme.contentColorFor(notPassedTrackColor),
  nextStopColor: Color = MaterialTheme.colorScheme.onSurface,
) = RouteSlider(
  value, stopCount = departures.size, modifier,
  weights = IntArray(departures.size - 1) { departures[it + 1] - departures[it] },
  trackWidth, pointRadius, nextPointRadius, passedTrackColor, notPassedTrackColor, passedStopColor,
  notPassedStopColor, nextStopColor,
)
