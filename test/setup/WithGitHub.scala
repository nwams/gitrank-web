package setup

import java.io.File

import akka.actor._
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory
import org.mashupbots.socko.handlers.{StaticFileRequest, StaticContentHandler, StaticContentHandlerConfig}
import org.mashupbots.socko.routes.{Routes, _}
import org.mashupbots.socko.webserver.{WebServer, WebServerConfig}
import org.specs2.specification.BeforeAfterAll

/**
  * Created by nicolas on 11/6/15.
  */
trait WithGitHub extends BeforeAfterAll {

  val actorConfig = """
      my-pinned-dispatcher {
        type=PinnedDispatcher
        executor=thread-pool-executor
      }
      my-static-content-handler {
		    root-file-paths="/"
		  }
      akka {
        actor {
          deployment {
            /static-file-router {
              router = round-robin
              nr-of-instances = 5
            }
          }
        }
      }"""

  val actorSystem = ActorSystem("MockGitHubServer", ConfigFactory.parseString(actorConfig))

  val handlerConfig = MyStaticHandlerConfig(actorSystem)

  val staticContentHandlerRouter = actorSystem.actorOf(Props(new StaticContentHandler(handlerConfig))
    .withRouter(FromConfig()).withDispatcher("my-pinned-dispatcher"), "static-file-router")

  val routes = Routes({
    case HttpRequest(request) => request match {
      case (GET(Path("/search/repositories"))) => {
        staticContentHandlerRouter ! new StaticFileRequest(request,
          new File("test/resources/github", "search_repositories.json"))
      }
      case (GET(Path("/users/callicles/starred"))) => {
        staticContentHandlerRouter ! new StaticFileRequest(request,
          new File("test/resources/github", "callicles_starred.json"))
      }
    }
  })

  val webServer = new WebServer(WebServerConfig(serverName="GitHub Server", port=19002), routes, actorSystem)

  override def beforeAll = {
    webServer.start()
  }

  override def afterAll = {
    webServer.stop()
  }
}

object MyStaticHandlerConfig extends ExtensionId[StaticContentHandlerConfig] with ExtensionIdProvider {
  override def lookup = MyStaticHandlerConfig
  override def createExtension(system: ExtendedActorSystem) =
    new StaticContentHandlerConfig(system.settings.config, "my-static-content-handler")
}
