package models.daos

import javax.inject.Inject

import models.{Feedback, Score}
import models.daos.drivers.Neo4J
import play.api.libs.json.{JsArray, JsUndefined, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class ScoreDAO @Inject() (neo: Neo4J){

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
        CREATE (u)-[c:SCORED {props}]->(r)
        RETURN c
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName,
        "props" -> Json.toJson(score)
      )
    ).map(parseNeoScore)
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
    ).map(parseNeoScore)
  }

  /**
   * Find all scores for a given repo
   *
   * @param repoName repository which was scored
   * @return saved scores.
   */
  def find( repoName: String): Future[Seq[Feedback]] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:SCORED]->(r:Repository)
        WHERE  r.name={repoName}
        RETURN c
      """,
      Json.obj(
        "repoName" -> repoName
      )
    ).map(parseNeoFeedbackList)
  }

  /**
   * Get all the scoring made for a repository corresponding to the page and items per page specified arguments.
   *
   * @param repoName name of the repository to get the scores from ("owner/repo")
   * @param page page number to get from the database. Default value to 1
   * @param itemsPerPage number of items to display in a database page
   * @return Seq of Feedback.
   */
  def findRepositoryFeedback(repoName: String, page: Int=1, itemsPerPage: Int=10): Future[Seq[Feedback]] = {

    if (page == 0){
      throw new Exception("Page 0 does not exist !")
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
    ).map(parseNeoFeedbackList)
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

  /**
   * Should parse a result list of scores and get it back
   *
   * @param response response from neo
   * @return Seq of Scores
   */
  def parseNeoFeedbackList(response: WSResponse): Seq[Feedback] =
    ((response.json \ "results")(0) \ "data").as[JsArray].value.map(jsValue =>
      Feedback(neo.parseSingleUser((jsValue \ "row")(1)).get, (jsValue \ "row")(0).as[Score]))

  /**
   * Parses a neo Score into a model
   *
   * @param response response from neo
   * @return
   */
  def parseNeoScore(response: WSResponse): Option[Score] = {
    (((response.json \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case score => score.asOpt[Score]
    }
  }
}
