package com.nordea.commonplatforms

import java.time.LocalDate

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.{complete, formFields, get, getFromResourceDirectory, pathEndOrSingleSlash, pathPrefix, post}
import akka.http.scaladsl.server.Route
import com.nordea.commonplatforms.DAO.{Breakfast, fromDbId}

import scala.concurrent.ExecutionContext

trait ServiceRoute {


  def now: LocalDate

  def render(dao: DAO)(implicit ex: ExecutionContext) = {
    val response = dao.fill(now, 3, 10)
      .map(views.html.Application.main(_))
      .map(content => HttpEntity(ContentTypes.`text/html(UTF-8)`, content.toString()))
    complete(response)
  }

  def route(dao: DAO)(implicit ex: ExecutionContext): Route = {
    pathEndOrSingleSlash {
      get {
        render(dao)
      } ~ post {
        formFields('_action, 'id.as[Int], 'assignee.?) { (action, id, assignee) =>
          val valid = (action, assignee) match {
            case ("remove", None) => true
            case ("add", Some(name)) if name.length > 0 => true
            case _ => false
          }
          if(!valid)
            throw new IllegalRequestException(
              ErrorInfo("oops!", s"($action, $assignee) is not a valid combination"),
              StatusCodes.BadRequest
            )
          val b = Breakfast(fromDbId(id), assignee)
          dao.addBreakfast(b)
          render(dao)
        }
      }
    } ~ pathPrefix("static") {
      getFromResourceDirectory("static")
    }
  }
}
