package hr.squidpai.zetlive

import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import androidx.collection.MutableIntIntMap
import hr.squidpai.zetlive.gtfs.RouteId
import java.io.*

/**
 * Object containing user data and preferences.
 */
object Data {

  private const val TAG = "Data"

  private const val PINNED_ROUTES = "pinnedRoutes"
  private const val PINNED_STOPS = "pinnedStops"
  private const val PREFERRED_DEFAULT_STOP_CODES = "defaultStopCodes"

  private var file: File? = null

  /**
   * The set of routes pinned to the top of the route list
   * in `MainActivityRoutes`, specifically, their IDs.
   */
  val pinnedRoutes = mutableStateSetOf<RouteId>()

  /**
   * The set of stops pinned to the top of the stop list
   * in `MainActivityStops`, specifically, their **parent IDs**.
   */
  val pinnedStops = mutableStateSetOf<Int>()

  /**
   * A map containing the index of the last selected child stop from
   * a grouped stop, the key being the **parent stop ID** and the value
   * being the index.
   *
   * Used in `MainActivityStops` to remember and show the stop which the
   * user previously selected, as the user will probably often check on
   * one specific stop.
   */
  val defaultStopCodes = MutableIntIntMap()

  /**
   * Tries loading the data from [file].
   *
   * If loading fails for whatever reason, the data
   * successfully loaded up until the error occurred will still
   * be loaded and no exception will be thrown.
   */
  fun load(file: File) {
    this.file = file

    try {
      JsonReader(file.bufferedReader()).use { reader ->
        reader.beginObject()

        while (reader.hasNext()) {
          when (reader.nextName()) {
            PINNED_ROUTES -> {
              reader.beginArray()

              val list = ArrayList<Int>()
              while (reader.hasNext())
                list += reader.nextInt()

              pinnedRoutes.clear()
              pinnedRoutes.addAll(list)

              reader.endArray()
            }

            PINNED_STOPS -> {
              reader.beginArray()

              val list = ArrayList<Int>()
              while (reader.hasNext())
                list += reader.nextInt()

              pinnedStops.clear()
              pinnedStops.addAll(list)

              reader.endArray()
            }

            PREFERRED_DEFAULT_STOP_CODES -> {
              reader.beginObject()

              defaultStopCodes.clear()
              while (reader.hasNext()) {
                val key = reader.nextName().toInt()
                val value = reader.nextInt()

                defaultStopCodes[key] = value
              }

              reader.endObject()
            }
          }
        }

        reader.endObject()
      }
    } catch (_: FileNotFoundException) {
      // no data to load
    } catch (e: IllegalStateException) {
      Log.w(TAG, "load: IllegalStateException occurred while loading data", e)
    } catch (e: IOException) {
      Log.w(TAG, "load: IOException occurred while loading data", e)
    }
  }

  /**
   * Saves the data to the file last loaded from in [load].
   *
   * If [load] was never called, the method does nothing.
   */
  fun save() {
    val file = file ?: run {
      Log.e(TAG, "save: No file to save to.")
      return
    }

    try {
      JsonWriter(file.bufferedWriter()).use { writer ->
        writer.setIndent(" ")
        writer.beginObject()

        val routes = pinnedRoutes.toSet()
        if (routes.isNotEmpty()) {
          writer.name(PINNED_ROUTES)
            .beginArray()
          for (route in routes)
            writer.value(route)
          writer.endArray()
        }

        val stops = pinnedStops.toSet()
        if (stops.isNotEmpty()) {
          writer.name(PINNED_STOPS)
            .beginArray()
          for (route in stops)
            writer.value(route)
          writer.endArray()
        }

        if (defaultStopCodes.isNotEmpty()) {
          writer.name(PREFERRED_DEFAULT_STOP_CODES)
            .beginObject()
          defaultStopCodes.forEach { k, v ->
            writer.name(k.toString()).value(v)
          }
          writer.endObject()
        }

        writer.endObject()
      }
    } catch (e: IOException) {
      Log.w(TAG, "save: IOException occurred while saving", e)
    }
  }

  /**
   * Calls [block] with the current data and then calls [save].
   */
  inline fun <R> updateData(block: Data.() -> R) = this.block().also { save() }

}