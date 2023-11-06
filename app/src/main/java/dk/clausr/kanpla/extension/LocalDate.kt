package dk.clausr.kanpla.extension

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

fun LocalDate.getNameOfDay(): String {
    return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
}
