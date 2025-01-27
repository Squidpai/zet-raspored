package hr.squidpai.zetapi.realtime

import hr.squidpai.zetapi.TripId

public object EmptyRealtimeDispatcher : RealtimeDispatcher {

   override fun getForTrip(tripId: TripId): RealtimeData = RealtimeData.None

}