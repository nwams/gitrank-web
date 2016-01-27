package models.daos.drivers

import play.api.libs.json.JsValue

/**
  * Represents a response from GitHub.
  *
  * @param endpoint URL to which the request has been made
  * @param nextPage URL of the next page if available
  * @param json body of the response parsed as JSON
  */
case class GitHubResponse (
                          endpoint: String,
                          nextPage: Option[String],
                          json: JsValue
                          )
