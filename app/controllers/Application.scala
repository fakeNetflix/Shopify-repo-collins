package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.openid._
import play.api.libs.concurrent._
import org.openid4java.consumer.ConsumerManager
import org.openid4java.message.ax.FetchRequest
import org.openid4java.message.ParameterList
import org.openid4java.consumer.VerificationResult
import org.openid4java.message.AuthSuccess
import org.openid4java.message.ax.AxMessage
import org.openid4java.message.ax.FetchResponse
import org.openid4java.OpenIDException
import org.openid4java.discovery.DiscoveryInformation
import org.openid4java.discovery.Identifier
import collection.JavaConversions._

import models._
import util.security.SecuritySpec
import views._
import util.security._
import models.User
import util.security.MixedAuthenticationProvider.isTypeEnabled

object Application extends SecureWebController {
  val manager = new ConsumerManager();
  val discovered = manager.associate(manager.discover("https://www.google.com/accounts/o8/id"))

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
        Ok(html.login(loginForm, isTypeEnabled("google")))
      case Some(location) =>
        Ok(html.login(loginForm.fill(("","",Some(location.head))).copy(errors = Nil), isTypeEnabled("google")))
    }
  }

  def authenticate = Action { implicit req =>
    setUser(None)
    if (req.body.asFormUrlEncoded.map(v => v.get("google")).getOrElse(None).isEmpty) {
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          val tmp: Map[String,String] = formWithErrors.data - "password"
          BadRequest(html.login(formWithErrors.copy(data = tmp), isTypeEnabled("google")))
        },
        user => {
          val u = User.authenticate(user._1, user._2)
          val session: Map[String,String] = u match {
            case None => Map()
            case Some(usr) => Map("uid" -> usr.id.toString)
          }
          user._3 match {
            case Some(location) =>
              SessionStore.withSession(Redirect(location), session)
            case None =>
              SessionStore.withSession(Redirect(app.routes.Resources.index), session)
          }
        }
      )
    } else if (isTypeEnabled("google")) {
      val authReq = manager.authenticate(discovered, routes.Application.openid.absoluteURL(false))
      val fetch = FetchRequest.createFetchRequest()
      fetch.addAttribute("email", "http://axschema.org/contact/email", true)
      authReq.addExtension(fetch)

      Redirect(authReq.getDestinationUrl(true)).withSession("location" -> req.body.asFormUrlEncoded.map(v => v.get("location").map(l => l(0)).getOrElse("")).getOrElse("").asInstanceOf[String])
    } else {
      Redirect(routes.Application.login).flashing(
        "security" -> "OpenId login is not enabled"
      )
    }
  }

  def openid = Action { implicit req =>
    if (isTypeEnabled("google")) {
      try {
        setUser(None)
        val response = new ParameterList(GoogleAuthenticationProvider.toJavaMap(req.queryString))
        val verification: VerificationResult = manager.verify("http://" + req.host + req.uri, response, discovered)

        val verified: Identifier = verification.getVerifiedId()
        if (verified != null) {
          val authSuccess: AuthSuccess = verification.getAuthResponse().asInstanceOf[AuthSuccess]
          val fetchResp: FetchResponse = authSuccess.getExtension(AxMessage.OPENID_NS_AX).asInstanceOf[FetchResponse]

          val u = GoogleAuthenticationProvider.authenticate(fetchResp.getAttributeValue("email"))
          u match {
            case None => Redirect(routes.Application.login).flashing(
                "security" -> "You do not have permission to use collins."
              )
            case Some(usr) =>
              req.session.get("location") match {
                case None | Some("") =>
                    SessionStore.withSession(Redirect(app.routes.Resources.index), Map("uid" -> usr.id.toString))
                case Some(location) =>
                    SessionStore.withSession(Redirect(location), Map("uid" -> usr.id.toString))
              }
          }
        } else {
          Redirect(routes.Application.login).flashing(
            "security" -> "OpenId response is invalid."
          )
        }
      } catch {
        case e: OpenIDException =>
        e.printStackTrace()
        Redirect(routes.Application.login).flashing(
          "security" -> "Login could not be verified, please try again."
        )
      }
    } else {
      Redirect(routes.Application.login).flashing(
        "security" -> "OpenId login is not enabled"
      )
    }
  }

  def logout = SecureAction { implicit req =>
    setUser(None)
    SessionStore.revokeSession(req)
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You have been logged out"
    )
  }(SecuritySpec(true))
}
