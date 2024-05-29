package hr.squidpai.zetlive

import android.app.Application
import hr.squidpai.zetlive.gtfs.Live
import hr.squidpai.zetlive.gtfs.Schedule
import java.io.File
import java.io.FileReader
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MainApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    val filesDir = filesDir
    val dataFile = File(filesDir, "data.json")

    Data.load(dataFile)

    Schedule.init(filesDir)

    Live.initialize()
  }

}