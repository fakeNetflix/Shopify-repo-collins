package util.security

import models.User


/**
 * Exception encountered during authentication phase
 */
class AuthenticationException(msg: String) extends Exception(msg)

/**
 * Provides authentication by a variety of methods
 */
class MixedAuthenticationProvider(types: String) extends AuthenticationProvider {

  /**
   * @return The authorization type provided by this class
   */
  def authType: String = types

  /**
   * Ordered list of permitted authentication types
   */
  private val configuredTypes = types.split(",").toSeq.map(_.trim)

  /**
   * Attempt to authenticate user by each method specified in :types
   * @return Authenticated user or none
   */
  def authenticate(username: String, password: String): Option[User] = {
    logger.debug("Beginning to try authentication types")

    // Iterate over the types lazily, such that if one method passes, iteration stops
    configuredTypes.view.flatMap({
      case "default" => {
        logger.trace("mock authentication type")
        val defaultProvider = AuthenticationProvider.Default
        val user = defaultProvider.authenticate(username, password)
        logger.debug("Tried mock authentication for %s, got back %s".format(username, user))
        user
      }
      case "basic" => {
        logger.trace("basic authentication type")
        val basicProvider = new BasicAuthenticationProvider()
        val user = basicProvider.authenticate(username, password)
        logger.debug("Tried basic authentication for %s, got back %s".format(username, user))
        user
      }
      case "ldap" => {
        val ldapProvider = new LdapAuthenticationProvider()
        val user = ldapProvider.authenticate(username, password)
        logger.debug("Tried ldap authentication for %s, got back %s".format(username, user))
        user
      }
      case "google" => {
        val googleProvider = new GoogleAuthenticationProvider()
        val user = googleProvider.authenticate(username, password)
        logger.debug("Tried google authentication for %s, got back %s".format(username, user))
        user
      }
      case t => {
        throw new AuthenticationException("Invalid authentication type provided: " + t)
      }
    }).headOption
  }
}

object MixedAuthenticationProvider {
  def isTypeEnabled(name: String): Boolean = {
    val auths = AuthenticationProviderConfig.authType.split(",").toSeq.map(_.trim)
    auths.contains(name)
  }
}
