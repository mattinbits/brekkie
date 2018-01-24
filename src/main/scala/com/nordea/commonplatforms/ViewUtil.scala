package com.nordea.commonplatforms

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, TextStyle}
import java.time.temporal.{ChronoField, TemporalField}

object ViewUtil {

  def ordinalSuffix(cardinal: Int): String = cardinal match {
    case n if n >= 10 && n <= 20 => "th"
    case n if n % 10 == 1 => "st"
    case n if n % 10 == 2 => "nd"
    case n if n % 10 == 3 => "rd"
    case _ => "th"
  }


  def uiDate(date: LocalDate): String =
    new DateTimeFormatterBuilder()
      .appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL)
      .appendLiteral(' ')
      .appendText(ChronoField.DAY_OF_MONTH, TextStyle.SHORT)
      .appendLiteral(ordinalSuffix(date.getDayOfMonth))
      .appendLiteral(' ')
      .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL)
      .toFormatter
      .format(date)
}
