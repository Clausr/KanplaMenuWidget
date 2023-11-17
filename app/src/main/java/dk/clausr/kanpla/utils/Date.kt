package dk.clausr.kanpla.utils

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.LocalDate
import java.time.LocalTime

fun getDesiredDate(): LocalDate {
    val now = LocalTime.now()
    val today = LocalDate.now()

    return when {
        today.dayOfWeek == DayOfWeek.SATURDAY -> today.plusDays(2)
        today.dayOfWeek == DayOfWeek.SUNDAY -> today.plusDays(1)
        now.isBefore(LocalTime.of(12, 0)) -> today
        now.isAfter(LocalTime.of(12, 0)) -> if (today.dayOfWeek == FRIDAY) {
            today.plusDays(3)
        } else {
            today.plusDays(1)
        }

        else -> today
    }
}
