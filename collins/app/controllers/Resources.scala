package controllers

import play.api._
import play.api.mvc._
import play.core.QueryStringBindable

import models._
import util.SecuritySpec
import views._

object Resources extends SecureWebController {

  implicit val spec = SecuritySpec(isSecure = true, Nil)

  type Help = Help.Value
  object Help extends Enumeration {
    val Ipmi = Value(1)
  }

  def help(htype: Help) = SecureAction { implicit req =>
    Ok(html.resources.help(htype))
  }

  def index = SecureAction { implicit req =>
    Ok(html.resources.index(AssetMeta.getViewable()))
  }

  def find = SecureAction { implicit req =>
    val initialQuery = req.queryString.filter { case(k,vs) => vs.nonEmpty && vs.head.nonEmpty }
    initialQuery.contains("TUMBLR_TAG") match {
      case true =>
        findBySecondaryId(initialQuery("TUMBLR_TAG").head)
      case false =>
        rewriteQuery(initialQuery) match {
          case Nil =>
            Redirect(routes.Resources.index).flashing("message" -> "No query specified")
          case q =>
            findByMeta(q)
        }
    }
  }

  def intake(id: Long, stage: Int = 1, extra: String = "") = SecureAction { implicit req =>
    Asset.findById(id).flatMap { asset =>
      intakeAllowed(asset) match {
        case true => Some(asset)
        case false => None
      }
    } match {
      case None =>
        Redirect(routes.Resources.index).flashing("error" -> "Can not intake host that isn't New")
      case Some(asset) => stage match {
        case 2 =>
          Ok(html.resources.intake2(asset))
        case 3 =>
          Ok(html.resources.intake3(asset, req.queryString("CHASSIS_ID").head))
        case 4 =>
          Ok("Done")
        case n =>
          Ok(html.resources.intake(asset))
      }
    }
  }(SecuritySpec(isSecure = true, Seq("infra")))

  protected def findBySecondaryId[A](id: String)(implicit r: Request[A]) = {
    Asset.findBySecondaryId(id) match {
      case None =>
        Redirect(routes.Resources.index).flashing("message" -> "Could not find asset with specified tumblr tag")
      case Some(asset) =>
        intakeAllowed(asset) match {
          case true =>
            Redirect(routes.Resources.intake(asset.id, 1))
          case false =>
            Ok(html.resources.list(Seq(asset)))
        }
    }
  }

  protected def intakeAllowed[A](asset: Asset)(implicit r: Request[A]) = {
    asset.isNew && hasRole(getUser(r), Seq("infra"))
  }

  private def rewriteQuery(request: Map[String,Seq[String]]): List[(AssetMeta.Enum, String)] = {
    request.map { case(k,vs) =>
      (AssetMeta.Enum.withName(k), vs.head + "%")
    }.toList
  }

  protected def findByMeta[A](query: List[(AssetMeta.Enum, String)])(implicit r: Request[A]) = {
    Asset.findByMeta(query) match {
      case Nil =>
        Redirect(routes.Resources.index).flashing("message" -> "No results found")
      case r =>
        Ok(html.resources.list(r))
    }
  }

  implicit def bindableHelp = new QueryStringBindable[Resources.Help] {
    def bind(key: String, params: Map[String, Seq[String]]) = 
      params.get(key).flatMap(_.headOption).map { i =>
        try {
          Right(Resources.Help(Integer.parseInt(i)))
        } catch {
          case e: Exception => Left("Cannot parse parameter " + key + " as Help")
        }
      }

    def unbind(key: String, value: Resources.Help) = key + "=" + value.id
  }

}
