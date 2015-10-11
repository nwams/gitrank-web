package modules

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.exceptions._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2._
import models.Identifiable
import org.eclipse.egit.github.core.client.GitHubClient
import play.api.libs.json.{JsArray, JsValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class CustomSocialProfile(
                                loginInfo: LoginInfo,
                                username: String,
                                fullName: Option[String] = None,
                                email: Option[String] = None,
                                avatarURL: Option[String] = None
                                ) extends SocialProfile with Identifiable

class CustomGitHubProfileParser extends SocialProfileParser[JsValue, CustomSocialProfile] {

  /**
   * The common social profile parser.
   */
  val commonParser = new GitHubProfileParser

  /**
   * Parses the social profile.
   *
   * @param json The content returned from the provider.
   * @return The social profile from given result.
   */
  def parse(json: JsValue) = commonParser.parse(json).map { commonProfile =>

    val username = (json \ "login").as[String]
    val avatarUrl = (json \ "avatar_url").as[String]

    CustomSocialProfile(
      loginInfo = commonProfile.loginInfo,
      username = username,
      fullName = commonProfile.fullName,
      avatarURL = Some(avatarUrl),
      email = commonProfile.email)
  }
}

class CustomGitHubProvider(
  protected val httpLayer: HTTPLayer,
  protected val stateProvider: OAuth2StateProvider,
  val settings: OAuth2Settings)
  extends BaseGitHubProvider {

  /**
   * The type of this class.
   */
  type Self = CustomGitHubProvider

  /**
   * The type of the profile a profile builder is responsible for.
   */
  type Profile = CustomSocialProfile

  /**
   * The profile parser.
   */
  val profileParser = new CustomGitHubProfileParser

  /**
   * The URI to be used to get the email of the user
   */
  val gitHubEmailURI = "https://api.github.com/user/emails?access_token=%s"

  /**
   * Builds the social profile.
   *
   * @param authInfo The auth info received from the provider.
   * @return On success the build social profile, otherwise a failure.
   */
  override protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {
    httpLayer.url(urls("api").format(authInfo.accessToken)).get().flatMap { response =>
      val json = response.json
      (json \ "message").asOpt[String] match {
        case Some(msg) =>
          val docURL = (json \ "documentation_url").asOpt[String]

          throw new ProfileRetrievalException("[Silhouette][%s] Error retrieving profile information. Error message: %s, doc URL: %s".format(id, msg, docURL))
        case _ => profileParser.parse(json)
      }
    }.flatMap(
      profile => profile.email match {
        case None =>  httpLayer
          .url(gitHubEmailURI.format(authInfo.accessToken))
          .get()
          .map(res => profile.copy(email = Some(getPrimaryEmail(res.json.as[JsArray].value))))
        case _ => Future(profile)
      }
    )
  }

  /**
   * Gets a provider initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the provider initialized with new settings.
   */
  def withSettings(f: (Settings) => Settings) = {
    new CustomGitHubProvider(httpLayer, stateProvider, f(settings))
  }

  /**
   * Used to parse the email list for the user.
   *
   * @param jsonList list of potential emails. We only use an active and primary email.
   * @return email as a string
   */
  def getPrimaryEmail(jsonList: Seq[JsValue]): String = {
    if ((jsonList.head \ "primary").as[Boolean] && (jsonList.head \ "verified").as[Boolean])
      (jsonList.head \ "email").as[String]
    else
      getPrimaryEmail(jsonList.tail)
  }

}