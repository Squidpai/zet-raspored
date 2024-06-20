package hr.squidpai.zetlive.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import hr.squidpai.zetlive.gtfs.Trip

private val LightColors = lightColorScheme(
  primary = Color(0xFF004DEA),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFDCE1FF),
  onPrimaryContainer = Color(0xFF001551),
  secondary = Color(0xFF684FA4),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFE9DDFF),
  onSecondaryContainer = Color(0xFF23005C),
  tertiary = Color(0xFF00677F),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFB7EAFF),
  onTertiaryContainer = Color(0xFF001F28),
  error = Color(0xFFBA1A1A),
  errorContainer = Color(0xFFFFDAD6),
  onError = Color(0xFFFFFFFF),
  onErrorContainer = Color(0xFF410002),
  background = Color(0xFFFEFBFF),
  onBackground = Color(0xFF1B1B1F),
  surface = Color(0xFFFEFBFF),
  onSurface = Color(0xFF1B1B1F),
  surfaceVariant = Color(0xFFE2E1EC),
  onSurfaceVariant = Color(0xFF45464F),
  outline = Color(0xFF767680),
  inverseOnSurface = Color(0xFFF2F0F4),
  inverseSurface = Color(0xFF303034),
  inversePrimary = Color(0xFFB7C4FF),
  surfaceTint = Color(0xFF004DEA),
  outlineVariant = Color(0xFFC6C5D0),
  scrim = Color(0xFF000000),
)


private val DarkColors = darkColorScheme(
  primary = Color(0xFFB7C4FF),
  onPrimary = Color(0xFF002780),
  primaryContainer = Color(0xFF0039B4),
  onPrimaryContainer = Color(0xFFDCE1FF),
  secondary = Color(0xFFB09CFF),
  onSecondary = Color(0xFF290E72),
  secondaryContainer = Color(0xFF50378A),
  onSecondaryContainer = Color(0xFFE9DDFF),
  tertiary = Color(0xFF5CD5FB),
  onTertiary = Color(0xFF003543),
  tertiaryContainer = Color(0xFF004E60),
  onTertiaryContainer = Color(0xFFB7EAFF),
  error = Color(0xFFFFB4AB),
  errorContainer = Color(0xFF93000A),
  onError = Color(0xFF690005),
  onErrorContainer = Color(0xFFFFDAD6),
  background = Color(0xFF1B1B1F),
  onBackground = Color(0xFFE4E1E6),
  surface = Color(0xFF1B1B1F),
  onSurface = Color(0xFFE4E1E6),
  surfaceVariant = Color(0xFF45464F),
  onSurfaceVariant = Color(0xFFC6C5D0),
  outline = Color(0xFF90909A),
  inverseOnSurface = Color(0xFF1B1B1F),
  inverseSurface = Color(0xFFE4E1E6),
  inversePrimary = Color(0xFF004DEA),
  surfaceTint = Color(0xFFB7C4FF),
  outlineVariant = Color(0xFF45464F),
  scrim = Color(0xFF000000),
)

val LocalSelectTrip =
  staticCompositionLocalOf<(Trip?, Long) -> Unit> { { _, _ -> } }

val LocalStatusBarColor =
  staticCompositionLocalOf { Color.Unspecified }

@Composable
fun AppTheme(
  statusBarElevation: Dp = 0.dp,
  content: @Composable () -> Unit
) {
  val darkTheme = isSystemInDarkTheme()

  val colorScheme = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    darkTheme -> DarkColors
    else -> LightColors
  }

  val statusBarColor = colorScheme.surfaceColorAtElevation(statusBarElevation)

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = statusBarColor.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme) {
    val selectedTrip = remember { mutableStateOf<Pair<Trip, Long>?>(null) }

    CompositionLocalProvider(
      LocalSelectTrip provides { trip, timeOffset ->
        selectedTrip.value = trip?.let { it to timeOffset }
      },
      LocalStatusBarColor provides statusBarColor
    ) {
      content()
    }

    LocalTripDialog(selectedTrip)
  }
}

@Composable
fun LocalTripDialog(selectedTrip: MutableState<Pair<Trip, Long>?>) {
  selectedTrip.value?.let { (stopTime, timeOffset) ->
    TripDialog(onDismissRequest = { selectedTrip.value = null }, stopTime, timeOffset)
  }
}
