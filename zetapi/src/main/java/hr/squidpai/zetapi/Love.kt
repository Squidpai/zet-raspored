package hr.squidpai.zetapi

import androidx.collection.IntIntPair
import androidx.collection.MutableIntObjectMap

public object Love {

   @Suppress("unused")
   private const val TAG = "Love"

   private val stopLabels = MutableIntObjectMap<String>().apply {
      operator fun Int.get(vararg pairs: Pair<Int, String>) {
         for ((code, value) in pairs) {
            put(StopId(this, code).rawValue, value)
         }
      }

      99[
         // Črnomerec
         //32 to "Peroni 1, 3, 4, 5, 9, 10",
         101 to "Peron 1",
         31 to "Peron 2",
         103 to "Peron 3",
         104 to "Peron 4",
         105 to "Peron 5",
         62 to "Peron 6",
         52 to "Peron 7",
         42 to "Peron 8",
         109 to "Peron 9",
         110 to "Peron 10",
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
         //51 to "Peroni 3, 4, 6, 8, 9, 10",
         103 to "Peron 3",
         104 to "Peron 4",
         44 to "Peron 5",
         106 to "Peron 6",
         42 to "Peron 7",
         108 to "Peron 8",
         109 to "Peron 9",
         110 to "Peron 10",
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

   public fun giveMeTheLabelForStop(stopId: StopId): String? =
      stopLabels[stopId.rawValue]

   private const val RIGHT = 1
   private const val LEFT = 2
   private const val UP = 3
   private const val DOWN = 4

   private val iconInfo = intIntMapOf(
      StopId(99, 36).rawValue, LEFT,
      StopId(135, 26).rawValue, DOWN,
      StopId(136, 30).rawValue, UP,
      StopId(205, 25).rawValue, RIGHT,
      StopId(211, 25).rawValue, DOWN, // Elka
      StopId(458, 26).rawValue, LEFT,
      StopId(524, 25).rawValue, RIGHT, // Spansko-okretiste
      StopId(933, 23).rawValue, LEFT, // Crnkovec
      StopId(933, 24).rawValue, RIGHT, // Crnkovec
      StopId(933, 27).rawValue, UP, // Crnkovec
      StopId(933, 28).rawValue, DOWN, // Crnkovec
      StopId(954, 23).rawValue, LEFT, // Jordanovac (bus)
      StopId(954, 27).rawValue, UP, // Jordanovac (bus)
      StopId(954, 28).rawValue, DOWN, // Jordanovac (bus)
      StopId(1059, 26).rawValue, LEFT,
      StopId(1164, 28).rawValue, RIGHT, // Reber, Habeki
      StopId(1248, 27).rawValue, LEFT, // Cehi - Buliceva
      StopId(1248, 28).rawValue, RIGHT, // Cehi - Buliceva
      StopId(1402, 26).rawValue, LEFT, // Jelkovecka
      StopId(1402, 22).rawValue, DOWN, // Jelkovecka
   )

   public fun giveMeTheIconCodeForStop(stopId: StopId): Int =
      iconInfo.getOrElse(stopId.rawValue) { stopId.stopCode }

   // TODO actually this can probably be read from the ZET website to make fully accessable route names
   private val extraRouteKeywords = mapOf(
      "1" to "Zapadni kolodvor",
      "13" to "Kvaternikov",
      "31" to "Savski",
      "101" to "Britanski Gornje",
      "102" to "Britanski",
      "103" to "Britanski",
      "104" to "Mihaljevac",
      "106" to "Mirogoj Krematorij",
      "108" to "Glavni kolodvor",
      // Zag.(S.m).-D.S.-S.O. ?????????????????????????????????? hvala ti zet
      "111" to "Zagreb Savski Most Donji Stupnik Stupnički Obrež",
      "115" to "Ljubljanica Jankomir",
      "116" to "Ljubljanica Podsused",
      "118" to "Mažuranića",
      "119" to "Črnomerec",
      "120" to "Črnomerec",
      "121" to "Črnomerec",
      "122" to "Črnomerec",
      "123" to "Podsusedsko",
      "124" to "Gornji Stenjevec",
      "125" to "Gornje",
      "126" to "Črnomerec Gornja Kustošija",
      "129" to "Šestinski",
      "132" to "Savski Goli Brezovica",
      "133" to "Savski Sveta",
      "137" to "Črnomerec",
      "138" to "Britanski trg Zelengaj",
      "139" to "Reljkovićeva",
      "141" to "Reljkovićeva",
      "142" to "Vrapčanska aleja Jačkovina",
      "143" to "Vrapčanska",
      "144" to "Črnomerec Sveti",
      "146" to "Reljkovićeva",
      "147" to "Vrapčanska",
      "148" to "Reljkovićeva Hercegovačka Bosanska",
      "150" to "Tuškanac garaža Gornji grad bana Josipa Jelačića",
      "151" to "Završje Stjepana",
      "159" to "Savski Strmec",
      "160" to "Savski Havidić Selo",
      "161" to "Savski most Kupinečki kraljevec Štrpet",
      "162" to "Savski",
      "163" to "Savski most Doonji Trpuci Gornji",
      "164" to "Zagreb Savski most",
      "166" to "Zagreb Glavni kolodvor Donji Dragonožec",
      "168" to "Savski Ježdovec",
      "172" to "Zagreb Črnomerec",
      "174" to "Zaprešić Hruševec Kupljenski",
      "176" to "Zagreb Črnomerec Gornja",
      "177" to "Zagreb Črnomerec Poljanica Gornja Bistra",
      "201" to "Kvaternikov",
      "202" to "Kvaternikov",
      "204" to "Kvaternikov Horvatovac Voćarska",
      "205" to "Dubrava Markuševec",
      "207" to "Kvaternikov",
      "210" to "Dubrava Studentski grad Novi retkovec",
      "211" to "Branovečina",
      "214" to "Koledinečka Kozari bok",
      "215" to "Kvaternikov",
      "216" to "Kvaternikov Ivanja",
      "217" to "Kvaternikov Petruševečko naselje",
      "218" to "Glavni",
      "219" to "kolodvor",
      "220" to "kolodvor",
      "221" to "kolodvor",
      "223" to "Trnovčica",
      "225" to "Sesvete Kozari",
      "226" to "Svetice",
      "227" to "Gornji bukovac Gračansko",
      "228" to "Borongaj",
      "229" to "Glavni Mala",
      "230" to "Granešinski",
      "233" to "Markuševec",
      "234" to "Glavni kolodvor Kajzerica Lanište",
      "235" to "Kozari",
      "237" to "Kvaternikov",
      "239" to "Čučerje Dankovec Markuševečka Trnava",
      "241" to "Glavni Veliko",
      "242" to "kolodvor",
      "243" to "Glavni kolodvor",
      "261" to "Sesvete",
      "262" to "Sesvete Planina",
      "263" to "Sesvete Kašina Planina Gornja",
      "264" to "Sesvete",
      "268" to "Zagreb Glavni kolodvor Velika Gorica",
      "269" to "Borongaj Sesvetski Kraljevec",
      "271" to "Sesvete Glavnica",
      "272" to "Sesvete",
      "273" to "Sesvete",
      "274" to "Zagreb Sesvete Laktec",
      "276" to "Zagreb Kvaternikov trg Ivanja Reka Dumovec",
      "277" to "Sesvetska",
      "278" to "Sesvete Sesvetska Sela Kraljevečki",
      "280" to "Sesvete Šimunčevec",
      "281" to "Glavni kolodvor Novi",
      "282" to "Novi",
      "283" to "Brestje",
      "284" to "Sesvete Ivanja Dumovec",
      "290" to "Zagreb Kvaternikov trg Zračna luka Velika Gorica",
      "295" to "Zaprešić Jakuševec",
      "307" to "Zagreb Strmec Bukevski",
      "308" to "Zagreb",
      "310" to "Zagreb glavni kolodvor",
      "311" to "Zagreb glavni kolodvor Cerovski",
      "313" to "Zagreb glavni kolodvor",
      "315" to "Zagreb Savski most",
      "330" to "Zagreb Glavni kolodvor Velika Gorica linija",
      "613" to "dolje",
      "617" to "Gračansko Draškovićeva",
   )

   public fun giveMeTheExtraKeywordForRoute(routeId: RouteId): String? = extraRouteKeywords[routeId]

   private val extraStopKeywords = MutableIntObjectMap<String>().apply {
      infix fun Int.to(value: String) {
         put(this, value)
      }

      103 to "Trg doktora Franje Tuđmana"
      106 to "Trg bana Josipa Jelačića"
      112 to "Branimirova tržnica"
      113 to "Autobusni kolodvor"
      128 to "Radićevo šetalište"
      170 to "Folnegovićevo naselje"
      171 to "Folnegovićevo naselje"
      277 to "Muzej suvremene umjetnosti"
      278 to "Muzej suvremene umjetnosti"
      286 to "Studentski dom Stjepan Radić"
      287 to "Studentski dom Stjepan Radić"
      296 to "Trg hrvatskih velikana"
      297 to "Trg kralja Petra Krešimira IV."
      298 to "Trg republike Hrvatske"
      299 to "Trg žrtava fašizma"
      305 to "Učiteljski fakultet"
      313 to "Zagrebački transporti"
      323 to "Soblinec - raskrižje - škola"
      339 to "Glavnica Donja okretište"
      350 to "Vurnovec - Banov brijeg - Kusova"
      353 to "Prepuštovec - izletište"
      376 to "Markovo polje - raskrižje"
      379 to "Vugrovec Donji - groblje"
      385 to "Bistrička - osnovna škola"
      433 to "Institut Ruđer Bošković"
      450 to "Nacionalna i sveučilišna knjižnica"
      451 to "Trnjanska Savica"
      459 to "Robni terminali Žitnjak"
      471 to "ZET - garaža Podsused"
      526 to "Remetinečka osnovna škola"
      551 to "Crkva svete Mati Slobode"
      567 to "Vatikanska - Svetog Mateja"
      569 to "Robni terminali Jankomir"
      571 to "Oktavijana Miletića"
      628 to "Trg Stjepana Severa"
      629 to "Zastavnice - nadvožnjak"
      635 to "Jadranska avenija - Arena"
      646 to "Šestinski dol odvojak"
      657 to "Ivane Brlić Mažuranić"
      662 to "Perjavica - Dominika Mandića"
      667 to "Tekstilno-tehnički fakultet"
      685 to "Gornjodragonožečka - Stari dom"
      687 to "Gornjodragonožečka - spomenik"
      694 to "Brezovička - pod kestenima"
      704 to "Kraljevečki brijegi - III. odvojak"
      705 to "Kraljevečki brijegi - VI. odvojak"
      706 to "Kupinečki Kraljevec - škola"
      707 to "Kraljevečki brijegi - Lojeni"
      708 to "Kraljevečki brijegi - Starjak"
      709 to "Kraljevečki brijegi - Vodosprema"
      771 to "Donja Bistra - centar"
      772 to "Poljanica - kod škole"
      777 to "Gornja Bistra - okretište"
      795 to "Poljanica - Zeleni brijeg"
      919 to "Sarajevska - Jakuševečka"
      927 to "Kosnica - izbjegličko naselje"
      934 to "Črnkovec, put za Strmec"
      939 to "Strmec Bukevski, okretište"
      971 to "Avenija Gojka Šuška - MUP"
      980 to "Markuševečka Trnava - Vida Ročića"
      981 to "Markuševečka Trnava 56"
      982 to "Markuševečka Trnava 84"
      1001 to "Markuševečki Popovec 62"
      1044 to "Veterinarski fakultet"
      1066 to "Ivanja Reka - okretište"
      1067 to "Svetog Ivana - Dane Grubera"
      1079 to "Petručevec Im odvojak V"
      1104 to "Brestovečka - društveni dom"
      1124 to "Bolnica Jordanovac"
      1135 to "I. Štefanovečki zavoj"
      1136 to "Štefanovečka - studentski kampus"
      1139 to "Branimirova - Aleja Javora"
      1141 to "Svetošimunska - trgovina"
      1145 to "VIII. Jazbinski odvojak"
      // TODO 1168 to "??????????????" (Gor Prekvršje P.i T.)
      1192 to "Hrašće Turopoljsko"
      1203 to "Velika Gorica - groblje"
      1216 to "Šašinovec - vatrogasni dom"
      1252 to "Put za Gornju Lomnicu"
      1252 to "Lomnica, Deverićeva 4"
      1256 to "Lomnica, Stepanska 6, okretište"
      1259 to "Gradići, Kralja Tomislava 85"
      1261 to "Gradići, društveni dom"
      1264 to "Petrovina, društveni dom"
      1266 to "Lukavec - Dolenska društveni dom"
      1271 to "Dubranec, vikend naselje"
      1288 to "Sisačka, nadvožnjak sjever"
      1295 to "Lukavec - put za Dubranec"
      1296 to "Lukavec - Školska - Lužec"
      1335 to "Trg doktora Franje Tuđmana"
      1350 to "Zrakoplovna tehnička škola"
      1355 to "Ljubijska - Hrvatskog proljeća"
      1375 to "Sveučilišna aleja"
      1385 to "Gornji Trpuci, okretište"
      1387 to "Svetog Leopolda Mandića"
      1389 to "Trg doktora Franje Tuđmana"
      1405 to "Selnička - Puđak Kate, okretište"
      1413 to "Turopoljska - Markulini"
      1417 to "Dobrodol - Šimunčevečka"
      1421 to "Šimunčevec - društveni dom"
      1464 to "Sarajevska - Vatikanska"
      1465 to "Sarajevska - Ukrajinska"
      1518 to "Dominika Mandića - Međašni klanac"
      1559 to "Kosirnikova - Jelenovac"
      1536 to "Šercerova - Šercerov prečac"
      1565 to "Kosirnikova - SRC Jelenovac"
      1634 to "Bolnica Jankomir"
      1660 to "Voćarska - društveni dom"
      1662 to "Kozari put I - odvojak V"
      1666 to "Ljudevita Posavskog - Kelekova"
      1667 to "144. brigade - Ljudevita Posavskog"
      1670 to "Rimski put - dječje igralište"
      1672 to "Ljudevita Posavskog - Rimski put"
      1673 to "Vrapče - željeznička stanica"
      1726 to "Planinarski dom Grafičar"
      1727 to "Apartmanska kuća Snježna kraljica"
      1741 to "Željeznička - Vukomerička"
      1742 to "Gradići, kralja Tomislava 46"
      1743 to "Gradići, kralja Tomislava 57"
      1774 to "Slavonska - Ljudevita Posavskog"
      1849 to "Trg bana Josipa Jelačića"
      1870 to "Kupljenski Hruševec - škola"
      1872 to "Kupljenski Hruševec - okretište"
      1903 to "Markovo Polje - mrtvačnica"
      1904 to "Markovo Polje - parkiralište"
      1905 to "Markovo Polje - istočni ulaz"
      1906 to "Markovo Polje - okretište"
      1919 to "Branimirova - Škrnjugova"
      1923 to "Tišinska ulica III. odvojak"
      1971 to "Gimnazija Lucijana Vranjanin"
      1999 to "Zagrebački velesajam"
      2025 to "Branimirova - Zagrebačka"
      2028 to "Ulica Kaktusa 34"
      2029 to "Mjesni odbor Novo Brestje"
      2031 to "Ulica Kaktusa 47"
      2036 to "Planinarski dom Runolist"
      2065 to "Zelena magistrala - Rušiščak"
      2070 to "Savski nasip Park & Ride"
      2103 to "Prilaz baruna Filipovića"
   }

   public fun giveMeTheExtraKeywordForStop(stopNumber: StopNumber): String? =
      extraStopKeywords[stopNumber]

   /*@Suppress("unused") // Used for testing the icon info, TODO should probably be moved someplace else...
   public fun testLabels(stops: Stops, routesAtStops: RoutesAtStopMap) {
      for (stop in stops.list) {
         if (stop.code !in 0..4 && stop.code !in 21..24 && stop.id.value !in stopLabels && stop.id.value !in iconInfo) {
            val routesAtStop = routesAtStops[stop.id.value]!!
            if (!routesAtStop.last && !routesAtStop.first)
               Log.i("LabelTest", "Stop ${stop.name} (${stop.id}) invalid")
         }
      }
   }*/

   private val specialTripLabels =
      HashMap<RouteId, Array<out Pair<Int, Pair<String?, String?>>>>().apply {

         operator fun Int.get(
            vararg labelConditions: Pair<Int, Pair<String?, String?>>
         ) {
            put(this.toString(), labelConditions)
         }

         infix fun Pair<String?, String?>.ifStop(stopId: String) =
            stopId.toStopId().rawValue to this

         infix fun Pair<String?, String?>.unlessStop(stopId: String) =
            -stopId.toStopId().rawValue to this

         132[
            "po Jadranskoj aveniji" to null ifStop "635_23", // Jadranska av.-Arena (sjeverno)
            null to "po Jadranskoj aveniji" ifStop "635_24", // Jadranska av.-Arena (juzno)
         ]
         140[null to "preko A.K. Snježna Kraljica" ifStop "1727_23"] // A.K.Snjezna Kraljica
         162["ne vozi preko Gajana" to null unlessStop "719_24"] // Gajani
         172[
            // Every trip stops at that stop, so the label is unnecessary
            //null to "Terminal" ifStop "161_22", // Zapresic (peron 7)
            "staje na Vrapčanskoj" to null ifStop "135_22", // Vrapcanska (zapadno)
         ]
         // Every trip stops at that stop, so the label is unnecessary
         /*174[
            null to "preko Kupljenskog Hruševca" ifStop "1916_21", // Hrusevecka (istocno)
            "preko Kupljenskog Hruševca" to null ifStop "1916_22", // Hrusevecka (istocno)
         ]*/
         182[null to "preko Groblja Zaprešić" ifStop "1641_22"] // Groblje Zapresic
         212[null to "ne vozi preko Selčine" unlessStop "1028_21"] // Trg Lovre Matacica
         220["preko Travnog" to null ifStop "1093_23"] // Božidara Magovca 111
         229[
            "preko Sloboštine" to null ifStop "1086_22", // Slobostina (zapadno)
            null to "preko Sloboštine" ifStop "1086_21", // Slobostina (istocno)
         ]
         268[
            "preko S.R. Njemačke" to null ifStop "565_24", // Islandska (juzno)
            null to "preko S.R. Njemačke" ifStop "565_22", // Islandska (zapadno)
         ]
         269[
            "staje na Maksimirskim naseljima" to null ifStop "1133_21", // Maksimirska naselja (istocno)
            null to "staje na Maksimirskim naseljima" ifStop "1133_22", // Maksimirska naselja (zapadno)
         ]
         276[null to "preko IKEA-e" ifStop "1889_23"] // IKEA
         284[
            null to "preko Sesvetske Selnice" ifStop "1404_23", // Ferde Kocha (sjeverno)
            "preko Sesvetske Selnice" to null ifStop "1404_24", // Ferde Kocha (juzno)
         ]
         // These lines no longer exist
         /*302[null to "preko Ključić brda" ifStop "1839_22"] // Kljucic brdo
         304[
            null to "preko Sisačke ceste" ifStop "874_21", // Sisacka - Mraclinska (istocno)
            "preko Sisačke ceste" to null ifStop "874_22", // Sisacka - Mraclinska (zapadno)
         ]*/
         307[null to "preko Sasa" ifStop "943_23"] // Sasi, okretiste (sjeverno)
         // These lines no longer exist
         /*321[
            null to "preko Sasa" ifStop "943_23", // Sasi, okretiste (sjeverno)
            null to "preko Zabrebačke" ifStop "1201_22", // Zagrebacka 42
         ]
         335["ne vozi preko Kurilovca" to null unlessStop "1298_22"] // Kolodvorska 76*/
      }

   public fun giveMeTheSpecialTripLabel(trip: Trip): Pair<String?, String?>? =
      specialTripLabels[trip.route.id]?.let { conditions ->
         for ((stopId, label) in conditions) {
            if (stopId >= 0) {
               if (trip.stops.any { it.id.rawValue == stopId })
                  return@let label
            } else if (label[!trip.directionId] != null &&
               trip.stops.none { it.id.rawValue == -stopId }
            )
               return@let label
         }
         null
      }

   public const val NULL_SERVICE_ID_MESSAGE: String =
      "Ne postoji vozni red za izabrani datum.\nPokušajte se spojiti na " +
            "internet, ako već niste, kako bi se preuzela najnovija inačica rasporeda."

   public const val NO_TRIPS_219_MESSAGE: String =
      "Polaske subotom, nedjeljom i praznikom ostvaruje autobus linije 229 koji " +
            "na Glavnom kolodvoru polazi s perona 10 na Koturaškoj cesti."

   public fun giveMeTheSpecialLabelForNoTrips(
      route: Route,
      serviceId: ServiceId?,
      selectedDate: Long,
      serviceTypes: ServiceTypes?,
   ): String {
      if (serviceId == null)
         return NULL_SERVICE_ID_MESSAGE

      val serviceType =
         if (serviceTypes != null)
            serviceTypes[serviceId] ?: return NULL_SERVICE_ID_MESSAGE
         else ServiceType.ofDate(selectedDate)

      // Route 219 gets a special label.
      if ((serviceType == ServiceType.SATURDAY ||
               serviceType == ServiceType.SUNDAY) && route.id == "219"
      ) return NO_TRIPS_219_MESSAGE

      if (route.trips.values.any { it.serviceId == serviceId })
         return "Linija nema više polazaka danas."

      return when (serviceType) {
         ServiceType.WEEKDAY -> "Linija nema polazaka na izabrani datum."
         ServiceType.SATURDAY -> "Linija ne vozi vikendom i praznicima."
         ServiceType.SUNDAY -> {
            val saturdayServiceId = serviceTypes?.entries
               ?.firstOrNull { it.value == ServiceType.SATURDAY }?.key

            if (saturdayServiceId != null &&
               route.trips.values.none { it.serviceId == saturdayServiceId }
            ) "Linija ne vozi vikendom i praznicima."
            else "Linija ne vozi nedjeljom i praznicima."
         }
      }
   }

   public fun giveMeTheSpecialLabelForNoTrips(
      routes: Collection<Route>,
      filterEmpty: Boolean,
      serviceId: ServiceId?,
      selectedDate: Long,
      serviceTypes: ServiceTypes?,
   ): String {
      if (routes.isEmpty()) return ""

      if (routes.size == 1)
         return giveMeTheSpecialLabelForNoTrips(
            routes.first(), serviceId, selectedDate, serviceTypes
         )

      if (serviceId == null)
         return NULL_SERVICE_ID_MESSAGE

      val serviceType =
         if (serviceTypes != null)
            serviceTypes[serviceId] ?: return NULL_SERVICE_ID_MESSAGE
         else ServiceType.ofDate(selectedDate)

      if (routes.any { route -> route.trips.values.any { it.serviceId == serviceId } })
         return if (filterEmpty) "Na postaji nema više polazaka danas."
         else "Na postaji nema više polazaka danas za izabrane linije."

      return when (serviceType) {
         ServiceType.WEEKDAY -> "Linije nemaju polazaka na izabrani datum."
         ServiceType.SATURDAY -> "Linije ne voze vikendom i praznicima."
         ServiceType.SUNDAY -> {
            val saturdayServiceId = serviceTypes?.entries
               ?.firstOrNull { it.value == ServiceType.SATURDAY }?.key

            if (saturdayServiceId != null &&
               routes.all { route ->
                  route.trips.values.none { it.serviceId == saturdayServiceId }
               }
            ) "Linije ne voze vikendom i praznicima."
            else "Linije ne voze nedjeljom i praznicima."
         }
      }
   }

   public fun giveMeTheServiceIdTypes(
      routes: Routes,
      calendarDates: CalendarDates,
   ): ServiceTypes {
      val serviceIds = calendarDates.serviceIds
      // I've selected 108 as the route with only weekday travels
      val tripsOfRouteWeekdaysOnly = routes["108"]?.trips
         ?: return emptyMap()
      // I've selected 159 as the route with weekday and saturday travels
      val tripsOfRouteWeekdaysAndSaturday = routes["159"]?.trips
         ?: return emptyMap()

      return giveMeTheServiceIdTypes(
         serviceIds,
         tripsOfRouteWeekdaysOnly,
         tripsOfRouteWeekdaysAndSaturday
      )
   }

   public fun giveMeTheServiceIdTypes(
      serviceIds: Iterator<ServiceId>,
      tripsOfRouteWeekdaysOnly: Trips,
      tripsOfRouteWeekdaysAndSaturday: Trips,
   ): ServiceTypes {
      val map = HashMap<ServiceId, ServiceType>()

      for (serviceId in serviceIds) {
         map[serviceId] =
            if (tripsOfRouteWeekdaysOnly.values
                  .any { it.serviceId == serviceId }
            )
            // There exist trips of this route on this service id.
            // Since this route drives on weekdays only, this must be a weekday.
               ServiceType.WEEKDAY
            else if (tripsOfRouteWeekdaysAndSaturday.values
                  .any { it.serviceId == serviceId }
            )
            // There exist trips of this route on this service id.
            // Since this route drives on weekdays and saturdays,
            // and it is not a weekday, this must be a saturday.
               ServiceType.SATURDAY
            else
            // There are no trips on the route that drives on weekdays
            // and saturdays, thus this must be a sunday (or a holiday).
               ServiceType.SUNDAY
      }

      return map
   }

   public fun giveMeTheForcedCommonHeadsign(
      routeId: RouteId
   ): Pair<String?, String?>? = when (routeId) {
      "217" -> "Petruš. nas." to null
      "295" -> "Sajam Jakuševec" to null
      else -> null
   }

   private infix fun StopId.to(other: StopId) =
      IntIntPair(this.rawValue, other.rawValue)

   public fun giveMeTheForcedFirstStop(routeId: RouteId): IntIntPair =
      when (routeId) {
         "217" -> StopId.Invalid to StopId(1081, 21)
         "295" -> StopId.Invalid to StopId(1230, 22)
         else -> StopId.Invalid to StopId.Invalid
      }

   /*
   TODO display which routes have the ability to carry bikes.
   It would be too much to check each specific trip since it would need
   to be manually checked, rather just show that the routes can carry
   bikes and tell the user to check the official route schedule for specifics.
   routes: 102, 103, 140
    */

   private val betterStopMapper = HashMap<StopId, (Stop) -> List<Stop>>()

   private val routeToBetterStopMapper = HashMap<StopId, Map<RouteId, StopId>>()

   private class BetterStopMapper(
      val routeStopMapper: Array<out Pair<StopCode, Set<RouteId>>>
   ) : (Stop) -> List<Stop> {
      override fun invoke(stop: Stop): List<Stop> {
         val remainingRoutes = stop.routes.keys.mapTo(mutableSetOf()) { it.id }
         val stops = ArrayList<Stop>()
         for ((newStopCode, mappedRoutes) in routeStopMapper) {
            stops += stop.copy(newStopCode, mappedRoutes)
            remainingRoutes -= mappedRoutes
         }
         if (remainingRoutes.isNotEmpty())
            stops += stop.copy(stop.routes.filterKeys { it.id in remainingRoutes })
         return stops
      }
   }

   private fun putBetterStopMapping(
      stopIdToTransform: String,
      vararg routeStopMapper: Pair<StopCode, Set<RouteId>>
   ) {
      val stopId = stopIdToTransform.toStopId()
      val stopNumber = stopId.stopNumber
      betterStopMapper[stopId] = BetterStopMapper(routeStopMapper)
      val betterRouteMap = mutableMapOf<RouteId, StopId>()
      for ((newStopCode, mappedRoutes) in routeStopMapper) {
         for (routeId in mappedRoutes)
            betterRouteMap[routeId] = StopId(stopNumber, newStopCode)
      }
      routeToBetterStopMapper[stopId] = betterRouteMap
   }

   internal fun makeMeABetterStopMapper() {
      putBetterStopMapping(
         stopIdToTransform = "99_32",
         101 to setOf("136"),
         103 to setOf("122", "123", "131"),
         104 to setOf("119", "120"),
         105 to setOf("117", "134"),
         109 to setOf("124", "130"),
         110 to setOf("176", "177"),
      )
      putBetterStopMapping(
         stopIdToTransform = "205_51",
         103 to setOf("261", "262"),
         104 to setOf("263"),
         106 to setOf("267", "280"),
         108 to setOf("264", "271", "272"),
         109 to setOf("270", "273"),
         110 to setOf("274"),
      )
      // TODO merge Sopot Izlaz 1794_12 to obicni sopot
      // todo isto i za zaprude zitnjak maksimir
   }

   internal fun releaseTheBetterStopMapper() {
      betterStopMapper.clear()
      routeToBetterStopMapper.clear()
   }

   internal fun giveMeBetterStops(stop: Stop): List<Stop>? {
      return betterStopMapper[stop.id]?.invoke(stop)
   }

   internal fun redirectMeToTheBetterStopId(forRoute: RouteId, stopId: StopId): StopId {
      return routeToBetterStopMapper[stopId]?.get(forRoute) ?: stopId
   }

   init {
      // TODO move to GtfsScheduleLoader when everything else is moved there
      makeMeABetterStopMapper()
   }

}
