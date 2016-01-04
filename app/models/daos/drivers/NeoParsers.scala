package models.daos.drivers

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json._

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
    * Parses a Json to get a unique user out of it.
    *
    * @param json Json object gotten from the request to neo4j
    * @return The parsed user.
    */
  def parseNeoUser(json: JsValue): Option[User] = {
    (((json \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _ :JsUndefined => None
      case user => Some(parseSingleUser(user))
    }
  }

  /**
    * Parses a WsResponse to get all users out of it.
    *
    * @param json Json object gotten from the request to neo4j
    * @return The parsed users.
    */
  def parseNeoUsers(json: JsValue): Seq[User] = {
    val users = json \\ "user"

    users.length match {
      case 0 => Seq()
      case _ => users.map(parseSingleUser(_))
    }
  }

  /**
    * Parse a stream  with a list of  objects
    *
    * @param jsonParser json parser responsible for parsing the stream
    * @param callback callback function for each item
    */
  def parseJson(jsonParser: JsonParser, callback: (Any) => Future[Unit]): Unit ={
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
    * Parse a fragment of a User Json
    *
    * @param jsonParser parser with the whole json stream
    * @param callback callback function for each item
    */
  def parseJsonFragment(jsonParser: JsonParser, callback: (Any) => Future[Unit])= {
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
    * @param json Json object gotten from the request to neo4j
    * @return A Sequence of contributors
    */
  def parseNeoRepo(json: JsValue): Option[Repository] = {
    (((json \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case repo => Some(repo.as[Repository])
    }
  }

  /**
    * Parse a list of repositories
    *
    * @param json Json object gotten from the request to neo4j
    * @return a list of repositories
    */
  def parseNeoRepos(json: JsValue): Seq[Repository] = {
    ((json \ "results")(0) \ "data").as[JsArray].value.map(jsValue =>
      (jsValue \ "row")(0).as[Repository]
    )
  }

  /**
    * Should parse a result list of feedback and get it back
    *
    * @param json Json object gotten from the request to neo4j
    * @return Seq of Scores
    */
  def parseNeoFeedbackList(json: JsValue): Seq[Feedback] =
    ((json \ "results")(0) \ "data").as[JsArray].value.map(jsValue =>
      Feedback(parseSingleUser((jsValue \ "row")(1)), (jsValue \ "row")(0).as[Score]))

  /**
    * Should parse a result list of scores and get it back
    *
    * @param json Json object gotten from the request to neo4j
    * @return Seq of Scores
    */
  def parseNeoScoreList(json: JsValue): Seq[Score] =
    ((json \ "results")(0) \ "data").as[JsArray].value.map(jsValue => (jsValue \ "row")(0).as[Score])

  /**
    * Parses a neo Score into a model
    *
    * @param json Json object gotten from the request to neo4j
    * @return
    */
  def parseNeoScore(json: JsValue): Option[Score] = {
    (((json \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case score => score.asOpt[Score]
    }
  }

  /**
    * Parses a neo Quickstart into a model
    *
    * @param json Json object gotten from the request to neo4j
    * @return Quickstart object
    */
  def parseNeoQuickstart(json: JsValue): Option[Quickstart] = {
    (((json \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case score => Some((score \ "properties").as[Quickstart].copy(
        id=(score \ "id").asOpt[Int],
        owner = (score \ "owner").asOpt[Int]
      ))
    }
  }

  /**
    * Should parse a result list of quickstarts and get it back
    *
    * @param json Json object gotten from the request to neo4j
    * @return Seq of quickstarters
    */
  def parseNeoQuickstartList(json: JsValue): Seq[Quickstart] =
    ((json \ "results")(0) \ "data").as[JsArray].value.map(jsValue =>
      ((jsValue \ "row")(0) \ "properties").as[Quickstart].copy(
        id=((jsValue \ "row")(0) \ "id").asOpt[Int],
        owner = ((jsValue \ "row")(0) \ "owner").asOpt[Int]
      ))

  /**
    * Parses a json to get a unique OAuth2Info out of it.
    *
    * @param json Json object gotten from the request to neo4j
    * @return The parsed OAuth2Info.
    */
  def parseNeoOAuth2Info(json: JsValue) = {
    (((json \ "results")(0) \ "data")(0) \ "row")(0) match {
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

  implicit val oAuth2InfoWrites: Writes[OAuth2Info] = (
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
    * @param json Json object gotten from the request to neo4j
    * @return parsed contribution or None
    */
  def parseNeoContribution(json: JsValue): Option[Contribution] = {
    (((json \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case repo => repo.asOpt[Contribution]
    }
  }

  /**
    * Parse multiple contributions and repos
    *
    * @param json Json object gotten from the request to neo4j
    * @return map with each contribution from repo
    */
  def parseNeoContributions(json: JsValue): Seq[(Repository,Contribution)] = {
    (json \\ "row").map{contribution => (contribution(0).as[Repository], contribution(1).as[Contribution])}.seq
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

  /**
    * Parses a boolean result from neo4j
    *
    * @param json json from neo4j
    * @return Boolean
    */
  def parseNeoBoolean(json: JsValue): Boolean = {
    (((json \ "results")(0) \ "data")(0) \ "row")(0).as[Boolean]
  }
}
