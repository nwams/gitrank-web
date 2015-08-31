package modules

import actors.GitHubActor
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport


class AkkaModule extends AbstractModule with AkkaGuiceSupport {

  def configure() = {
    bindActor[GitHubActor]("github-actor")
  }
}