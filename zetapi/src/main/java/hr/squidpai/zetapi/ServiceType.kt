package hr.squidpai.zetapi

public enum class ServiceType {
   WEEKDAY, SATURDAY, SUNDAY;

   public companion object {
      public fun ofDate(date: Long): ServiceType =
         // January 1st, 1970 (day 0) was a thursday.
         // Offset it by three days and mod by 7 since weekdays are
         // independent of any other time frame.
         when ((date + 3) % 7) {
            5L -> SATURDAY
            6L -> SUNDAY
            else -> WEEKDAY
         }
   }
}

public typealias ServiceTypes = Map<ServiceId, ServiceType>
