package modules

import com.google.inject.AbstractModule
import models.daos.drivers.{ElasticsearchChecker, ConnectionChecker}
import net.codingwell.scalaguice.ScalaModule

/**
 * Created by nicolas on 10/25/15.
 */
class GitrankModule extends AbstractModule with ScalaModule{

  def configure() = {
    bind[ConnectionChecker].asEagerSingleton()
    bind[ElasticsearchChecker].asEagerSingleton()

  }
}
