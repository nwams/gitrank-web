package models.daos

import java.util.UUID

import org.anormcypher._
import com.mohiva.play.silhouette.api.LoginInfo
import models.User

import scala.concurrent.Future

// TODO Implement here connection with the neo4j Datastore
/**
 * Give access to the user object.
 */
class UserDAOImpl extends UserDAO {

  implicit val connection2 = Neo4jREST("localhost", 7474, "/db/data/", "username", "password")

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = Future.successful()

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID) = Future.successful()

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {
    Future.successful(user)
  }
}