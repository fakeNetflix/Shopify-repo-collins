package models

import util.security.AuthenticationProvider
import org.specs2.mutable._
import java.io.File

object UserSpec extends Specification {

  "The User Model" should {
    "handle authentication" in {
      "with default authentication" in {
        val provider = AuthenticationProvider.Default
        User.authenticate("blake", "admin:first", Some(provider)) must beSome[User]
        User.authenticate("no", "suchuser", Some(provider)) must beNone
      }
    }
    "serialize/deserialize" in {
      val u = new User("blake", "*", "engineering", "mock")
      val uMap = u.toMap()
      val newU = User.fromMap(uMap).get
      newU.username mustEqual u.username
      newU.id mustEqual u.id
      newU.roles mustEqual u.roles
    }
  }

}
