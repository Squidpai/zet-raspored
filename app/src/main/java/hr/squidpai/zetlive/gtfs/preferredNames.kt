package hr.squidpai.zetlive.gtfs

import hr.squidpai.zetapi.Route
import hr.squidpai.zetapi.ServiceId
import hr.squidpai.zetapi.Stop
import hr.squidpai.zetapi.Trip
import hr.squidpai.zetlive.Data

val Route.preferredName: String
    get() = if (Data.useFullRouteNames) fullName else longName

val Stop.preferredName: String
    get() = if (Data.useFullStopNames) fullName else name

val Trip.preferredHeadsign: String
    get() = if (Data.useFullHeadsigns) fullHeadsign else headsign

val Route.preferredCommonHeadsigns: Map<ServiceId, Pair<String, String>>
    get() = if (Data.useFullHeadsigns) fullCommonHeadsigns else commonHeadsigns
