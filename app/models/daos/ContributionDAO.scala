package models.daos

import javax.inject.Inject

import models.daos.drivers.{Neo4j, NeoParsers}
import models.{Contribution, Repository}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContributionDAO @Inject() (neo: Neo4j,
                                parser: NeoParsers){

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
    ).map(parser.parseNeoContribution)
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
    ).map(parser.parseNeoContribution).map(_.isDefined)
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
    ).map(parser.parseNeoContributions)
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
    ).map(parser.parseNeoContribution)
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
    ).map(parser.parseNeoContribution)
  }
}
