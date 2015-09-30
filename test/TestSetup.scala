import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws._

import scala.collection.mutable
import scala.concurrent.Future

/**
 * Created by nicolas on 9/13/15.
 */
object TestSetup {

  val neo4jEndpoint = "http://localhost:7474/db/data/"
  val neo4jUser = sys.env("NEO4J_USER")
  val neo4jPassword = sys.env("NEO4J_PASSWORD")

  /**
   * Clears all the data from the neo4J database
   */
  def clearNeo4JData = cypher("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r")

  /**
   * Populates the database with the data in the json file test/resources/neo4j.json
   *
   * @return void
   */
  def populateNeo4JData() = {

    clearNeo4JData

    val source = scala.io.Source.fromFile("resources/neo4j.json")
    val lines = try source.mkString finally source.close()

    // We use a mutable map to create an index of all the nodes indexes in the database to be able to create
    // the relationships
    val mapping = new mutable.HashMap[Int,Int]()

    val neo4jData = Json.parse(lines)

    (neo4jData \ "nodes").as[Seq[JsObject]].zipWithIndex.map{ case (node: JsObject, i: Int) =>
      cypher("CREATE (n:" + formatLabels(node) + " {props}) return id(n)",
        Json.obj("props" -> (node \ "properties").as[JsObject])
      ).map(res => mapping.put(i, (((res.json \ "results")(0) \ "data")(0) \ "row")(0).as[Int]))
    }.map(_ => (neo4jData \ "edges").as[Seq[JsObject]].zipWithIndex.map { case (edge: JsObject, i: Int) =>
      cypher(
        """
          MATCH (a),(b)
          WHERE id(a)={ida} AND id(b)={idb}
          CREATE (a)-[c:{type} {props}]->(b)
        """, Json.obj(
          "props" -> (edge \ "properties").as[JsObject],
          "type" -> (edge \ "type").as[String],
          "ida" -> mapping.get((edge \ "sourceIndex").as[Int]).get,
          "idb" -> mapping.get((edge \ "targetIndex").as[Int]).get
        )).map(res =>
        mapping.put(i, (((res.json \ "results")(0) \ "data")(0) \ "row")(0).as[Int])
        )
    })
  }

  /**
   * Creates a correctly formatted string to denote a node labels
   *
   * @param node represents a node in neo4j, should have a labels field
   * @return String as a list of labels separated by columns
   */
  private def formatLabels(node: JsObject): String = (node \ "labels").as[Seq[String]].mkString(":")

  /**
   * Makes a cypher query to neo4j
   *
   * @param query query to make
   * @param parameters parameters to be used
   * @return
   */
  private def cypher(query: String, parameters: JsObject = Json.obj()): Future[WSResponse] = WS
    .url(neo4jEndpoint + "transaction/commit")
    .withAuth(neo4jUser, neo4jPassword, WSAuthScheme.BASIC)
    .withHeaders("Accept" -> "application/json ; charset=UTF-8", "Content-Type" -> "application/json")
    .withRequestTimeout(10000)
    .post(Json.obj(
    "statements" -> Json.arr(
      Json.obj(
        "statement" -> query,
        "parameters" -> parameters
      )
    )
  ))
}
