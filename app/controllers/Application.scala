package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.openid._
import play.api.libs.concurrent._

import models._
import util.security.SecuritySpec
import views._
import util.security._
import models.{User, UserImpl}
import util.security.MixedAuthenticationProvider.isTypeEnabled

object Application extends SecureWebController {

  val loginForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText(3),
      "location" -> optional(text)
      ) verifying ("Invalid username or password", result => result match {
        case(username,password,location) =>
          User.authenticate(username, password).isDefined
      })
  )

  def login = Action { implicit req =>
    setUser(None)
    req.queryString.get("location") match {
      case None =>
        Ok(html.login(loginForm, isTypeEnabled("openid")))
      case Some(location) =>
        Ok(html.login(loginForm.fill(("","",Some(location.head))).copy(errors = Nil), isTypeEnabled("openid")))
    }
  }

  def authenticate = Action { implicit req =>
    setUser(None)
    if (req.body.asFormUrlEncoded.map(v => v.get("openid")).getOrElse(None).isEmpty) {
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          val tmp: Map[String,String] = formWithErrors.data - "password"
          BadRequest(html.login(formWithErrors.copy(data = tmp), isTypeEnabled("openid")))
        },
        user => {
          val u = User.toMap(User.authenticate(user._1, user._2))
          user._3 match {
            case Some(location) =>
              Redirect(location).withSession(u.toSeq:_*)
            case None =>
              Redirect(app.routes.Resources.index).withSession(u.toSeq:_*)
          }
        }
      )
    } else if (isTypeEnabled("openid")) {
      Redirect(OpenIdAuthenticationProvider.getRedirectURL(routes.Application.openid.absoluteURL(), req.body.asFormUrlEncoded.map(v => v.get("location")).getOrElse(None)))
    } else {
      Redirect(routes.Application.login).flashing(
        "security" -> "OpenId login is not enabled"
      )
    }
  }

  def openid = Action { implicit req =>
    if (isTypeEnabled("openid")) {
      setUser(None)
      AsyncResult(
        OpenID.verifiedId.extend( _.value match {
          case Redeemed(info) =>
            val u = User.toMap(User.authenticate(info.attributes("email"), ""))
            if (u.isEmpty) {
              Redirect(routes.Application.login).flashing(
                "security" -> "You do not have permission to use collins."
              )
            } else {
              req.queryString.get("location") match {
                case Some(location) =>
                  Redirect(location(0)).withSession(u.toSeq:_*)
                case None =>
                  Redirect(app.routes.Resources.index).withSession(u.toSeq:_*)
              }
            }
          case Thrown(t) => {
            t.printStackTrace()
            Redirect(routes.Application.login).flashing(
              "security" -> "Login could not be verified, please try again."
            )
          }
        })
      )
    } else {
      Redirect(routes.Application.login).flashing(
        "security" -> "OpenId login is not enabled"
      )
    }
  }

  def logout = SecureAction { implicit req =>
    setUser(None)
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You have been logged out"
    )
  }(SecuritySpec(true))
}
