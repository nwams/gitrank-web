package modules

import actors.{UsersSupervisor, RepositorySupervisor}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport


class AkkaModule extends AbstractModule with AkkaGuiceSupport {

  def configure() = {
    bindActor[RepositorySupervisor]("repository-supervisor")
    bindActor[UsersSupervisor]("user-supervisor")
  }
}
