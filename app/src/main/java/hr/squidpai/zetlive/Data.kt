package hr.squidpai.zetlive

import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hr.squidpai.zetlive.gtfs.RouteId
import hr.squidpai.zetlive.gtfs.StopId
import hr.squidpai.zetlive.gtfs.toStopId
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Object containing user data and preferences.
 */
object Data {

   private const val TAG = "Data"

   private const val PINNED_ROUTES = "pinnedRoutes"
   private const val PINNED_STOPS = "pinnedStops"
   private const val DIRECTION_SWAPPED = "directionSwapped"
   private const val DEFAULT_STOP_CODES = "defaultStopCodes"
   private const val HIGHLIGHT_NEXT_STOP = "highlightNextStop"
   private const val HINTS = "hints"

   private var file: File? = null

   /**
    * The set of routes pinned to the top of the route list
    * in `MainActivityRoutes`, specifically, their IDs.
    */
   val pinnedRoutes = mutableStateSetOf<RouteId>()

   /**
    * The set of stops pinned to the top of the stop list
    * in `MainActivityStops`, specifically, their **station number**.
    */
   val pinnedStops = mutableStateSetOf<Int>()

   /**
    * The set of routes for which the user selected the opposite direction to view.
    *
    * Used in `MainActivityRoutes` and `RouteScheduleActivity` to remember and
    * show the direction which the user previously selected.
    */
   val directionSwapped = MutableIntSet()

   /** Returns the preferred direction based on [directionSwapped]. */
   fun getDirectionForRoute(routeId: RouteId) = (routeId in directionSwapped).toInt()

   /**
    * A map containing the station code of the last selected child stop from
    * a grouped stop, the key being the **station number**, and the value
    * being the **station code**.
    *
    * Used in `MainActivityStops` to remember and show the stop which the
    * user previously selected, as the user will probably often check on
    * one specific stop.
    */
   val defaultStopCodes = MutableIntIntMap()

   /**
    * If `true`, the next station is highlighted. Otherwise, the
    * current station is highlighted.
    */
   var highlightNextStop by mutableStateOf(true)

   // this class and its entries are used as properties
   @Suppress("ClassName", "EnumEntryName")
   enum class hints(val hintText: String, private vararg val dependencies: hints) {
      swapDirection("Pritisnite na strelicu (⇄) da se prikaže vozni red za suprotni smjer.");

      private var satisfied = false

      fun satisfyWithoutUpdate() {
         if (shouldBeVisible())
            satisfied = true
      }

      fun satisfy() {
         if (shouldBeVisible()) {
            satisfied = true
            save()
         }
      }

      fun shouldBeVisible() = !satisfied && dependencies.all { it.satisfied }

      companion object {
         internal fun load(reader: JsonReader) {
            reader.beginArray()
            while (reader.hasNext())
               try {
                  valueOf(reader.nextString()).satisfied = true
               } catch (_: IllegalArgumentException) {
               }
            reader.endArray()
         }

         internal fun save(writer: JsonWriter) {
            writer.beginArray()
            for (entry in entries)
               if (entry.satisfied)
                  writer.value(entry.name)
            writer.endArray()
         }
      }
   }

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
                        list += reader.nextInt() //.let { if (it >= 65536) it / 65536 else it }

                     pinnedStops.clear()
                     pinnedStops.addAll(list)

                     reader.endArray()
                  }

                  DIRECTION_SWAPPED -> {
                     reader.beginArray()

                     directionSwapped.clear()
                     while (reader.hasNext())
                        directionSwapped += reader.nextInt()

                     reader.endArray()
                  }

                  DEFAULT_STOP_CODES -> {
                     reader.beginArray()

                     defaultStopCodes.clear()
                     while (reader.hasNext()) {
                        val (number, code) = reader.nextString().toStopId()

                        defaultStopCodes[number] = code
                     }

                     reader.endArray()
                  }

                  HIGHLIGHT_NEXT_STOP -> highlightNextStop = reader.nextBoolean()

                  HINTS -> hints.load(reader)

                  else -> reader.skipValue()
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
               for (stop in stops)
                  writer.value(stop)
               writer.endArray()
            }

            if (directionSwapped.isNotEmpty()) {
               writer.name(DIRECTION_SWAPPED)
                  .beginArray()
               directionSwapped.forEach { route ->
                  writer.value(route)
               }
               writer.endArray()
            }

            if (defaultStopCodes.isNotEmpty()) {
               writer.name(DEFAULT_STOP_CODES)
                  .beginArray()
               defaultStopCodes.forEach { k, v ->
                  writer.value(StopId(k, v).toString())
               }
               writer.endArray()
            }

            writer.name(HIGHLIGHT_NEXT_STOP)
               .value(highlightNextStop)

            writer.name(HINTS)
            hints.save(writer)

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