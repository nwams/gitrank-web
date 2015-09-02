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
   * get all the scoring made for a repository.
   *
   * @param repoName name of the repository to get the scores from ("owner/repo")
   * @return Seq of Scores.
   */
  def findRepositoryFeedback(repoName: String): Future[Seq[Feedback]] ={
    neo.cypher(
      """
        MATCH (u:User)-[c:SCORED]->(r:Repository)
        WHERE r.name={repoName}
        RETURN c, u
      """,
      Json.obj("repoName" -> repoName)
    ).map(parseNeoFeedbackList)
  }

  /**
   * Should parse a result list of scores and get it back
   *
   * @param response response from neo
   * @return Seq of Scores
   */
  def parseNeoFeedbackList(response: WSResponse): Seq[Feedback] =
    ((Json.parse(response.body) \ "results")(0) \ "data").as[JsArray].value.map(jsValue =>
      Feedback(neo.parseSingleUser((jsValue \ "row")(0)).get, (jsValue \ "row")(0).as[Score]))

  /**
   * Parses a neo Score into a model
   *
   * @param response response from neo
   * @return
   */
  def parseNeoScore(response: WSResponse): Option[Score] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case score => score.asOpt[Score]
    }
  }

}
