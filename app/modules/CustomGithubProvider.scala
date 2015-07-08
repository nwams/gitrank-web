package modules

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.providers.oauth2.{GitHubProfileParser, BaseGitHubProvider}
import com.mohiva.play.silhouette.impl.providers._
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global

case class CustomSocialProfile(
                                loginInfo: LoginInfo,
                                username: Option[String] = None,
                                fullName: Option[String] = None,
                                email: Option[String] = None,
                                avatarURL: Option[String] = None
                                ) extends SocialProfile

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
    CustomSocialProfile(
      loginInfo = commonProfile.loginInfo,
      username = Some(username),
      fullName = commonProfile.fullName,
      avatarURL = commonProfile.avatarURL,
      email = commonProfile.email)
  }
}

class CustomGitHubProvider (
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
     * Gets a provider initialized with a new settings object.
     *
     * @param f A function which gets the settings passed and returns different settings.
     * @return An instance of the provider initialized with new settings.
     */
    def withSettings(f: (Settings) => Settings) = {
      new CustomGitHubProvider(httpLayer, stateProvider, f(settings))
    }
}
