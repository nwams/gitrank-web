package models.daos.drivers

import java.net.ConnectException
import com.typesafe.config.ConfigFactory
import scalaj.http.Http

/**
 * Checks if the database is connected during creation of the injector. The application will fail to start
 * if the configuration is wrong
 *
 * This is not in the neo4j driver since the driver needs play to run and cannot be eagerly instantiated.
 */
class NeoChecker (){

  val conf = ConfigFactory.load

  val neo4jUsername = conf.getString("neo4j.username")
  val neo4jPassword = conf.getString("neo4j.password")
  val neo4jServer = conf.getString("neo4j.server")
  val neo4jEndpoint = conf.getString("neo4j.endpoint")

  try {
    Http(neo4jServer + neo4jEndpoint)
      .auth(neo4jUsername, neo4jPassword)
      .headers(("Accept", "application/json ; charset=UTF-8"), ("Content-Type", "application/json"))
      .asString
  } catch {
    case e:ConnectException => throw new Exception("Could not connect to the Neo4j database. Check configuration.")
  }
}
