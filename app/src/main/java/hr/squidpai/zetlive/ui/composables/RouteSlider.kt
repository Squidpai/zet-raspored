package hr.squidpai.zetlive.ui.composables

import androidx.collection.IntList
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max

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
 * Example: `o=o=====o=--O--o----o` (weights = `[1, 5, 3, 2, 4]`)
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
   currentPointPosition += totalTrackLength * (weightRatios?.get(max(valueInt + 1, 0))
      ?: pointSpacing)
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
