package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import models.User
import models.services.UserService
import modules.CustomGitHubProvider
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Action

import scala.concurrent.Future

/**
 * The social auth controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param userService The user service implementation.
 * @param authInfoRepository The auth info service implementation.
 * @param gitHubProvider GitHub Provider
 */
class SocialAuthController @Inject() (
                                       val messagesApi: MessagesApi,
                                       val env: Environment[User, SessionAuthenticator],
                                       userService: UserService,
                                       authInfoRepository: AuthInfoRepository,
                                       gitHubProvider: CustomGitHubProvider)
  extends Silhouette[User, SessionAuthenticator] with Logger {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticate(provider: String) = Action.async { implicit request =>
    gitHubProvider.authenticate().flatMap {
      case Left(result) => Future.successful(result.withSession(("login-referrer", request.headers.get(REFERER).get)))
      case Right(authInfo) => for {
        profile <- gitHubProvider.retrieveProfile(authInfo)
        user <- userService.save(profile, authInfo)
        authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
        authenticator <- env.authenticatorService.create(profile.loginInfo)
        value <- env.authenticatorService.init(authenticator)
        result <- env.authenticatorService.embed(value,
          request.session.get("login-referrer") match {
            case Some(ref) => Redirect(ref)
            case None => Redirect(routes.ApplicationController.index())
          })
      } yield {
          env.eventBus.publish(LoginEvent(user, request, request2Messages))
          result
        }
    }
  }
}
