package models.daos.drivers

import com.fasterxml.jackson.core.{JsonToken, JsonParser}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

/**
  * Created by nicolas on 11/4/15.
  */
class NeoParsers {

  /**
    * Parser responsible for parsing the jsLookup
    *
    * @param user jsLookup for single user
    * @return a single user
    */
  def parseSingleUser(user : JsLookup): User = {
    val loginInfo = (user \ "loginInfo").as[String]
    val logInfo = loginInfo.split(":")
    User(
      LoginInfo(logInfo(0), logInfo(1)),
      (user \ "username").as[String],
      (user \ "fullName").asOpt[String],
      (user \ "email").asOpt[String],
      (user \ "avatarURL").asOpt[String],
      (user \ "karma").as[Int],
      (user \ "publicEventsETag").asOpt[String],
      (user \ "lastPublicEventPull").asOpt[Long]
    )
  }

  /**
    * Parses a WsResponse to get a unique user out of it.
    *
    * @param response response object
    * @return The parsed user.
    */
  def parseNeoUser(response: WSResponse): Option[User] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _ :JsUndefined => None
      case user => Some(parseSingleUser(user))
    }
  }

  /**
    * Parses a WsResponse to get all users out of it.
    *
    * @param response response object
    * @return The parsed users.
    */
  def parseNeoUsers(response: WSResponse): Seq[User] = {
    val users = Json.parse(response.body) \\ "user"

    users.length match {
      case 0 => Seq()
      case _ => users.map(parseSingleUser(_))
    }
  }

  /**
    * Parse a stream  with a list of  objects
    * @param jsonParser json parser responsible for parsing the stream
    * @param callback callback function for each item
    */
  def parseJson( jsonParser: JsonParser,callback: (Any) => Future[Unit]): Unit ={
    jsonParser.setCodec(new ObjectMapper())

    jsonParser.getCurrentName match {
      case "row" => {
        Stream.cons(
          parseJsonFragment(jsonParser,callback),
          Stream.continually(parseJsonFragment(jsonParser,callback))
        ).find( x => jsonParser.nextToken() == JsonToken.END_ARRAY)
      }
      case _ => Option(jsonParser.nextToken()).foreach(jsonToken => parseJson(jsonParser, callback))

    }
  }

  /**
    *
    * Parse a fragment of a User Json
    * @param jsonParser parser with the whole json stream
    * @param callback callback function for each item
    */
  def parseJsonFragment(jsonParser: JsonParser,callback: (Any) => Future[Unit])= {
    jsonParser.getCurrentToken match{
      case JsonToken.START_OBJECT =>{
        val jsonTree : JsonNode = jsonParser.readValueAsTree[JsonNode]()
        val loginInfo = jsonTree.get("loginInfo").asText().split(":")
        callback(Some(User(
          LoginInfo(loginInfo(0), loginInfo(1)),
          jsonTree.get("username").asText(),
          Option(jsonTree.get("fullName")).map(_.asText),
          Option(jsonTree.get("email")).map(_.asText),
          Option(jsonTree.get("avatarURL")).map(_.asText),
          jsonTree.get("karma").asInt(),
          Option(jsonTree.get("publicEventsETag")).map(_.asText),
          Option(jsonTree.get("lastPublicEventPull")).map(_.asLong())
        )))
      }
      case _ =>
    }
  }

  /**
    * Gets all the contributors for a given repository with all their contributions
    *
    * @param response response from neo
    * @return A Sequence of contributors
    */
  def parseNeoRepo(response: WSResponse): Option[Repository] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case repo => Some(repo.as[Repository])
    }
  }

  /**
    * Parse a list of repositories
    *
    * @param response response from neo
    * @return a list of repositories
    */
  def parseNeoRepos(response: WSResponse): Seq[Repository] = {
    ((Json.parse(response.body) \ "results")(0) \ "data").as[JsArray].value.map(jsValue =>
      jsValue.as[Repository])
  }

  /**
    * Should parse a result list of feedback and get it back
    *
    * @param response response from neo
    * @return Seq of Scores
    */
  def parseNeoFeedbackList(response: WSResponse): Seq[Feedback] =
    ((response.json \ "results")(0) \ "data").as[JsArray].value.map(jsValue =>
      Feedback(parseSingleUser((jsValue \ "row")(1)), (jsValue \ "row")(0).as[Score]))

  /**
    * Should parse a result list of scores and get it back
    *
    * @param response response from neo
    * @return Seq of Scores
    */
  def parseNeoScoreList(response: WSResponse): Seq[Score] =
    ((response.json \ "results")(0) \ "data").as[JsArray].value.map(jsValue => (jsValue \ "row")(0).as[Score])

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

  /**
    * Parses a WsResponse to get a unique OAuth2Info out of it.
    *
    * @param response response object
    * @return The parsed OAuth2Info.
    */
  def parseNeoOAuth2Info(response: WSResponse) = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case repo => Some(OAuth2Info(
        (repo \ "accessToken").as[String],
        (repo \ "tokenType").asOpt[String],
        (repo \ "expiresIn").asOpt[Int],
        (repo \ "refreshToken").asOpt[String],
        (repo \ "params").asOpt[Map[String, String]]
      ))
    }
  }

  implicit val OAuth2InfoWrites: Writes[OAuth2Info] = (
    (JsPath \ "accessToken").write[String] and
      (JsPath \ "tokenType").writeNullable[String] and
      (JsPath \ "expiresIn").writeNullable[Int] and
      (JsPath \ "refreshToken").writeNullable[String] and
      (JsPath \ "params").writeNullable[Map[String, String]]
    )(unlift(OAuth2Info.unapply))

  /**
    * Writer to get the Oauth Info to Json
    *
    * @param authInfo token information
    * @return Json to write to the Database
    */
  def writeNeoOAuth2Info(authInfo: OAuth2Info) = {
    val jsonAuth = Json.toJson(authInfo)
    val params = (jsonAuth \ "params").asOpt[JsValue]

    params match {
      case None => jsonAuth
      case Some(p) => (jsonAuth.as[JsObject] - "params") ++ Json.obj("params" -> Json.stringify(p))
    }
  }

  /**
    * Parses a neo4j response to get a Contribution out of it.
    *
    * @param response neo4j response
    * @return parsed contribution or None
    */
  def parseNeoContribution(response: WSResponse): Option[Contribution] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case repo => repo.asOpt[Contribution]
    }
  }

  /**
    * Parse multiple contributions and repos
    * @param response response from neo4j
    * @return map with each contribution from repo
    */
  def parseNeoContributions(response: WSResponse): Seq[(Repository,Contribution)] = {
    (Json.parse(response.body) \\ "row").map{contribution => (contribution(0).as[Repository], contribution(1).as[Contribution])}.seq
  }

  /**
    * Parses a string representing the buffer of the current week contribution getting the deleted lines
    *
    * @param currentWeekBuffer String containing the deleted lines this week already counted for needed to be extracted.
    * @return count of the deleted lines already accounted for extracted as an Int
    */
  def parseWeekDeletedLines(currentWeekBuffer: Option[String]): Int = {
    val str = currentWeekBuffer.getOrElse("a0d0")
    str.substring(str.indexOf("d"), str.length).toInt
  }

  /**
    * Parses a string representing the buffer of the current week contribution getting the added lines
    *
    * @param currentWeekBuffer String containing the added lines this week already counted for needed to be extracted.
    * @return count of the added lines already accounted for extracted as an Int
    */
  def parseWeekAddedLines(currentWeekBuffer: Option[String]): Int = {
    val str = currentWeekBuffer.getOrElse("a0d0")
    str.substring(0, str.indexOf("d")).toInt
  }
}
