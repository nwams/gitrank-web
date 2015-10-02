package unit.models.service

import models.daos.{ContributionDAO, RepositoryDAO, UserDAO}
import models.services.KarmaService
import models.{Contribution, Repository, User}
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._



@RunWith(classOf[JUnitRunner])
class KarmaServiceSpec extends Specification with Mockito{

  val userDAO = mock[UserDAO]
  val repositoryDAO = mock[RepositoryDAO]
  val contributionDAO = mock[ContributionDAO]
  val karmaService = new KarmaService(userDAO, repositoryDAO, contributionDAO )

  "karmaService#calculateKarma" should {
    "if user has no contributions karma should be 0" in {
      val user = mock[User]
      val contributionList = Seq[(Repository,Contribution)]()
      karmaService.calculateKarma(user, contributionList) shouldEqual 0
    }
    "if user has 1 contribution karma should be related to that" in {
      val user = mock[User]
      val repo = mock[Repository]
      val contrib = mock[Contribution]
      contrib.addedLines returns 10
      contrib.removedLines returns 10
      repo.score returns 4
      repo.addedLines returns 10
      repo.removedLines returns 20
      val contributionList = Seq[(Repository,Contribution)]((repo,contrib))
      karmaService.calculateKarma(user, contributionList) shouldEqual (((10.0+10)/30) *16).toInt
    }
    "if user has 2 contribution karma should be related to that" in {
      val user = mock[User]
      val repo = mock[Repository]
      val repo2 = mock[Repository]
      val contrib = mock[Contribution]
      contrib.addedLines returns 10
      contrib.removedLines returns 10
      repo.score returns 4
      repo.addedLines returns 10
      repo.removedLines returns 20
      repo2.score returns 4
      repo2.addedLines returns 10
      repo2.removedLines returns 20
      val contributionList = Seq[(Repository,Contribution)]((repo,contrib),(repo2,contrib))
      karmaService.calculateKarma(user, contributionList) shouldEqual (2*(((10.0+10)/30)*16)).toInt
    }
  }

}
