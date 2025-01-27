package hr.squidpai.zetapi

import hr.squidpai.zetapi.cached.CachedScheduleIO
import hr.squidpai.zetapi.gtfs.GtfsScheduleLoader

/**
 * Object containing all the information about the schedule.
 *
 * Use [GtfsScheduleLoader] to download and load the schedule,
 * and then, if needed, save it using [CachedScheduleIO] to
 * quickly load only parts of the schedule.
 */
public class Schedule internal constructor(
   public val feedInfo: FeedInfo,
   public val routes: Routes,
   public val stops: Stops,
   public val shapes: Shapes,
   public val calendarDates: CalendarDates,
   public val serviceTypes: ServiceTypes,
)
