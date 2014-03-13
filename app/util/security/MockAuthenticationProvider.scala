package util
package security

import models.User

class MockAuthenticationProvider extends AuthenticationProvider {
  override val authType = "default"

  private val users = Map(
    "blake" -> new User("blake", "admin:first", "engineering,infra,ops", "mock"),
    "matt" -> new User("matt", "foobar", "engineering,management", "mock"),
    "test" -> new User("test", "fizz", "", "mock"),
    "joeengineer" -> new User("joeengineer", "flah", "engineering", "mock")
  )

  override def authenticate(username: String, password: String): Option[User] = {
    users.get(username) match {
      case None => None
      case Some(user) => (password == user.password) match {
        case true => Some(user)
        case false => None
      }
    }
  }
}
