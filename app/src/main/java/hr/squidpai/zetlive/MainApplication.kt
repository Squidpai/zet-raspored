package hr.squidpai.zetlive

import android.app.Application
import hr.squidpai.zetlive.gtfs.Live
import hr.squidpai.zetlive.gtfs.Schedule
import hr.squidpai.zetlive.ui.NotificationTrackerService
import java.io.File
import kotlin.concurrent.thread

class MainApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    thread {
      NotificationTrackerService.createNotificationChannel(this)

      val filesDir = filesDir
      val dataFile = File(filesDir, "data.json")

      Data.load(dataFile)

      Schedule.init(filesDir)

      Live.initialize()
    }
  }

}