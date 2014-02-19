package models

import play.api._
import play.api.mvc._
import controllers.Permissions
import util.Stats
import util.security.{AuthenticationAccessor, AuthenticationProvider}
import java.security.SecureRandom
import java.math.BigInteger

class Session(_data: Map[String,String]) {
  var data = _data
  var expiry: Long = 0
  val token = new BigInteger(130, Session.random).toString(32)

  def revoke() {
    expiry = 0
    SessionStore.revokeSession(token)
  }

  def refresh(minutes: Long) {
    expiry = System.currentTimeMillis + (60000 * minutes)
  }

  def isValid(): Boolean = {
    System.currentTimeMillis < expiry
  }
}

object Session {
  private val random = new SecureRandom()
}

object SessionStore {
  private val SESSION_TIMEOUT = 30 // In minutes
  private val store: collection.mutable.Map[String,Session] = collection.mutable.Map()

  def withSession(req: PlainResult, data: Map[String,String]): PlainResult = {
    val session = new Session(data)
    session.refresh(SESSION_TIMEOUT)
    store.put(session.token, session)
    req.withSession(("token" -> session.token))
  }

  def revokeSession(token: String) {
    store.remove(token)
  }

  def revokeSession(req: RequestHeader) {
    val token = req.session.get("token")
    if (!token.isEmpty) revokeSession(token.get)
  }

  def getSession(req: RequestHeader): Map[String,String] = {
    val token = req.session.get("token")
    token match {
      case None => Map()
      case Some(tok) => store.get(tok) match {
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
    val session = store.get(token)
    if (!session.isEmpty) session.get.refresh(SESSION_TIMEOUT)
  }

  def refreshSession(req: RequestHeader) {
    val token = req.session.get("token")
    if (!token.isEmpty) refreshSession(token.get)
  }

}
