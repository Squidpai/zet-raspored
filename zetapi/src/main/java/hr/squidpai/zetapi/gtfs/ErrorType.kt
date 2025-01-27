package hr.squidpai.zetapi.gtfs

public enum class ErrorType {
   MALFORMED_URL, OPENING_CONNECTION, NO_CONTENT, NO_FILENAME, UP_TO_DATE, ALREADY_DOWNLOADING,
   DOWNLOAD_ERROR;

   public val errorMessage: String?
      get() = when (this) {
         ALREADY_DOWNLOADING -> null
         MALFORMED_URL ->
            "Nije moguće spojiti se na ZET-ovu stranicu. Provjerite svoju internet konekciju."

         OPENING_CONNECTION -> "Dogodila se greška prilikom spajanja na ZET-ovu stranicu. " +
               "Provjerite svoju internet konekciju."

         NO_CONTENT, NO_FILENAME -> "Nije moguće preuzeti raspored sa ZET-ove stranice."
         UP_TO_DATE -> "Već je preuzet najnoviji raspored."
         DOWNLOAD_ERROR -> "Dogodila se greška prilikom preuzimanja novog rasporeda. " +
               "Provjerite svoju internet konekciju."
      }
}