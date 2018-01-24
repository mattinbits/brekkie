package com.nordea.commonplatforms

import org.scalatest.Matchers
import org.scalatest.WordSpec
import java.time._
import java.time.temporal.{ChronoUnit, TemporalUnit}

class FridaysSpec extends WordSpec with Matchers {

  "Fridays" should {

    "Return number of days until Friday" in {
      val input = Seq(
        "2017-12-01",
        "2017-12-02",
        "2017-12-03",
        "2017-12-04",
        "2017-12-05",
        "2017-12-06",
        "2017-12-07",
        "2017-12-08")
      val output = input.map(d => Fridays.daysUntilFriday(LocalDate.parse(d)))
      output shouldBe Seq(0,6,5,4,3,2,1,0)
    }

    "Return days since Friday" in {
      val input = Seq(
        "2017-12-01",
        "2017-12-02",
        "2017-12-03",
        "2017-12-04",
        "2017-12-05",
        "2017-12-06",
        "2017-12-07",
        "2017-12-08")
      val output = input.map(d => Fridays.daysSinceFriday(LocalDate.parse(d)))
      output shouldBe Seq(7,1,2,3,4,5,6,7)
    }

    "Return the next several Fridays" in {
      Fridays.next(LocalDate.parse("2017-11-28"), 2) shouldEqual Seq(
        LocalDate.parse("2017-12-01"),
        LocalDate.parse("2017-12-08")
      )
    }

    "Return previous several Fridays" in {
      Fridays.previous(LocalDate.parse("2017-12-08"), 3) shouldEqual Seq(
        LocalDate.parse("2017-11-17"),
        LocalDate.parse("2017-11-24"),
        LocalDate.parse("2017-12-01")
      )

      Fridays.previous(LocalDate.parse("2017-12-09"), 3) shouldEqual Seq(
        LocalDate.parse("2017-11-24"),
        LocalDate.parse("2017-12-01"),
        LocalDate.parse("2017-12-08")
      )
    }

    "Return a range of Fridays" in {
      Fridays.rangeFrom(LocalDate.parse("2017-12-09"), 3, 2) shouldEqual Seq(
        LocalDate.parse("2017-11-24"),
        LocalDate.parse("2017-12-01"),
        LocalDate.parse("2017-12-08"),
        LocalDate.parse("2017-12-15"),
        LocalDate.parse("2017-12-22")
      )
    }

    "Return the number of weeks from now until now" in {
      val date = LocalDate.parse("2017-12-22")
      Fridays.numberInstancesAway(date, date) shouldBe 1
    }

    "Return the number of weeks from now until a future Friday" in {
      val date = LocalDate.parse("2017-12-08")
      val future = LocalDate.parse("2017-12-29")
      val actual =
        (1 to 21)
          .map(date.plus(_, ChronoUnit.DAYS))
          .map(Fridays.numberInstancesAway(_, future))
      val fill7 = Seq.fill[Int](7) _
      val expected = fill7(3) ++ fill7(2) ++ fill7(1)
      actual shouldBe expected
    }
  }
}