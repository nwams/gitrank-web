package models.daos

import javax.inject.Inject

import models.daos.drivers.{Neo4j, NeoParsers}
import models.{Feedback, Score}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ScoreDAO @Inject()(neo: Neo4j,
                        parser: NeoParsers) {

  /**
   * Saves a score into the data store.
   *
   * @param username username of the user who score the repo
   * @param repoName repository which was scored
   * @param score score given by the user to the repository
   * @return saved score.
   */
  def save(username: String, repoName: String, score: Score): Future[Option[Score]] = {
    neo.cypher(
      """
        MATCH (u:User),(r:Repository)
        WHERE u.username={username} AND r.name={repoName}
        CREATE UNIQUE (u)-[c:SCORED {props}]->(r)
        RETURN c
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName,
        "props" -> Json.toJson(score)
      )
    ).map(parser.parseNeoScore)
  }


  /**
   * Find a score from a user to a repo
   *
   * @param username username of the user who score the repo
   * @param repoName repository which was scored
   * @return saved score.
   */
  def find(username: String, repoName: String): Future[Option[Score]] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:SCORED]->(r:Repository)
        WHERE u.username={username} AND r.name={repoName}
        RETURN c
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName
      )
    ).map(parser.parseNeoScore)
  }


  /**
   * Delete a score from a user to a repo
   *
   * @param username username of the user who score the repo
   * @param repoName repository which was scored
   * @return
   */
  def delete(username: String, repoName: String): Future[WSResponse] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:SCORED]->(r:Repository)
        WHERE u.username={username} AND r.name={repoName}
        DELETE c
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName
      )
    )
  }

  /**
   * Update a score from a user to a repo
   *
   * @param username username of the user who score the repo
   * @param repoName repository which was scored
   * @return
   */
  def update(username: String, repoName: String,score: Score): Future[Option[Score]] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:SCORED]->(r:Repository)
        WHERE u.username={username} AND r.name={repoName}
        SET c={props}
        RETURN c
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName,
        "props" -> Json.toJson(score)
      )
    ).map(parser.parseNeoScore)
  }

  /**
   * Get all the scoring made for a repository corresponding to the page and items per page specified arguments.
   *
   * @param repoName name of the repository to get the scores from ("owner/repo")
   * @param page page number to get from the database. Default value to 1
   * @param itemsPerPage number of items to display in a database page
   * @return Seq of Feedback.
   */
  def findRepositoryFeedback(repoName: String, page: Int = 1, itemsPerPage: Int = 10): Future[Seq[Feedback]] = {

    if (page < 1){
      throw new Exception("Page must be a positive non null integer")
    }

    neo.cypher(
      """
        MATCH (u:User)-[c:SCORED]->(r:Repository)
        WHERE r.name={repoName}
        RETURN c, u ORDER BY c.timestamp DESC SKIP {feedbackSkip} LIMIT {pageSize}
      """, Json.obj(
        "repoName" -> repoName,
        "feedbackSkip" -> (page - 1) * itemsPerPage,
        "pageSize" -> itemsPerPage
      )
    ).map(parser.parseNeoFeedbackList)
  }

  /**
   * Get all the scoring made for a repository corresponding to the page and items per page specified arguments.
   *
   * @param repoName name of the repository to get the scores from ("owner/repo")
   * @param page page number to get from the database. Default value to 1
   * @param itemsPerPage number of items to display in a database page
   * @return Seq of Scores.
   */
  def findRepositoryScores(repoName: String, page: Int=1, itemsPerPage: Int=10): Future[Seq[Score]] = {
    if (page < 1){
      throw new Exception("Page must be a positive non null integer")
    }

    neo.cypher(
      """
        MATCH (u:User)-[c:SCORED]->(r:Repository)
        WHERE r.name={repoName}
        RETURN c ORDER BY c.timestamp DESC SKIP {scoreSkip} LIMIT {pageSize}
      """, Json.obj(
        "repoName" -> repoName,
        "scoreSkip" -> (page - 1) * itemsPerPage,
        "pageSize" -> itemsPerPage
      )
    ).map(parser.parseNeoScoreList)
  }

  /**
   * Gets the number of feedback there is for a given repository
   *
   * @param repoName name of the repository to get the feedback count from
   * @return
   */
  def countRepositoryFeedback(repoName: String): Future[Int] = {
    neo.cypher(
      """
      MATCH (u:User)-[c:SCORED]->(r:Repository)
      WHERE r.name={repoName}
      RETURN count(c) AS feedbackCount
      """,
      Json.obj("repoName" -> repoName)
    ).map(response => (((response.json \ "results")(0) \ "data")(0) \ "row")(0).as[Int])
  }
}
