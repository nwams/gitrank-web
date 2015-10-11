package unit.models.service

import java.util.Date

import models.daos.{QuickstartDAO, ContributionDAO, RepositoryDAO, UserDAO}
import models.services.{QuickstartService, KarmaService}
import models.{Quickstart, Contribution, Repository, User}
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global



@RunWith(classOf[JUnitRunner])
class QuickstartServiceSpec extends Specification with Mockito {

  val quickstartDAO = mock[QuickstartDAO]


  "quickstartService#createQuickstart" should {
    "format correctly the begining of the url" in {
      val quickstartService = new QuickstartService(quickstartDAO)
      val quickstart = quickstartService.createQuickstart(mock[User], mock[Repository], "title", "description", "www.google.com")
      quickstart.url shouldEqual "http://www.google.com"
    }
    "properly initialize values" in {
      val quickstartService = new QuickstartService(quickstartDAO)
      val quickstart = quickstartService.createQuickstart(mock[User], mock[Repository], "title", "description", "http://www.google.com")
      quickstart.url shouldEqual "http://www.google.com"
      quickstart.upvote shouldEqual 0
      quickstart.downvote shouldEqual 0
    }

  }
  "quickstartService#buildFromVote" should {
    "update the upvote correctly and put user in list" in {
      val quickstartService = new QuickstartService(quickstartDAO)
      val guide = quickstartService.createQuickstart(mock[User], mock[Repository], "title", "description", "www.google.com")
      val quickstart = quickstartService.buildFromVote(guide, true, "username")
      quickstart.upvote shouldEqual 1
      quickstart.downvote shouldEqual 0
      quickstart.listVoters should contain("username")
    }

    "update the upvote correctly and put user in list" in {
      val quickstartService = new QuickstartService(quickstartDAO)
      val guide = quickstartService.createQuickstart(mock[User], mock[Repository], "title", "description", "www.google.com")
      val quickstart = quickstartService.buildFromVote(guide, false, "username")
      quickstart.upvote shouldEqual 0
      quickstart.downvote shouldEqual 1
      quickstart.listVoters should contain("username")
    }
  }

  "quickstartService#updateVote" should {
    "update if user is not on list" in {
      val repo = mock[Repository]
      repo.name returns "reponame"
      val quickstartDAOMock = mock[QuickstartDAO]
      val quickstartService = new QuickstartService(quickstartDAOMock)
      quickstartDAOMock.findRepositoryGuide(anyString, anyString) returns Future(Option(quickstartService.createQuickstart(mock[User], mock[Repository], "title", "description", "www.google.com")))
      quickstartService.updateVote(repo, true, "username", mock[User])
      there was one(quickstartDAOMock).update(anyString, anyString, any[Quickstart])
    }
    "not update is user is on list" in {
      val user = mock[User]
      user.username returns "username"
      val repo = mock[Repository]
      val quickstartDAOMock = mock[QuickstartDAO]
      val quickstartService = new QuickstartService(quickstartDAOMock)
      val guide = quickstartService.createQuickstart(mock[User], mock[Repository], "title", "description", "www.google.com")
      repo.name returns "reponame"
      quickstartDAOMock.findRepositoryGuide(anyString, anyString) returns Future(Option(quickstartService.buildFromVote(guide, true, "username")))
      quickstartService.updateVote(repo, true, "title", user)
      quickstartService.updateVote(repo, true, "title", user)
      there was no(quickstartDAOMock).update(anyString, anyString, any[Quickstart])
    }

  }

}
