/*
Big thanks to iamcalledrob for creating this code. All credits go to them.
I would've pulled all my hair out trying to figure this out.
Link: https://gist.github.com/iamcalledrob/871568679ad58e64959b097d4ef30738
*/

package hr.squidpai.zetlive.ui

import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Picture
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.coroutines.suspendCoroutine

/*
    Usage example:

    val bitmap = useVirtualDisplay(applicationContext) { display ->
        captureComposable(
            context = context,
            size = DpSize(100.dp, 100.dp),
            display = display
        ) {
            LaunchedEffect(Unit) {
                capture()
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Red))
        }
    }
    
 */

/** Use virtualDisplay to capture composables into a virtual (i.e. invisible) display. */
suspend fun <T> useVirtualDisplay(context: Context, callback: suspend (display: Display) -> T): T {
   val texture = SurfaceTexture(false)
   val surface = Surface(texture)
   // Size of virtual display doesn't matter, because images are captured from compose, not the display surface.
   val virtualDisplay = context.getDisplayManager().createVirtualDisplay(
      "virtualDisplay", 1, 1, 72, surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
   )

   val result = callback(virtualDisplay.display)

   virtualDisplay.release()
   surface.release()
   texture.release()

   return result
}

data class CaptureComposableScope(val capture: () -> Unit)

/** Captures composable content, by default using a hidden window on the default display.
 *
 *  Be sure to invoke capture() within the composable content (e.g. in a LaunchedEffect) to perform the capture.
 *  This gives some level of control over when the capture occurs, so it's possible to wait for async resources */
suspend fun captureComposable(
   context: Context,
   size: IntSize,
   display: Display = context.getDisplayManager().getDisplay(Display.DEFAULT_DISPLAY),
   content: @Composable CaptureComposableScope.() -> Unit,
): Bitmap {
   val presentation = Presentation(context.applicationContext, display).apply {
      window?.decorView?.let { view ->
         view.setViewTreeLifecycleOwner(ProcessLifecycleOwner.get())
         view.setViewTreeSavedStateRegistryOwner(EmptySavedStateRegistryOwner.shared)
         view.alpha =
            0f // If using default display, to ensure this does not appear on top of content.
      }
   }

   val composeView = ComposeView(context).apply {
      require(size.width > 0 && size.height > 0) { "pixel size must not have zero dimension" }

      layoutParams = ViewGroup.LayoutParams(size.width, size.height)
   }

   presentation.setContentView(composeView, composeView.layoutParams)
   presentation.show()

   // removing the explicit type argument causes an internal compiler error
   @Suppress("RemoveExplicitTypeArguments")
   val bitmap = suspendCoroutine<Bitmap> { continuation ->
      composeView.setContent {
         var shouldCapture by remember { mutableStateOf(false) }
         val dpSize = with(LocalDensity.current) {
            DpSize(size.width.toDp(), size.height.toDp())
         }
         Box(
            modifier = Modifier
               .size(dpSize)
               .thenIf(shouldCapture) {
                  drawIntoPicture { picture ->
                     val result = Result.success(picture.toBitmap())
                     continuation.resumeWith(result)
                  }
               },
         ) {
            CaptureComposableScope(capture = { shouldCapture = true }).run {
               content()
            }
         }
      }
   }

   presentation.dismiss()
   return bitmap
}

private inline fun Modifier.thenIf(
   condition: Boolean,
   crossinline other: Modifier.() -> Modifier,
) = if (condition) other() else this

private fun Context.getDisplayManager(): DisplayManager =
   getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

// Note: This causes a warning: requestLayout() improperly called by androidx.compose.ui.platform.ViewLayerContainer
//       In Compose 1.7.0, this could be replaced with rememberGraphicsLayer(), which may fix this?
private fun Modifier.drawIntoPicture(onDraw: (Picture) -> Unit) = this
   .drawWithContent {
      val width = size.width.toInt()
      val height = size.height.toInt()

      val picture = Picture()
      val canvas = Canvas(picture.beginRecording(width, height))
      draw(this, layoutDirection, canvas, size) {
         this@drawWithContent.drawContent()
      }
      picture.endRecording()

      onDraw(picture)
   }

private fun Picture.toBitmap(): Bitmap =
   Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
      android.graphics.Canvas(it).drawPicture(this)
   }


private class EmptySavedStateRegistryOwner : SavedStateRegistryOwner {
   private val controller = SavedStateRegistryController.create(this).apply {
      performRestore(null)
   }

   private val lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()

   override val lifecycle: Lifecycle
      get() =
         // lifecycleOwner for some reason can be null sometimes
         @Suppress("UNNECESSARY_SAFE_CALL")
         object : Lifecycle() {
            override fun addObserver(observer: LifecycleObserver) {
               lifecycleOwner?.lifecycle?.addObserver(observer)
            }

            override fun removeObserver(observer: LifecycleObserver) {
               lifecycleOwner?.lifecycle?.removeObserver(observer)
            }

            override val currentState = State.INITIALIZED
         }

   override val savedStateRegistry: SavedStateRegistry
      get() = controller.savedStateRegistry

   companion object {
      val shared = EmptySavedStateRegistryOwner()
   }
}