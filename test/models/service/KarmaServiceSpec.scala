package models.service


import java.util.Dictionary

import com.fasterxml.jackson.core.JsonFactory
import models.{Repository, Contribution, User}
import models.daos.{ContributionDAO, RepositoryDAO, UserDAO}
import models.daos.drivers.Neo4J
import models.services.KarmaService
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.Future



@RunWith(classOf[JUnitRunner])
class KarmaServiceSpec extends Specification with Mockito{

  var userDAO = mock[UserDAO]
  var repositoryDAO = mock[RepositoryDAO]
  var contributionDAO = mock[ContributionDAO]
  var karmaService = new KarmaService(userDAO, repositoryDAO, contributionDAO )

  "karmaService#calculateKarma" should {
    "if user has no contributions karma should be 0" in {
      var user = mock[User]
      var contributionList = HashMap[Repository,Contribution]()
      karmaService.calculateKarma(user, contributionList) shouldEqual 0
    }
    "if user has 1 contribution karma should be related to that" in {
      var user = mock[User]
      var repo = mock[Repository]
      var contrib = mock[Contribution]
      contrib.addedLines returns 10
      contrib.removedLines returns 10
      repo.score returns 4
      repo.addedLines returns 10
      repo.removedLines returns 20
      var contributionList = HashMap[Repository, Contribution]()
      contributionList + (repo->contrib)
      karmaService.calculateKarma(user, contributionList) shouldEqual (((10.0+10)/30) *16).toInt
    }
    "if user has 2 contribution karma should be related to that" in {
      var user = mock[User]
      var repo = mock[Repository]
      var repo2 = mock[Repository]
      var contrib = mock[Contribution]
      contrib.addedLines returns 10
      contrib.removedLines returns 10
      repo.score returns 4
      repo.addedLines returns 10
      repo.removedLines returns 20
      repo2.score returns 4
      repo2.addedLines returns 10
      repo2.removedLines returns 20
      var contributionList = HashMap[Repository, Contribution]()
      contributionList + (repo->contrib)
      contributionList + (repo2->contrib)
      karmaService.calculateKarma(user, contributionList) shouldEqual (2*(((10.0+10)/30)*16)).toInt
    }
  }

}
