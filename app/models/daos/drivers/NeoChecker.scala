package models.daos.drivers

import java.net.ConnectException
import javax.inject.Inject
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import scalaj.http.Http

/**
 * Checks if the database is connected during creation of the injector. The application will fail to start
 * if the configuration is wrong
 *
 * This is not in the neo4j driver since the driver needs play to run and cannot be eagerly instantiated.
 */
class NeoChecker @Inject()(configuration: Configuration){

  val conf = ConfigFactory.load

  val neo4jUsername = configuration.getString("neo4j.username").getOrElse(Neo4jDefaults.user)
  val neo4jPassword = configuration.getString("neo4j.password").getOrElse(Neo4jDefaults.password)
  val neo4jServer = configuration.getString("neo4j.server").getOrElse(Neo4jDefaults.serverAddress)
  val neo4jEndpoint = configuration.getString("neo4j.endpoint").getOrElse(Neo4jDefaults.endpoint)

  try {
    Http(neo4jServer + neo4jEndpoint)
      .auth(neo4jUsername, neo4jPassword)
      .headers(("Accept", "application/json ; charset=UTF-8"), ("Content-Type", "application/json"))
      .asString
  } catch {
    case e:ConnectException => throw new Exception("Could not connect to the Neo4j database. Check configuration.")
  }
}
