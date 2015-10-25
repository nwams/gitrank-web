package models.services

import java.util.Date
import javax.inject.Inject

import models.daos.QuickstartDAO
import models.{Quickstart, Repository, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QuickstartService @Inject()(
                                   quickstartDAO: QuickstartDAO,
                                   repositoryService: RepositoryService) {

  /**
   * Create a quickstart guide
   * @param user user who made the guide
   * @param repoName repo name for whom the guide is intended
   * @param title title of the quick starter
   * @param description descritption of the quick starter
   * @param url url
   * @return guide created
   */
  def createQuickstart(user: User, repoName: String, title: String, description: String, url: String): Future[Quickstart] = {
    repositoryService.findOrCreate(user, repoName).flatMap(repo => {

      val quickstart = Quickstart(
        new Date(),
        title,
        description,
        if (url.startsWith("http")) url else "http://" + url,
        0,
        0,
        List()
      )

      quickstartDAO.save(user.username, repo.name, quickstart).map({
        case Some(quick) => quick
        case None => throw new Exception("Quickstart not created properly")
      })
    })
  }

  /**
   * Get all quickstart guides from repo
   * @param repository repository
   * @return sequence of guides
   */
  def getQuickstartGuidesForRepo(repository: Repository, page: Int = 1): Future[Seq[Quickstart]] = {
    quickstartDAO.findRepositoryGuides(repository.name, page)
  }

  /**
   * Build a Quickstart from a vote and a previous version of the guide
   *
   * @param guide guide to be updated
   * @param upVote vote true for up false for down
   * @param username name of the voter
   * @return
   */
  def buildFromVote(guide: Quickstart, upVote: Boolean, username: String): Quickstart = {
    Quickstart(
      guide.timestamp,
      guide.title,
      guide.description,
      if (guide.url.startsWith("http")) guide.url else "http://" + guide.url,
      guide.upvote + (if (upVote) 1 else 0),
      guide.downvote + (if (!upVote) 1 else 0),
      guide.listVoters :+ username
    )
  }

  /**
   * Update downvote and upVote of a guide on given repo
   * @param repository repo on which the guide is
   * @param upVote is it upVote?
   * @param title title of the guide
   */
  def updateVote(repository: Repository, upVote: Boolean, title: String, user: User): Future[Option[Quickstart]] = {
    quickstartDAO.findRepositoryGuide(repository.name, title).flatMap {
      case Some(guide) => {
        if (!guide.listVoters.contains(user.username)) {
          quickstartDAO.update(title, repository.name, buildFromVote(guide, upVote, user.username))
        } else {
          Future(Some(guide))
        }
      }
      case None => Future(None)
    }
  }
}
