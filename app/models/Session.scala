package models

import play.api._
import play.api.mvc._
import controllers.Permissions
import util.Stats
import util.security.{AuthenticationAccessor, AuthenticationProvider, AuthenticationProviderConfig}
import java.security.SecureRandom
import java.math.BigInteger
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column
import play.api.libs.json.Json
import play.api.libs.json.JsValue

class DbSession(val token: String, val expiry: Long, val data: String) extends KeyedEntity[String] {
  @Column("token")
  def id = token
}

class Session(
    var data: Map[String,String],
    var expiry: Long = 0,
    val token: String = new BigInteger(130, Session.random).toString(32)
  ) {
  def revoke() {
    expiry = 0
    SessionStore.revokeSession(token)
  }

  def refresh() {
    expiry = System.currentTimeMillis + AuthenticationProviderConfig.sessionTimeout
  }

  def isValid(): Boolean = {
    System.currentTimeMillis < expiry
  }
}

object Session {
  private val random = new SecureRandom()
}

object SessionStore extends Schema {
  private val sessions = table[DbSession]("sessions")

  def withSession(req: PlainResult, data: Map[String,String]): PlainResult = {
    val session = new Session(data)
    session.refresh()
    val value: JsValue = Json.toJson(data)
    transaction { sessions.insertOrUpdate(new DbSession(session.token, session.expiry, Json.stringify(value))) }
    req.withSession(("token" -> session.token))
  }

  def revokeSession(token: String) {
    transaction { sessions.delete(token) }
  }

  def revokeSession(req: RequestHeader) {
    val token = req.session.get("token")
    if (!token.isEmpty) revokeSession(token.get)
  }

  def getSession(token: String): Option[Session] = {
    val session = transaction { sessions.lookup(token) }
    return session match {
      case None => None
      case Some(s) =>
        val json:JsValue = Json.parse(s.data)
        val data:Map[String,String] = Json.fromJson[Map[String, String]](json)
        Some(new Session(data, s.expiry, s.token))
    }
  }

  def getSession(req: RequestHeader): Map[String,String] = {
    val token = req.session.get("token")
    token match {
      case None => Map()
      case Some(tok) => getSession(tok) match {
        case None => Map()
        case Some(session) => session.isValid match {
          case false => {
            session.revoke()
            Map()
          }
          case true => session.data
        }
      }
    }
  }

  def refreshSession(token: String) {
    val session = getSession(token)
    if (!session.isEmpty) session.get.refresh()
  }

  def refreshSession(req: RequestHeader) {
    val token = req.session.get("token")
    if (!token.isEmpty) refreshSession(token.get)
  }
}
