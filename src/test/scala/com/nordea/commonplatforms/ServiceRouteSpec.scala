package com.nordea.commonplatforms

import java.time.LocalDate

import akka.http.scaladsl.model.{FormData, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.nordea.commonplatforms.DAO.Breakfast
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

class ServiceRouteSpec extends WordSpec with Matchers with ScalatestRouteTest with ServiceRoute {

  def testDAO(initial: Seq[DAO.Breakfast]) = new DAO {

    var breakfasts: Map[Int, Breakfast] = initial.map(b => b.id -> b).toMap

    def bList = breakfasts.values.toSeq.sortBy(_.id)

    override def allBreakfastFrom(start: LocalDate): Future[Seq[DAO.Breakfast]] =
      Future.successful(bList.filter(_.id >= DAO.toDbId(start)))

    override def addBreakfast(breakfast: DAO.Breakfast): Future[Unit] = {
      breakfasts = breakfasts + (breakfast.id -> breakfast)
      Future.successful()
    }

    override def latestAssignedBreakfastOnOrAfter(cutOff: LocalDate): Future[Option[DAO.Breakfast]] =
      Future.successful(bList.find(b => (b.id >= DAO.toDbId(cutOff)) && b.assignee.nonEmpty))
  }

  "ServiceRoute" should {

    "return static resources" in {
      val site = route(testDAO(Nil))
      Get("/static/test.txt") ~> site ~> check {
        responseAs[String] shouldEqual "ABC"
      }
    }

    "return web page with breakfasts" in {
      val dao = testDAO(Nil)
      dao.addBreakfast(Breakfast("2018-01-12", "John Smith"))
      dao.addBreakfast(Breakfast("2018-01-19"))
      val site = route(dao)
      Get() ~> site ~> check {
        val resp = responseAs[String]
        resp.containsSlice("row-20180112") shouldBe true
        resp.containsSlice("row-20180119") shouldBe true
      }
    }

    "Update with posted form" in {
      val dao = testDAO(Nil)
      val site = route(dao)
      Post("/", FormData(
        "add" -> "Sign Up",
        "_action" -> "add",
        "id" -> "20180119",
        "assignee" -> "Alan Turing"
      )) ~> site ~> check {
        val resp = responseAs[String]
        responseAs[String].containsSlice("Alan Turing") shouldBe true
      }
    }

    "Delete with posted form" in {
      val dao = testDAO(Breakfast("2018-01-19", "Grace Hopper") :: Nil)
      val site = route(dao)
      Get() ~> site ~> check {
        response.status shouldBe StatusCodes.OK
        responseAs[String].containsSlice("Grace Hopper") shouldBe true
      }
      Post("/", FormData(
        "add" -> "Remove",
        "_action" -> "remove",
        "id" -> "20180119"
      )) ~> site ~> check {
        response.status shouldBe StatusCodes.OK
        responseAs[String].containsSlice("Grace Hopper") shouldBe false
      }
    }

    "handle invalid posted data" in {
      val dao = testDAO(Nil)
      val site = route(dao)
      Post("/", FormData(
        "add" -> "Sign Up",
        "_action" -> "add",
        "id" -> "20180119"
      )) ~> site ~> check {
        response.status shouldBe StatusCodes.BadRequest
      }
    }
  }

  override def now: LocalDate = LocalDate.parse("2018-01-12")
}
