package com.nordea.commonplatforms

import java.time.LocalDate

import com.nordea.commonplatforms.DAO.Breakfast
import com.typesafe.config._
import com.typesafe.scalalogging.LazyLogging
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import org.flywaydb.core.Flyway

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

trait DAO {

  def allBreakfastFrom(start: LocalDate): Future[Seq[Breakfast]]

  def addBreakfast(breakfast: Breakfast): Future[Unit]

  def latestAssignedBreakfastOnOrAfter(cutOff: LocalDate): Future[Option[Breakfast]]

  def fill(today: LocalDate, from: Int, untilAtLeast: Int): Future[Seq[Breakfast]] = {
    val allFridays = latestAssignedBreakfastOnOrAfter(today).map{brekOpt =>
      val until =
        Math.max(untilAtLeast, brekOpt.map(b => Fridays.numberInstancesAway(today, b.date)).getOrElse(untilAtLeast))
      Fridays.rangeFrom(today, from, until)
    }
    val existingBreakfasts = allBreakfastFrom(Fridays.previous(today, from).head)
    for {
      all <- allFridays
      existing <- existingBreakfasts
    } yield {
      all.foldLeft((Seq.empty[Breakfast], existing)){
        case ((acc, Seq()), d) =>
          (acc :+ Breakfast(d), Seq.empty)

        case ((acc, remainder), d) =>
          d.compareTo(remainder.head.date) match {
            case x if x < 0 => (acc :+ Breakfast(d), remainder)
            case 0 => (acc :+ remainder.head, remainder.tail)
            case _ => throw new IllegalStateException(s"Somehow breakfast ${remainder.head} didn't match a friday!")
          }
      }._1
    }
  }
}



object DAO {

  class DbDAO(val config: DatabaseConfig[JdbcProfile]) extends DAO with LazyLogging  {

    import config.profile.api._
    import config.db

    class BreakfastTable(tag: Tag) extends Table[(Int, Option[String])](tag, Some("PUBLIC"), "BREAKFAST") {

      def dateId = column[Int]("date_id", O.PrimaryKey)
      def assignee = column[Option[String]]("assignee")
      def * = (dateId, assignee)
    }

    val breakfastTable = TableQuery[BreakfastTable]

    def allBreakfastFrom(start: LocalDate): Future[Seq[Breakfast]] = {
      val filter = DAO.toDbId(start)
      val query = breakfastTable.sortBy(_.dateId).filter(_.dateId >= filter).map(r => (r.dateId, r.assignee))
      logger.debug(s"Statements: ${query.result.statements.mkString("\n")}")
      db.run(query.result).map(_.map(Breakfast(_)))
    }

    def addBreakfast(breakfast: Breakfast): Future[Unit] = {
      val insert = breakfastTable.insertOrUpdate(breakfast.toRecord)
      logger.debug(s"Statements: ${insert.statements.mkString("\n")}")
      db.run(insert).map{
        case 1 => ()
        case other => throw new Exception(s"Expected a single changed row following insert, but got $other")
      }
    }

    def latestAssignedBreakfastOnOrAfter(cutOff: LocalDate): Future[Option[Breakfast]] = {
      val select =
        breakfastTable
          .filter(b => b.dateId >= toDbId(cutOff) && b.assignee.nonEmpty)
          .sortBy(_.dateId.desc)
          .take(1)
      db.run(select.result).map(_.headOption.map(Breakfast(_)))
    }
  }

  case class Breakfast(date: LocalDate, assignee: Option[String]) {
    def id = toDbId(date)

    def toRecord = (toDbId(date), assignee)
  }

  object Breakfast {

    def apply(date: String, assignee: String): Breakfast =
      Breakfast(LocalDate.parse(date), Some(assignee))

    def apply(date: String): Breakfast =
      Breakfast(LocalDate.parse(date), None)

    def apply(date: LocalDate): Breakfast =
      Breakfast(date, None)

    def apply(tuple: (Int, Option[String])): Breakfast = Breakfast(fromDbId(tuple._1), tuple._2)
  }

  def toDbId(date: LocalDate): Int =
    date.getYear * 10000 + date.getMonthValue * 100 + date.getDayOfMonth

  def fromDbId(dateId: Int): LocalDate =
    LocalDate.of(dateId / 10000, (dateId % 10000) / 100, dateId % 100)

  def runFlyway(config: Config): Try[Unit] = Try {
    val flywayLocations = config.getStringList("flyway-locations")
    val url = config.getString("db.url")
    val user = config.getString("db.user")
    val password = config.getString("db.password")
    val flyway = new Flyway()
    flyway.setDataSource(url, user, password)
    flyway.setLocations(flywayLocations.asScala.mkString(","))
    flyway.migrate()
  }

  def createWithFlyway(config: Config): Try[DAO] = {
    for {
      dbconf <- Try(config.getConfig("brekkie"))
      _ <- runFlyway(dbconf)
      dao <- Try(new DbDAO(DatabaseConfig.forConfig("", dbconf)))
    } yield dao
  }
}
