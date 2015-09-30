package models.daos

import javax.inject.Inject

import models.daos.drivers.Neo4J
import models.{Contribution, Repository}
import play.api.libs.json.{JsUndefined, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContributionDAO @Inject() (neo: Neo4J){

  /**
   * Finds a contribution in the data store.
   *
   * @param username name of the contributing user
   * @param repoName name od the repository he contributed to
   * @return the found Contribution if any.
   */
  def find(username: String, repoName:String): Future[Option[Contribution]] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:CONTRIBUTED_TO]->(r:Repository)
        WHERE u.username={username} AND r.name={repoName}
        RETURN c
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName
      )
    ).map(parseNeoContribution)
  }


  def checkIfUserContributed(username: String, repoName:String): Future[Boolean] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:CONTRIBUTED_TO]->(r:Repository)
        WHERE u.username={username} AND r.name={repoName}
        RETURN c
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName
      )
    ).map(parseNeoContribution).map(_.isDefined)
  }

  /**
   * Finds all contributions of a given user in the data store.
   *
   * @param username name of the contributing user
   * @return the found Contribution if any.
   */
  def findAll(username: String): Future[Seq[(Repository,Contribution)]] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:CONTRIBUTED_TO]->(r:Repository)
        WHERE u.username={username}
        RETURN r, c
      """,
      Json.obj(
        "username" -> username
      )
    ).map(parseNeoContributions)
  }

  /**
   * Adds a contribution for a user to a repository. If the username or the repository doesn't exist. This doesn't do
   * anything and returns None.
   *
   * @param username name of the contributing user
   * @param repoName name of the repository he contributes to.
   * @param contribution contribution to be saved.
   * @return saved contribution
   */
  def add(username: String, repoName: String, contribution: Contribution): Future[Option[Contribution]] = {
    neo.cypher(
      """
        MATCH (u:User),(r:Repository)
        WHERE u.username={username} AND r.name={repoName}
        CREATE (u)-[c:CONTRIBUTED_TO {props}]->(r)
        RETURN c
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName,
        "props" -> Json.toJson(contribution)
      )
    ).map(parseNeoContribution)
  }

  /**
   * Overrides an existing contribution relationship with the one provided. If the username or the repository doesn't exist. This doesn't do
   * anything and returns None.
   *
   * @param username name of the user who made the contribution
   * @param repoName name of the repo he contributed to
   * @param contribution contribution to be saved
   * @return saved contribution
   */
  def update(username: String, repoName: String, contribution: Contribution): Future[Option[Contribution]] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:CONTRIBUTED_TO]->(r:Repository)
        WHERE u.username={username} AND r.name={repoName}
        SET c={props}
        RETURN c
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName,
        "props" -> Json.toJson(contribution)
      )
    ).map(parseNeoContribution)
  }

  /**
   * Parses a neo4j response to get a Contribution out of it.
   *
   * @param response neo4j response
   * @return parsed contribution or None
   */
  def parseNeoContribution(response: WSResponse): Option[Contribution] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case repo => repo.asOpt[Contribution]
    }
  }

  /**
   * Parse multiple contributions and repos
   * @param response response from neo4j
   * @return map with each contribution from repo
   */
  def parseNeoContributions(response: WSResponse): Seq[(Repository,Contribution)] = {
      (Json.parse(response.body) \\ "row").map{contribution => (contribution(0).as[Repository], contribution(1).as[Contribution])}.seq
  }

  /**
   * Parses a string representing the buffer of the current week contribution getting the deleted lines
   *
   * @param currentWeekBuffer String containing the deleted lines this week already counted for needed to be extracted.
   * @return count of the deleted lines already accounted for extracted as an Int
   */
  def parseWeekDeletedLines(currentWeekBuffer: Option[String]): Int = {
    val str = currentWeekBuffer.getOrElse("a0d0")
    str.substring(str.indexOf("d"), str.length).toInt
  }

  /**
   * Parses a string representing the buffer of the current week contribution getting the added lines
   *
   * @param currentWeekBuffer String containing the added lines this week already counted for needed to be extracted.
   * @return count of the added lines already accounted for extracted as an Int
   */
  def parseWeekAddedLines(currentWeekBuffer: Option[String]): Int = {
    val str = currentWeekBuffer.getOrElse("a0d0")
    str.substring(0, str.indexOf("d")).toInt
  }

}
