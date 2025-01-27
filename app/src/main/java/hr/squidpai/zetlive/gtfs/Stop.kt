package hr.squidpai.zetlive.gtfs

import hr.squidpai.zetapi.Love
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetlive.ui.Symbols

val Stop.label
   get() =
      Love.giveMeTheLabelForStop(id) ?: when {
         routes.values.all { it.isFirst } ->
            "Ulaz ${routes.keys.joinToString { it.shortName }}"

         routes.values.all { it.isLast } ->
            if (routes.size == 1)
               "Izlaz ${routes.keys.single().shortName}"
            else "Izlaz"

         else -> null
      }

val Stop.iconInfo
   get() =
      when (Love.giveMeTheIconCodeForStop(id) % 10) {
         1 -> Symbols.ArrowRightAlt to "istok"
         2 -> Symbols.ArrowLeftAlt to "zapad"
         3 -> Symbols.ArrowUpwardAlt to "sjever"
         4 -> Symbols.ArrowDownwardAlt to "jug"
         else -> null
      }
