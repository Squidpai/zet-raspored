package hr.squidpai.zetapi.realtime

import com.google.transit.realtime.GtfsRealtime.FeedMessage
import hr.squidpai.zetapi.TripId
import java.net.URL

public interface RealtimeDispatcher {

   public fun getForTrip(tripId: TripId): RealtimeData

   public companion object {
      public const val LINK: String = "https://www.zet.hr/gtfs-rt-protobuf"

      public fun download(): FeedMessage =
         FeedMessage.parseFrom(URL(LINK).openStream())
   }

}
