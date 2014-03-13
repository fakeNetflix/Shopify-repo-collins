package util
package security

import models.User
import java.net.URLEncoder
import io.Source

class GoogleAuthenticationProvider extends AuthenticationProvider {
  override val authType = "google"

  override def authenticate(email: String, password: String): Option[User] = {
    AuthenticationProvider.getUser(email, "google") match {
      case None => None
      case Some(u) => (AuthenticationProvider.hashPassword(password, u.salt) == u.password) match {
        case true => Some(u)
        case false => None
      }
    }
  }
}

object GoogleAuthenticationProvider {
  def getRedirectURL(returnRoute: String, location: Option[Seq[String]]) = {
    val realm = returnRoute.substring(0, returnRoute.indexOf('/', 8) + 1)
    val return_to = returnRoute + (location match {
      case Some(loc) => "?location=" + URLEncoder.encode(loc(0), "UTF-8")
      case None => ""
    })
    "https://www.google.com/accounts/o8/ud?openid.ns=http://specs.openid.net/auth/2.0" +
      "&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select" +
      "&openid.identity=http://specs.openid.net/auth/2.0/identifier_select" +
      "&openid.return_to=" + URLEncoder.encode(return_to, "UTF-8") +
      "&openid.realm=" + URLEncoder.encode(realm, "UTF-8") +
      "&openid.assoc_handle=ABSmpf6DNMw" +
      "&openid.mode=checkid_setup" +
      "&openid.ns.ax=http://openid.net/srv/ax/1.0" +
      "&openid.ax.mode=fetch_request" +
      "&openid.ax.type.email=http://axschema.org/contact/email" +
      "&openid.ax.required=email"
  }

  def authenticate(email: String): Option[User] = {
    println("Email logged in: " + email)
    AuthenticationProvider.getUser(email, "google") match {
      case None => None
      case Some(user) =>
        println("User is whitelisted: " + email)
        Some(user)
    }
  }

  def toJavaMap(src: Map[String, Seq[String]]): java.util.Map[String, Object] = {
    val result = new java.util.HashMap[String, Object]()
    for (a <- src.keys) {
      val extractedLocalValue = src(a).toArray
      val value = if (extractedLocalValue.length > 1) {
        extractedLocalValue
      } else {
        extractedLocalValue(0)
      }
      result.put(a, value)
      // Logger.info(a + "=" + value)
    }
    result;
  }
}
