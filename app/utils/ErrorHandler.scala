package utils

import javax.inject.Inject

import com.mohiva.play.silhouette.api.SecuredErrorHandler
import controllers.routes
import play.api.http.DefaultHttpErrorHandler
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{Results, Result, RequestHeader}
import play.api.routing.Router
import play.api.{UsefulException, OptionalSourceMapper, Configuration}

import scala.concurrent.Future

/**
 * A secured error handler.
 */
class ErrorHandler @Inject() (
                               env: play.api.Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: javax.inject.Provider[Router])
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
  with SecuredErrorHandler {

  /**
   * Called when a user is not authenticated.
   *
   * As defined by RFC 2616, the status code of the response should be 401 Unauthorized.
   *
   * @param request The request header.
   * @param messages The messages for the current language.
   * @return The result to send to the client.
   */
  override def onNotAuthenticated(request: RequestHeader, messages: Messages): Option[Future[Result]] = {
    Some(Future.successful(Redirect(routes.ApplicationController.index())))
  }

  /**
   * Called when a user is authenticated but not authorized.
   *
   * As defined by RFC 2616, the status code of the response should be 403 Forbidden.
   *
   * @param request The request header.
   * @param messages The messages for the current language.
   * @return The result to send to the client.
   */
  override def onNotAuthorized(request: RequestHeader, messages: Messages): Option[Future[Result]] = {
    Some(Future.successful(Redirect(routes.ApplicationController.index()).flashing("error" -> Messages("access.denied")(messages))))
  }

  /**
   * Invoked when a client error occurs, that is, an error in the 4xx series.
   *
   * @param request The request that caused the client error.
   * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
   * @param message The error message.
   */
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = statusCode match {
    case BAD_REQUEST => onBadRequest(request, message)
    case FORBIDDEN => onForbidden(request, message)
    case NOT_FOUND => Future.successful(NotFound(views.html.error(
      "notFound", statusCode, "Not Found", "We cannot find the requested page, try something else !"
    )))
    case clientError if statusCode >= 400 && statusCode < 500 =>
      Future.successful(Results.Status(clientError)(views.html.defaultpages.badRequest(request.method, request.uri, message)))
    case nonClientError =>
      throw new IllegalArgumentException(s"onClientError invoked with non client error status code $statusCode: $message")
  }

  /**
   * Invoked when an error occurs in production
   *
   * @param request request header of the error
   * @param exception exception raised
   * @return
   */
  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    Future.successful(
      InternalServerError(views.html.error(
        "Error", 500, "Server Error", "Looks like something is going wrong, please" +
          " <a href=\"https://github.com/gitlinks/gitrank-web/issues\"> report </a> what happened."
      ))
    )
  }
}
