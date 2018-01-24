package com.nordea.commonplatforms

import java.time._
import java.time.temporal.ChronoUnit

object Fridays {

  def rangeFrom(today: LocalDate, prevNum: Int, nextNum: Int): Seq[LocalDate] =
    previous(today, prevNum) ++ next(today, nextNum)

  def numberInstancesAway(today: LocalDate, futureDate: LocalDate): Int = {
    ChronoUnit.DAYS.between(today, futureDate).toInt match {
      case 0 => 1
      case x if x > 0 => (x + 7) / 7
      case _ => throw new IllegalArgumentException("today may not be later than future")
    }
  }

  //Next n Fridays, including today if today is Friday
  def next(today: LocalDate, number: Int): Seq[LocalDate] = {
    require(number > 0)
    val daysUntilNextFriday = daysUntilFriday(today)
    (0 until number).map(n => today.plusDays(n*7 + daysUntilNextFriday))
  }

  //Previous n Fridays, excluding today if today is Friday
  def previous(today: LocalDate, number: Int): Seq[LocalDate] = {
    require(number > 0)
    val daysSinceLastFriday = daysSinceFriday(today)
    (0 until number).map(n => today.minusDays(n*7 + daysSinceLastFriday)).sortBy(_.toString)
  }

  val adjustment = 7 - DayOfWeek.FRIDAY.getValue

  def daysUntilFriday(day: LocalDate): Int = {
    val adjustedDoW = (day.getDayOfWeek.getValue + adjustment) % 7
    (7 - adjustedDoW) % 7
  }

  def daysSinceFriday(day: LocalDate): Int = {
    val adjustedDoW = (day.getDayOfWeek.getValue + adjustment) % 7
    if(adjustedDoW == 0) 7 else adjustedDoW
  }
}