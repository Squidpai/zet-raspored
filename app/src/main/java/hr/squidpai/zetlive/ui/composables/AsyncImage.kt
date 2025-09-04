package hr.squidpai.zetlive.ui.composables

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.encoding.Base64

private sealed interface ImageState {
    @JvmInline
    value class Success(val image: ImageBitmap) : ImageState
    data object Downloading : ImageState
    data object Failure : ImageState
}

@Composable
fun AsyncImage(
    sourceUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val (state, setState) = remember { mutableStateOf<ImageState>(ImageState.Downloading) }

    val cacheDirectory = LocalContext.current.cacheDir

    LaunchedEffect(sourceUrl) {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(sourceUrl)
                    .openConnection() as HttpURLConnection

                val urlAsBase64 = Base64.UrlSafe.encode(sourceUrl.toByteArray())

                val imagePath = File(cacheDirectory, urlAsBase64)

                connection.ifModifiedSince = imagePath.lastModified()

                connection.connect()

                val bitmap = when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> BitmapFactory.decodeStream(
                        connection.inputStream.buffered()
                    )

                    HttpURLConnection.HTTP_NOT_MODIFIED -> BitmapFactory.decodeFile(
                        imagePath.path
                    )

                    else -> null
                }

                setState(
                    if (bitmap != null)
                        ImageState.Success(image = bitmap.asImageBitmap())
                    else
                        ImageState.Failure
                )

            } catch (e: IOException) {
                Log.e("AsyncImage", "Failed to download image $e", e)
                setState(ImageState.Failure)
            }
        }
    }

    if (state is ImageState.Success) {
        Image(state.image, contentDescription, modifier)
        return
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (state == ImageState.Downloading)
            LoadingIndicator()
        else // state = ImageState.Failure
            Icon(Symbols.HideImage, contentDescription)
    }
}