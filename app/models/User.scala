package models

import play.api._
import controllers.Permissions
import java.security.SecureRandom
import java.math.BigInteger
import org.squeryl.Schema
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column
import org.squeryl.PrimitiveTypeMode._
import util.Stats
import util.security.{AuthenticationAccessor, AuthenticationProvider}
import java.lang.NumberFormatException

case class UserException(message: String) extends Exception(message)

class User(
    val username: String,
    val password: String,
    val roles: String,
    @Column("type")
    val auth_type: String,
    val salt: String = new BigInteger(130, User.random).toString(32),
    val id: Long = 0
  ) extends KeyedEntity[Long] {
  def authenticate(username: String, password: String) = User.authenticate(username, password)
  def rolesSet = roles.split(",").toSet
  def hasRole(name: String): Boolean = rolesSet.contains(name)
  def getRole[T](name: String): Option[String] = hasRole(name) match {
    case true => Some(name)
    case false => None
  }
  def isEmpty(): Boolean = username.isEmpty && password.isEmpty && roles.isEmpty
  def canSeePasswords(): Boolean = Permissions.please(this, Permissions.Feature.CanSeePasswords)
  def isAdmin(): Boolean = hasRole("infra")
}

object User extends Schema {
  private val random = new SecureRandom()

  val users = table[User]("users")
  on(users)(u => declare(
    u.id is(autoIncremented,primaryKey),
    u.username is(unique),
    u.auth_type is(indexed)
  ))

  def authenticate(username: String, password: String, provider: Option[AuthenticationProvider] = None) = {
    val p = provider match {
      case None => getProviderFromFramework()
      case Some(p) => p
    }
    p.tryAuthCache(username, password) match {
      case None =>
        Stats.count("Authentication","Failure")
        None
      case Some(user) =>
        Stats.count("Authentication", "Success")
        Some(user)
    }
  }

  private def getProviderFromFramework(): AuthenticationProvider = {
    Play.maybeApplication.map { app =>
      app.global match {
        case a: AuthenticationAccessor => a.getAuthentication()
        case _ => throw UserException("No authentication handler available")
      }
    }.getOrElse(throw UserException("Not in application"))
  }

  def fromSession(map: Map[String,String]): Option[User] = {
    val uid = try {
      map.getOrElse("uid", "-1").toLong
    } catch {
      case e: NumberFormatException => -1
    }

    transaction { users.lookup(uid) }
  }
}
