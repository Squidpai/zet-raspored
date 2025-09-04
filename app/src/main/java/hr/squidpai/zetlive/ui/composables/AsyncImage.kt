package hr.squidpai.zetlive.ui.composables

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import hr.squidpai.zetlive.ui.Symbols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.encoding.Base64
import kotlin.math.max

private sealed interface ImageState {
    @JvmInline
    value class Success(val image: ImageBitmap) : ImageState
    data object Downloading : ImageState
    data object Failure : ImageState
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                    HttpURLConnection.HTTP_OK -> decodeStreamAndSave(
                        stream = connection.inputStream.buffered(),
                        targetFile = imagePath,
                        lastModified = connection.lastModified,
                        contentLength = connection.contentLength,
                    )

                    HttpURLConnection.HTTP_NOT_MODIFIED -> BitmapFactory.decodeFile(imagePath.path)

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

private const val READ_BLOCK_SIZE = 16384

private const val MAX_ARRAY_SIZE = Int.MAX_VALUE - 8

private fun decodeStreamAndSave(
    stream: BufferedInputStream,
    targetFile: File,
    lastModified: Long,
    contentLength: Int,
): Bitmap? {
    val buffer = object : ByteArrayOutputStream(max(contentLength, 65536)) {
        fun toByteArrayInputStream() = ByteArrayInputStream(buf, 0, count)

        private fun ensureCapacityForTransfer() {
            val minCapacity = count + READ_BLOCK_SIZE
            // overflow-conscious code
            if (minCapacity - buf.size > 0) {
                // overflow-conscious code
                val oldCapacity = buf.size
                var newCapacity = oldCapacity shl 1
                if (newCapacity - minCapacity < 0) newCapacity = minCapacity
                // Int.MAX_VALUE = MAX_ARRAY_SIZE
                if (newCapacity - MAX_ARRAY_SIZE > 0)
                    newCapacity =
                        if (minCapacity < 0) // overflow
                            throw OutOfMemoryError()
                        else if (minCapacity > MAX_ARRAY_SIZE)
                            Int.MAX_VALUE
                        else MAX_ARRAY_SIZE
                buf = buf.copyOf(newCapacity)
            }
        }

        fun transferFrom(input: InputStream) {
            while (true) {
                ensureCapacityForTransfer()
                val readBytes = input.read(buf, count, READ_BLOCK_SIZE)
                if (readBytes == -1)
                    break
                count += readBytes
            }
        }
    }

    buffer.transferFrom(stream)

    FileOutputStream(targetFile).use { fileOutputStream ->
        buffer.writeTo(fileOutputStream)
    }

    targetFile.setLastModified(lastModified)

    return BitmapFactory.decodeStream(buffer.toByteArrayInputStream())
}
