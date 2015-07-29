package models.daos

import javax.inject.Inject

import models.Contribution
import models.daos.drivers.Neo4J
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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
    ).map(neo.parseNeoContribution)
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
    ).map(neo.parseNeoContribution)
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
  def save(username: String, repoName: String, contribution: Contribution): Future[Option[Contribution]] = {
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
    ).map(neo.parseNeoContribution)
  }


}
