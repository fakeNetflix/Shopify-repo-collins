package util
package security

import collins.cache.ConfigCache
import models.User

import io.Source

class BasicAuthenticationProvider() extends AuthenticationProvider {
  override val authType = "basic"

  override def authenticate(username: String, password: String): Option[User] = {
    AuthenticationProvider.getUser(username, "basic") match {
      case None => None
      case Some(u) => (AuthenticationProvider.hashPassword(password, u.salt) == u.password) match {
        case true => Some(u)
        case false => None
      }
    }
  }
}

