package hr.squidpai.zetlive

import android.app.Application
import hr.squidpai.zetlive.gtfs.ScheduleManager
import hr.squidpai.zetlive.ui.NotificationTrackerService
import java.io.File

class MainApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    val filesDir = filesDir
    val dataFile = File(filesDir, "data.json")

    Data.load(dataFile)

    ScheduleManager.init(filesDir)
    ScheduleManager.realtimeDispatcher.start()

    NotificationTrackerService.createNotificationChannel(this)
  }

}