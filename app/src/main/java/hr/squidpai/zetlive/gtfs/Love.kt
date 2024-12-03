package hr.squidpai.zetlive.gtfs

import android.util.Log
import androidx.collection.IntIntPair
import hr.squidpai.zetlive.IntObjectPair
import hr.squidpai.zetlive.IntPair
import hr.squidpai.zetlive.any
import hr.squidpai.zetlive.buildIntObjectMap
import hr.squidpai.zetlive.get
import hr.squidpai.zetlive.intIntMapOf
import hr.squidpai.zetlive.intObjectMapOf
import hr.squidpai.zetlive.to

object Love {

   @Suppress("unused")
   private const val TAG = "Love"

   private val stopLabels = buildIntObjectMap {
      operator fun Int.get(vararg pairs: IntObjectPair<String>) {
         for ((code, value) in pairs) {
            put(StopId(this, code).value, value)
         }
      }

      99[
         // Črnomerec
         32 to "Peroni 1, 3, 4, 5, 9, 10",
         31 to "Peron 2",
         62 to "Peron 6",
         52 to "Peron 7",
         42 to "Peron 8",
         75 to "Peron 11",
         85 to "Peron 12",
         26 to "Peron 15",
      ]

      104[
         // Britanski trg
         24 to "Peron 1",
         23 to "Peron 2",
         22 to "Peron 3",
      ]

      110[
         // Glavni kolodvor
         72 to "Peron 1",
         62 to "Peron 2",
         52 to "Peron 3",
         82 to "Peron 4",
         42 to "Peron 5",
         46 to "Peron 6",
         32 to "Peron 7",
         36 to "Peron 8",
         41 to "Peron 10",
         51 to "Peron 11",
         61 to "Peron 12",
      ]

      136[
         // Vrapcanska aleja
         26 to "Peron 2",
         27 to "Peron 3",
         29 to "Peron 4",
      ]

      161[
         // Zapresic
         26 to "Peron 1",
         21 to "Peron 3",
         31 to "Peron 4",
         22 to "Peron 7",
      ]

      176[
         // Mihaljevac
         28 to "Peron 1",
         34 to "Peron 2",
         27 to "Peron 3",
      ]

      192[
         // Borongaj (bus)
         42 to "Peron 1",
         32 to "Peron 2",
      ]

      193[2 to "Ulaz"] // Borongaj (tram)

      205[
         // Dubec
         71 to "Peron 1",
         61 to "Peron 2",
         51 to "Peroni 3, 4, 6, 8, 9, 10",
         44 to "Peron 5",
         42 to "Peron 7",
         82 to "Peron 11",
      ]

      208[
         // Dubrava
         44 to "Peron 1",
         34 to "Peron 2",
         24 to "Peron 3",
         54 to "Peron 4",
         23 to "Peron 5",
         33 to "Peron 6",
         53 to "Peron 7",
         63 to "Peron 8",
         43 to "Peron 9",
      ]

      244[
         // Ljubljanica (bus)
         24 to "Peron 1",
         34 to "Peron 2",
         44 to "Peron 3",
         54 to "Peron 4",
      ]

      245[1 to "Ulaz"] // Ljubljanica (tram)

      259[1 to "Ulaz"] // Precko (tram)

      266[3 to "Ulaz"] // Savisce

      271[
         // Savski most
         23 to "Peron 1",
         33 to "Peron 2",
         43 to "Peron 3",
         53 to "Peron 4",
         63 to "Peron 5",
         27 to "Peron 6",
         21 to "Peron 7",
      ]

      272[3 to "Ulaz"] // Savski most

      290[
         // Svetice
         43 to "Peron 1",
         33 to "Peron 2",
         23 to "Peron 3",
      ]

      420[
         // Kaptol
         23 to "Peron 1",
         25 to "Peron 2",
         27 to "Peron 3",
      ]

      611[27 to "Oba perona"] // Prečko - terminal

      663[23 to "Oba perona"] // Reljkoviceva

      1043[
         // Kvaternikov trg
         74 to "Peron 1",
         64 to "Peron 2",
         25 to "Peron 3",
         54 to "Peron 4",
         24 to "Peron 5",
         34 to "Peron 6",
         44 to "Peron 7",
      ]

      1665[
         // Sesvete - ukrcaj
         32 to "Peron 1",
         42 to "Peron 2",
         52 to "Peron 3",
         62 to "Peron 4",
         72 to "Peron 5",
         92 to "Peron 6",
      ]

      1669[28 to "Oba smjera"] // Jelkovec - tržnica
   }

   private const val RIGHT = 1
   private const val LEFT = 2
   private const val UP = 3
   private const val DOWN = 4

   private infix fun StopId.to(that: Int) = IntPair(this.value, that)

   private val iconInfo = intIntMapOf(
      StopId(99, 36) to LEFT,
      StopId(135, 26) to DOWN,
      StopId(136, 30) to UP,
      StopId(205, 25) to RIGHT,
      StopId(211, 25) to DOWN, // Elka
      StopId(458, 26) to LEFT,
      StopId(524, 25) to RIGHT, // Spansko-okretiste
      StopId(933, 23) to LEFT, // Crnkovec
      StopId(933, 24) to RIGHT, // Crnkovec
      StopId(933, 27) to UP, // Crnkovec
      StopId(933, 28) to DOWN, // Crnkovec
      StopId(954, 23) to LEFT, // Jordanovac (bus)
      StopId(954, 27) to UP, // Jordanovac (bus)
      StopId(954, 28) to DOWN, // Jordanovac (bus)
      StopId(1059, 26) to LEFT,
      StopId(1164, 28) to RIGHT, // Reber, Habeki
      StopId(1248, 27) to LEFT, // Cehi - Buliceva
      StopId(1248, 28) to RIGHT, // Cehi - Buliceva
      StopId(1402, 26) to LEFT, // Jelkovecka
      StopId(1402, 22) to DOWN, // Jelkovecka
   )

   @Suppress("unused") // Used for testing the icon info, TODO should probably be moved someplace else...
   fun testLabels(stops: Stops, routesAtStops: RoutesAtStopMap) {
      for (stop in stops.list) {
         if (stop.code !in 0..4 && stop.code !in 21..24 && stop.id.value !in stopLabels && stop.id.value !in iconInfo) {
            val routesAtStop = routesAtStops[stop.id.value]!!
            if (!routesAtStop.last && !routesAtStop.first)
               Log.i("LabelTest", "Stop ${stop.name} (${stop.id}) invalid")
         }
      }
   }

   fun giveMeTheLabelForStop(stopId: StopId): String? = stopLabels[stopId.value]

   fun giveMeTheIconCodeForStop(stopId: StopId): Int =
      iconInfo.getOrElse(stopId.value) { stopId.stationCode }

   private operator fun Int.get(vararg labelConditions: IntObjectPair<Pair<String?, String?>>) =
      this to labelConditions

   private infix fun Pair<String?, String?>.ifStop(stopId: String) =
      stopId.toStopId().value to this

   private infix fun Pair<String?, String?>.unlessStop(stopId: String) =
      -stopId.toStopId().value to this

   private val specialTripLabels = intObjectMapOf(
      132[
         "po Jadranskoj aveniji" to null ifStop "635_23", // Jadranska av.-Arena (sjeverno)
         null to "po Jadranskoj aveniji" ifStop "635_24", // Jadranska av.-Arena (juzno)
      ],
      140[null to "preko A.K. Snježna Kraljica" ifStop "1727_23"], // A.K.Snjezna Kraljica
      162["ne vozi preko Gajana" to null unlessStop "719_24"], // Gajani
      172[
         // Every trip stops at that stop, so the label is unnecessary
         //null to "Terminal" ifStop "161_22", // Zapresic (peron 7)
         "staje na Vrapčanskoj" to null ifStop "135_22", // Vrapcanska (zapadno)
      ],
      // Every trip stops at that stop, so the label is unnecessary
      /*174[
         null to "preko Kupljenskog Hruševca" ifStop "1916_21", // Hrusevecka (istocno)
         "preko Kupljenskog Hruševca" to null ifStop "1916_22", // Hrusevecka (istocno)
      ],*/
      182[null to "preko Groblja Zaprešić" ifStop "1641_22"], // Groblje Zapresic
      212[null to "ne vozi preko Selčine" unlessStop "1028_21"], // Trg Lovre Matacica
      220["preko Travnog" to null ifStop "1093_23"], // Božidara Magovca 111
      229[
         "preko Sloboštine" to null ifStop "1086_22", // Slobostina (zapadno)
         null to "preko Sloboštine" ifStop "1086_21", // Slobostina (istocno)
      ],
      268[
         "preko S.R. Njemačke" to null ifStop "565_24", // Islandska (juzno)
         null to "preko S.R. Njemačke" ifStop "565_22", // Islandska (zapadno)
      ],
      269[
         "staje na Maksimirskim naseljima" to null ifStop "1133_21", // Maksimirska naselja (istocno)
         null to "staje na Maksimirskim naseljima" ifStop "1133_22", // Maksimirska naselja (zapadno)
      ],
      276[null to "preko IKEA-e" ifStop "1889_23"], // IKEA
      284[
         null to "preko Sesvetske Selnice" ifStop "1404_23", // Ferde Kocha (sjeverno)
         "preko Sesvetske Selnice" to null ifStop "1404_24", // Ferde Kocha (juzno)
      ],
      // These lines no longer exist
      /*302[null to "preko Ključić brda" ifStop "1839_22"], // Kljucic brdo
      304[
         null to "preko Sisačke ceste" ifStop "874_21", // Sisacka - Mraclinska (istocno)
         "preko Sisačke ceste" to null ifStop "874_22", // Sisacka - Mraclinska (zapadno)
      ],*/
      307[null to "preko Sasa" ifStop "943_23"], // Sasi, okretiste (sjeverno)
      // These lines no longer exist
      /*321[
         null to "preko Sasa" ifStop "943_23", // Sasi, okretiste (sjeverno)
         null to "preko Zabrebačke" ifStop "1201_22", // Zagrebacka 42
      ],
      335["ne vozi preko Kurilovca" to null unlessStop "1298_22"], // Kolodvorska 76*/
   )

   fun giveMeTheSpecialTripLabel(trip: Trip): Pair<String?, String?>? =
      specialTripLabels[trip.routeId]?.let { conditions ->
         for ((stopId, label) in conditions) {
            if (stopId >= 0) {
               if (stopId in trip.stops)
                  return@let label
            } else if (label[1 - trip.directionId] != null && -stopId !in trip.stops)
               return@let label
         }
         null
      }

   const val NULL_SERVICE_ID_MESSAGE =
      "Ne postoji vozni red za izabrani datum.\nPokušajte se spojiti na " +
            "internet, ako već niste, kako bi se preuzela najnovija inačica rasporeda."

   fun giveMeTheSpecialLabelForNoTrips(
      routeId: RouteId,
      trips: List<Trip>?,
      serviceId: ServiceId?,
      selectedDate: Long,
      serviceIdTypes: ServiceIdTypes?,
   ): String {
      if (serviceId == null && trips != null)
         return NULL_SERVICE_ID_MESSAGE

      val serviceIdType =
         if (serviceIdTypes != null)
            serviceIdTypes[serviceId] ?: return NULL_SERVICE_ID_MESSAGE
         else
            ServiceIdType.ofDate(selectedDate)

      // Route 219 gets a special label.
      if ((serviceIdType == ServiceIdType.SATURDAY || serviceIdType == ServiceIdType.SUNDAY) && routeId == 219)
         return "Polaske subotom, nedjeljom i praznikom ostvaruje autobus linije 229 koji na Glavnom kolodvoru polazi " +
               "s perona 10 na Koturaškoj cesti."

      return when (serviceIdType) {
         ServiceIdType.WEEKDAY -> "Linija nema polazaka na izabrani datum."
         ServiceIdType.SATURDAY -> "Linija ne vozi vikendom i praznicima."
         ServiceIdType.SUNDAY -> {
            val saturdayServiceId =
               serviceIdTypes?.entries?.firstOrNull { it.value == ServiceIdType.SATURDAY }?.key

            if (saturdayServiceId != null && trips?.none { it.serviceId == saturdayServiceId } == true)
               "Linija ne vozi vikendom i praznicima."
            else
               "Linija ne vozi nedjeljom i praznicima."
         }
      }
   }

   fun giveMeTheServiceIdTypes(schedule: LoadedSchedule): ServiceIdTypes? {
      val serviceIds = schedule.calendarDates.serviceIds
      // I've selected 108 as the route with only weekday travels
      val tripsOfRouteWeekdaysOnly = schedule.getTripsOfRoute(108).value?.list ?: return null
      // I've selected 159 as the route with weekday and saturday travels
      val tripsOfRouteWeekdaysAndSaturday = schedule.getTripsOfRoute(159).value?.list ?: return null

      return giveMeTheServiceIdTypes(
         serviceIds,
         tripsOfRouteWeekdaysOnly,
         tripsOfRouteWeekdaysAndSaturday
      )
   }

   fun giveMeTheServiceIdTypes(
      serviceIds: Iterator<ServiceId>,
      tripsOfRouteWeekdaysOnly: TripsList,
      tripsOfRouteWeekdaysAndSaturday: TripsList,
   ): ServiceIdTypes {
      val map = HashMap<ServiceId, ServiceIdType>()

      for (serviceId in serviceIds) {
         map[serviceId] = if (tripsOfRouteWeekdaysOnly.any { it.serviceId == serviceId })
         // There exist trips of this route on this service id.
         // Since this route drives on weekdays only, this must be a weekday.
            ServiceIdType.WEEKDAY
         else if (tripsOfRouteWeekdaysAndSaturday.any { it.serviceId == serviceId })
         // There exist trips of this route on this service id.
         // Since this route drives on weekdays and saturdays, and it is not a weekday, this must be a saturday.
            ServiceIdType.SATURDAY
         else
         // There are no trips on the route that drives on weekdays and saturdays,
         // thus this must be a sunday (or a holiday).
            ServiceIdType.SUNDAY
      }

      return map
   }

   fun giveMeTheForcedCommonHeadsign(routeId: RouteId): Pair<String?, String?>? =
      when (routeId) {
         217 -> "Petruš. nas." to null
         295 -> "Sajam Jakuševec" to null
         else -> null
      }

   private infix fun StopId.to(other: StopId) = IntIntPair(this.value, other.value)

   fun giveMeTheForcedFirstStop(routeId: RouteId) =
      when (routeId) {
         217 -> StopId.Invalid to StopId(1081, 21)
         295 -> StopId.Invalid to StopId(1230, 22)
         else -> StopId.Invalid to StopId.Invalid
      }

   /*
   TODO display which routes have the ability to carry bikes.
   It would be too much to check each specific trip since it would need
   to be manually checked, rather just show that the routes can carry
   bikes and tell the user to check the official route schedule for specifics.
   routes: 102, 103, 140
    */

}
