package rs.raf.banka2.mobile.core.format

/**
 * Helperi za formatiranje racuna i kartica koji se ponavljaju kroz UI.
 */
object AccountFormatter {

    /** Formatira broj racuna u "XXX-XXXXXXXXXXXX-XX" stil. */
    fun formatAccountNumber(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val cleaned = raw.replace("-", "").replace(" ", "")
        return when {
            cleaned.length >= 17 -> "${cleaned.substring(0, 3)}-${cleaned.substring(3, 15)}-${cleaned.substring(15)}"
            cleaned.length >= 13 -> "${cleaned.substring(0, 3)}-${cleaned.substring(3, cleaned.length - 2)}-${cleaned.substring(cleaned.length - 2)}"
            else -> cleaned
        }
    }

    /** Maskira broj kartice — prikazuje samo poslednje 4 cifre. */
    fun maskCardNumber(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val digits = raw.replace(" ", "").replace("-", "")
        if (digits.length < 4) return digits
        val last4 = digits.takeLast(4)
        return "•••• •••• •••• $last4"
    }

    /** Vraca prve 3 cifre racuna — routing number za inter-bank detekciju. */
    fun routingPrefix(accountNumber: String?): String? {
        if (accountNumber.isNullOrBlank()) return null
        val cleaned = accountNumber.replace("-", "").replace(" ", "")
        return cleaned.takeIf { it.length >= 3 }?.take(3)
    }
}
