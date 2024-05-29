package hr.squidpai.zetlive

import com.google.transit.realtime.GtfsRealtime
import org.junit.Test
import java.net.URL

class PrintLiveData {

  @Test
  fun printLiveData() {
    println(GtfsRealtime.FeedMessage.parseFrom(URL("https://www.zet.hr/gtfs-rt-protobuf").openStream()))
  }
}