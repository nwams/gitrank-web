package models.daos

import javax.inject.Inject

import models.{Quickstart, Score}
import models.daos.drivers.Neo4J
import play.api.libs.json.{JsArray, JsUndefined, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class QuickstartDAO @Inject()(neo: Neo4J) {

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
        RETURN c
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
        RETURN c ORDER BY c.timestamp DESC SKIP {scoreSkip} LIMIT {pageSize}
      """, Json.obj(
        "repoName" -> repoName,
        "scoreSkip" -> (page - 1) * itemsPerPage,
        "pageSize" -> itemsPerPage
      )
    ).map(parseNeoQuickstartList)
  }


  /**
   * Update a quickstart guide
   * @param title title of the guide
   * @param repoName repository name
   * @param quickstart data to be updated
   * @return quickstart guide updated
   */
  def update(title: String, repoName: String, quickstart: Quickstart): Future[Option[Quickstart]] = {
    neo.cypher(
      """
        MATCH (u:User)-[c:QUICKSTARTED]->(r:Repository)
        WHERE c.title={title} AND r.name={repoName}
        SET c={props}
        RETURN c
      """,
      Json.obj(
        "title" -> title,
        "repoName" -> repoName,
        "props" -> Json.toJson(quickstart)
      )
    ).map(parseNeoQuickstart)
  }

  /**
   * Find a single quickstart guide
   * @param repoName name of the repo
   * @param title title of the guide
   * @return guide itself
   */
  def findRepositoryGuide(repoName: String, title: String): Future[Option[Quickstart]] = {

    neo.cypher(
      """
        MATCH (u:User)-[c:QUICKSTARTED]->(r:Repository)
        WHERE r.name={repoName}  AND c.title ={title}
        RETURN c ORDER BY c.timestamp DESC
      """, Json.obj(
        "repoName" -> repoName,
        "title" -> title
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
      case score => score.asOpt[Quickstart]
    }
  }

  /**
   * Should parse a result list of quickstarts and get it back
   *
   * @param response response from neo
   * @return Seq of quickstarters
   */
  def parseNeoQuickstartList(response: WSResponse): Seq[Quickstart] =
    ((response.json \ "results")(0) \ "data").as[JsArray].value.map(jsValue => (jsValue \ "row")(0).as[Quickstart])


}
