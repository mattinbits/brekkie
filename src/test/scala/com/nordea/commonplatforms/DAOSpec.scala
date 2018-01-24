package com.nordea.commonplatforms

import java.time.LocalDate

import com.nordea.commonplatforms.DAO.Breakfast
import com.typesafe.config.ConfigFactory
import org.scalatest.Matchers
import org.scalatest.WordSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class DAOSpec extends WordSpec with Matchers {

  "DAO" should {

    "Calculate id from date" in {
      Seq("2016-01-01", "2017-02-02", "2017-12-31")
        .map(LocalDate.parse)
        .map(DAO.toDbId) shouldBe Seq(20160101, 20170202, 20171231)
    }

    "Calculate date from id" in {
      Seq(20160101, 20170202, 20171231)
        .map(DAO.fromDbId) shouldBe Seq("2016-01-01", "2017-02-02", "2017-12-31").map(LocalDate.parse)
    }

    def createConfig(dbName: String) = ConfigFactory.parseString(
      s"""
        |brekkie.db.url = "jdbc:h2:mem:${dbName};DB_CLOSE_DELAY=-1"
      """.stripMargin
    ).withFallback(ConfigFactory.load())

    def wait[T](f: Future[T]): T = Await.result(f, Duration.Inf)

    "Add a breakfast entry" in {
      val conf = createConfig("add")
      val dao = DAO.createWithFlyway(conf).get
      val before = wait(dao.allBreakfastFrom(LocalDate.parse("1900-01-01")))
      before shouldBe empty
      val newBrek = Breakfast(LocalDate.parse("2017-12-08"), Some("Genghis Khan"))
      wait(dao.addBreakfast(newBrek))
      val after = wait(dao.allBreakfastFrom(LocalDate.parse("1900-01-01")))
      after shouldBe Seq(newBrek)
    }

    "Add a breakfast with no Assignee" in {
      val conf = createConfig("withNull")
      val dao = DAO.createWithFlyway(conf).get
      val before = wait(dao.allBreakfastFrom(LocalDate.parse("1900-01-01")))
      before shouldBe empty
      val newBrek = Breakfast(LocalDate.parse("2017-12-08"), None)
      wait(dao.addBreakfast(newBrek))
      val after = wait(dao.allBreakfastFrom(LocalDate.parse("1900-01-01")))
      after shouldBe Seq(newBrek)
    }

    "Exclude breakfasts before stated date" in {
      val conf = createConfig("before")
      val dao = DAO.createWithFlyway(conf).get
      val before = wait(dao.allBreakfastFrom(LocalDate.parse("1900-01-01")))
      before shouldBe empty
      val olderBrek = Breakfast(LocalDate.parse("2017-12-08"), Some("Genghis Khan"))
      wait(dao.addBreakfast(olderBrek))
      val newerBrek = Breakfast(LocalDate.parse("2017-12-15"), Some("Barry Khan"))
      wait(dao.addBreakfast(newerBrek))
      wait(dao.allBreakfastFrom(LocalDate.parse("2017-12-08"))) shouldBe Seq(olderBrek, newerBrek)
      wait(dao.allBreakfastFrom(LocalDate.parse("2017-12-09"))) shouldBe Seq(newerBrek)
      wait(dao.allBreakfastFrom(LocalDate.parse("2017-12-15"))) shouldBe Seq(newerBrek)
      wait(dao.allBreakfastFrom(LocalDate.parse("2017-12-16"))) shouldBe empty
    }

    "Return None for 'latest assigned' when DB is empty" in {
      val conf = createConfig("latest-empty")
      val dao = DAO.createWithFlyway(conf).get
      wait(dao.latestAssignedBreakfastOnOrAfter(LocalDate.parse("2017-12-08"))) shouldBe None
    }

    "Return None for 'latest assigned' when no late enough entries" in {
      val conf = createConfig("latest-too-early")
      val dao = DAO.createWithFlyway(conf).get
      val brek = Breakfast(LocalDate.parse("2017-12-01"), Some("Genghis Khan"))
      wait(dao.addBreakfast(brek))
      wait(dao.latestAssignedBreakfastOnOrAfter(LocalDate.parse("2017-12-08"))) shouldBe None
    }

    "Return None for 'latest assigned' when only unassigned are later" in {
      val conf = createConfig("latest-unassigned")
      val dao = DAO.createWithFlyway(conf).get
      val olderBrek = Breakfast("2017-12-01", "Genghis Khan")
      val newerBrek = Breakfast("2017-12-15")
      wait(dao.addBreakfast(olderBrek))
      wait(dao.addBreakfast(newerBrek))
      wait(dao.latestAssignedBreakfastOnOrAfter(LocalDate.parse("2017-12-08"))) shouldBe None
    }

    "Return when latest is today" in {
      val conf = createConfig("latest-today")
      val dao = DAO.createWithFlyway(conf).get
      val brek = Breakfast(LocalDate.parse("2017-12-01"), Some("Genghis Khan"))
      wait(dao.addBreakfast(brek))
      wait(dao.latestAssignedBreakfastOnOrAfter(LocalDate.parse("2017-12-01"))) shouldBe Some(brek)
    }

    "Return the latest when multiple are later" in {
      val conf = createConfig("multi-later")
      val dao = DAO.createWithFlyway(conf).get
      val olderBrek = Breakfast("2017-12-08", "Genghis Khan")
      val newerBrek = Breakfast("2017-12-15", "Donald Trump")
      wait(dao.addBreakfast(olderBrek))
      wait(dao.addBreakfast(newerBrek))
      wait(dao.latestAssignedBreakfastOnOrAfter(LocalDate.parse("2017-12-01"))) shouldBe Some(newerBrek)
    }

    "Fill from an empty DB" in {
      val conf = createConfig("fill-empty")
      val dao = DAO.createWithFlyway(conf).get
      val resultFromAFriday = wait(dao.fill(LocalDate.parse("2017-12-01"), 2, 3))
      resultFromAFriday shouldBe Seq(
        Breakfast("2017-11-17"),
        Breakfast("2017-11-24"),
        Breakfast("2017-12-01"),
        Breakfast("2017-12-08"),
        Breakfast("2017-12-15")
      )
      val resultFromNotAFriday = wait(dao.fill(LocalDate.parse("2017-11-30"), 2, 3))
      resultFromNotAFriday shouldBe Seq(
        Breakfast("2017-11-17"),
        Breakfast("2017-11-24"),
        Breakfast("2017-12-01"),
        Breakfast("2017-12-08"),
        Breakfast("2017-12-15")
      )
    }

    "Fill from a partially populated DB where fill is longer" in {
      val conf = createConfig("fill-partial-1")
      val dao = DAO.createWithFlyway(conf).get
      val olderBrek = Breakfast("2017-11-24", "Genghis Khan")
      val newerBrek = Breakfast("2017-12-08", "Donald Trump")
      wait(dao.addBreakfast(olderBrek))
      wait(dao.addBreakfast(newerBrek))
      val resultFromAFriday = wait(dao.fill(LocalDate.parse("2017-12-01"), 2, 3))
      resultFromAFriday shouldBe Seq(
        Breakfast("2017-11-17"),
        Breakfast("2017-11-24", "Genghis Khan"),
        Breakfast("2017-12-01"),
        Breakfast("2017-12-08", "Donald Trump"),
        Breakfast("2017-12-15")
      )
      val resultFromNotAFriday = wait(dao.fill(LocalDate.parse("2017-11-30"), 2, 3))
      resultFromNotAFriday shouldBe Seq(
        Breakfast("2017-11-17"),
        Breakfast("2017-11-24", "Genghis Khan"),
        Breakfast("2017-12-01"),
        Breakfast("2017-12-08", "Donald Trump"),
        Breakfast("2017-12-15")
      )
    }

    "Fill from a partially populated DB where db is longer" in {
      val conf = createConfig("fill-partial-2")
      val dao = DAO.createWithFlyway(conf).get
      val olderBrek = Breakfast("2017-11-24", "Genghis Khan")
      val newerBrek = Breakfast("2017-12-08", "Donald Trump")
      val newestBrek = Breakfast("2017-12-22", "Amelia Earnhart")
      wait(dao.addBreakfast(olderBrek))
      wait(dao.addBreakfast(newerBrek))
      wait(dao.addBreakfast(newestBrek))
      val resultFromAFriday = wait(dao.fill(LocalDate.parse("2017-12-01"), 2, 3))
      resultFromAFriday shouldBe Seq(
        Breakfast("2017-11-17"),
        Breakfast("2017-11-24", "Genghis Khan"),
        Breakfast("2017-12-01"),
        Breakfast("2017-12-08", "Donald Trump"),
        Breakfast("2017-12-15"),
        Breakfast("2017-12-22", "Amelia Earnhart")
      )
      val resultFromNotAFriday = wait(dao.fill(LocalDate.parse("2017-11-28"), 2, 3))
      resultFromNotAFriday shouldBe Seq(
        Breakfast("2017-11-17"),
        Breakfast("2017-11-24", "Genghis Khan"),
        Breakfast("2017-12-01"),
        Breakfast("2017-12-08", "Donald Trump"),
        Breakfast("2017-12-15"),
        Breakfast("2017-12-22", "Amelia Earnhart")
      )
    }

    "Fill from a fully populated db" in {
      val conf = createConfig("fill-full")
      val dao = DAO.createWithFlyway(conf).get
      val inserts = Seq(
        Breakfast("2017-11-10", "User 1"),
        Breakfast("2017-11-17", "User 2"),
        Breakfast("2017-11-24", "User 3"),
        Breakfast("2017-12-01", "User 4"),
        Breakfast("2017-12-08", "User 5"),
        Breakfast("2017-12-15", "User 6"),
        Breakfast("2017-12-22", "User 7"),
        Breakfast("2017-12-29", "User 8")
      )
      inserts.foreach(b => wait(dao.addBreakfast(b)))
      val resultFromAFriday = wait(dao.fill(LocalDate.parse("2017-12-01"), 2, 3))
      resultFromAFriday shouldBe Seq(
        Breakfast("2017-11-17", "User 2"),
        Breakfast("2017-11-24", "User 3"),
        Breakfast("2017-12-01", "User 4"),
        Breakfast("2017-12-08", "User 5"),
        Breakfast("2017-12-15", "User 6"),
        Breakfast("2017-12-22", "User 7"),
        Breakfast("2017-12-29", "User 8")
      )
      val resultFromNotAFriday = wait(dao.fill(LocalDate.parse("2017-11-28"), 2, 3))
      resultFromNotAFriday shouldBe Seq(
        Breakfast("2017-11-17", "User 2"),
        Breakfast("2017-11-24", "User 3"),
        Breakfast("2017-12-01", "User 4"),
        Breakfast("2017-12-08", "User 5"),
        Breakfast("2017-12-15", "User 6"),
        Breakfast("2017-12-22", "User 7"),
        Breakfast("2017-12-29", "User 8")
      )
    }
  }
}