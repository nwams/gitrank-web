package models.daos

import javax.inject.Inject

import models.Quickstart
import models.daos.drivers.Neo4j
import play.api.libs.json.{JsArray, JsUndefined, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QuickstartDAO @Inject()(neo: Neo4j) {

  /**
   * Saves a quickstart guide into the data store.
   *
   * @param username username of the user who did the quickstart for the repo
   * @param repoName repository which the guide was created
   * @param quickstart guide given by the user to the repository
   * @return saved quickstart guide.
   */
  def save(username: String, repoName: String, quickstart: Quickstart): Future[Option[Quickstart]] = {
    neo.cypher(
      """
        MATCH (u:User),(r:Repository)
        WHERE u.username={username} AND r.name={repoName}
        CREATE UNIQUE (u)-[c:QUICKSTARTED {props}]->(r)
        RETURN {id: id(c), properties: c}
      """,
      Json.obj(
        "username" -> username,
        "repoName" -> repoName,
        "props" -> Json.toJson(quickstart)
      )
    ).map(parseNeoQuickstart)
  }

  /**
   * Get all the guides made for a repository corresponding to the page and items per page specified arguments.
   *
   * @param repoName name of the repository to get the guides from ("owner/repo")
   * @param page page number to get from the database. Default value to 1
   * @param itemsPerPage number of items to display in a database page
   * @return Seq of Quickstarts.
   */
  def findRepositoryGuides(repoName: String, page: Int = 1, itemsPerPage: Int = 10): Future[Seq[Quickstart]] = {
    if (page < 1) {
      throw new Exception("Page must be a positive non null integer")
    }

    neo.cypher(
      """
        MATCH (u:User)-[c:QUICKSTARTED]->(r:Repository)
        WHERE r.name={repoName}
        RETURN {id: id(c), properties: c} ORDER BY c.upvote DESC SKIP {scoreSkip} LIMIT {pageSize}
      """, Json.obj(
        "repoName" -> repoName,
        "scoreSkip" -> (page - 1) * itemsPerPage,
        "pageSize" -> itemsPerPage
      )
    ).map(parseNeoQuickstartList)
  }

  /**
   * Update a quickstart guide
   *
   * @param id id of the guide
   * @param repoName repository name
   * @param quickstart data to be updated
   * @return quickstart guide updated
   */
  def update(id: Int, repoName: String, quickstart: Quickstart): Future[Option[Quickstart]] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:QUICKSTARTED]->(r:Repository)
        WHERE id(c)={id} AND r.name={repoName}
        SET c={props}
        RETURN {id: id(c), properties: c}
      """,
      Json.obj(
        "id" -> id,
        "repoName" -> repoName,
        "props" -> Json.toJson(quickstart)
      )
    ).map(parseNeoQuickstart)
  }

  /**
   * Find a single quickstart guide
   *
   * @param repoName name of the repo
   * @param id id of the guide
   * @return guide itself
   */
  def findRepositoryGuide(repoName: String, id: Int): Future[Option[Quickstart]] = {

    neo.cypher(
      """
        MATCH (u:User)-[c:QUICKSTARTED]->(r:Repository)
        WHERE r.name={repoName}  AND id(c) = {id}
        RETURN {id:id(c), properties: c} ORDER BY c.timestamp DESC
      """, Json.obj(
        "repoName" -> repoName,
        "id" -> id
      )
    ).map(parseNeoQuickstart)
  }

  /**
   * Parses a neo Quickstart into a model
   *
   * @param response response from neo
   * @return Quickstart object
   */
  def parseNeoQuickstart(response: WSResponse): Option[Quickstart] = {
    (((response.json \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case score => Some((score \ "properties").as[Quickstart].copy(id=(score \ "id").asOpt[Int]))
    }
  }

  /**
   * Should parse a result list of quickstarts and get it back
   *
   * @param response response from neo
   * @return Seq of quickstarters
   */
  def parseNeoQuickstartList(response: WSResponse): Seq[Quickstart] =
    ((response.json \ "results")(0) \ "data").as[JsArray].value.map(jsValue =>
      ((jsValue \ "row")(0) \ "properties").as[Quickstart].copy(id=((jsValue \ "row")(0) \ "id").asOpt[Int]))
}
