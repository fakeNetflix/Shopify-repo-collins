package util
package security

import models.{User, UserImpl}
import java.net.URLEncoder
import io.Source

class OpenIdAuthenticationProvider extends AuthenticationProvider {
  override val authType = "openid"

  override def authenticate(email: String, blank: String): Option[User] = {
    println("Email logged in: " + email)
    if (email.endsWith("@" + OpenIdAuthenticationProvider.config.whitelistDomain)) {
      println("Domain is whitelisted")
      val username = email.substring(0, email.lastIndexOf('@'))
      if (OpenIdAuthenticationProvider.whitelist.contains(username)) {
        println("User is on whitelist: " + username)
        Some(UserImpl(username, "*", Set("engineering","Infra","ops"), 2000 + math.abs(email.hashCode() % 1000000), true))
      } else {
        None
      }
    } else {
      None
    }
  }
}

object OpenIdAuthenticationProvider {
  val config = OpenIdAuthenticationProviderConfig

  lazy val whitelist = Source.fromFile(config.whitelistFile).getLines.toList

  def getRedirectURL(returnRoute: String, location: Option[Seq[String]]) = {
    val ch = config.baseUrl.contains('?') match {
      case true => '&'
      case false => '?'
    }
    val realm = returnRoute.substring(0, returnRoute.indexOf('/', 8) + 1)
    val return_to = returnRoute + (location match {
      case Some(loc) => "?location=" + URLEncoder.encode(loc(0), "UTF-8")
      case None => ""
    })
    config.baseUrl + ch + "openid.ns=http://specs.openid.net/auth/2.0" +
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
}
