package cl.duoc.esucar.core.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chileanLocale = Locale("es", "CL")

private val shortDateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM, hh:mm a", chileanLocale)

private val longDateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", chileanLocale)

private val uploadedDateFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy", chileanLocale)

fun formatShortDateTime(fecha: String): String =
    runCatching { LocalDateTime.parse(fecha).format(shortDateTimeFormatter) }
        .getOrElse { fecha }

fun formatLongDateTime(fecha: String): String =
    runCatching { LocalDateTime.parse(fecha).format(longDateTimeFormatter) }
        .getOrElse { fecha }

fun formatUploadedDate(date: String): String =
    runCatching { LocalDate.parse(date).format(uploadedDateFormatter) }
        .getOrElse { date }

fun nowTimestamp(): String = LocalDateTime.now().withNano(0).toString()

fun todayDate(): String = LocalDate.now().toString()
