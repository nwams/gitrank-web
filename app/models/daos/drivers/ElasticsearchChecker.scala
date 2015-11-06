package models.daos.drivers

import java.net.ConnectException

import com.sendgrid.SendGrid
import com.typesafe.config.ConfigFactory
import play.api.Play

import scala.util.{Failure, Try}
import scalaj.http.Http

/**
 * Checks if the database is connected during creation of the injector. The application will fail to start
 * if the configuration is wrong
 *
 * This is not in the neo4j driver since the driver needs play to run and cannot be eagerly instantiated.
 */
class ElasticsearchChecker() {

  val conf = ConfigFactory.load

  val elasticSearchAPIUrl = conf.getString("elasticsearch.server")
  val elasticSearchAPISearchEndpoint = conf.getString("elasticsearch.endpoint")
  val sgRecipients = conf.getString("sendgrid.recipients").split(",")


  Try(
    Http(elasticSearchAPIUrl + elasticSearchAPISearchEndpoint)
      .headers(("Accept", "application/json ; charset=UTF-8"), ("Content-Type", "application/json"))
      .asString
  ) match {
    case Failure(e)=>{
      val email = new SendGrid.Email()
      email.setTo(sgRecipients)
      email.setText("Elasticsearch is not working "+e.getMessage )
      email.setFrom("gitrank@gitrank.io")
      email.setSubject("Error detected on deployment environment- Elasticsearch")
      new SendGrid(conf.getString("sendgrid.key")).send(email)
    }
    case _ =>

  }
}
