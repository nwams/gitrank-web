package models.services

import java.util.Date
import javax.inject.Inject

import models.{Score, Quickstart, Repository, User}
import models.daos.{QuickstartDAO, ScoreDAO}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QuickstartService @Inject()(quickstartDAO: QuickstartDAO) {

  /**
   * Create a quickstart guide
   * @param user user who made the guide
   * @param repository repo for whom the guide is intended
   * @param title title of the quick starter
   * @param description descritption of the quick starter
   * @param url url
   * @return guide created
   */
  def createQuickstart(user: User, repository: Repository, title: String, description: String, url: String): Quickstart = {
    val quickstart = Quickstart(
      new Date(),
      title,
      description,
      (if (url.startsWith("http")) url else "http://" + url),
      0,
      0,
      List()
    )
    quickstartDAO.save(user.username, repository.name, quickstart)
    quickstart
  }

  /**
   * Get all quickstart guides from repo
   * @param repository repository
   * @return sequence of guides
   */
  def getQuickstartGuidesForRepo(repository: Repository, page: Int = 1): Future[Seq[Quickstart]] = {
    quickstartDAO.findRepositoryGuides(repository.name, page)
  }

  def buildFromVote(guide: Quickstart, upvote: Boolean, username: String): Quickstart = {
    Quickstart(
      guide.timestamp,
      guide.title,
      guide.description,
      (if (guide.url.startsWith("http")) guide.url else "http://" + guide.url),
      guide.upvote + (if (upvote) 1 else 0),
      guide.downvote + (if (!upvote) 1 else 0),
      guide.listVoters :+ username
    )
  }

  /**
   * Update downvote and upvote of a guide on given repo
   * @param repository repo on which the guide is
   * @param upvote is it upvote?
   * @param title title of the guide
   */
  def updateVote(repository: Repository, upvote: Boolean, title: String, user: User): Future[Option[Quickstart]] = {
    quickstartDAO.findRepositoryGuide(repository.name, title).flatMap {
      case Some(guide) => {
        if (!guide.listVoters.contains(user.username)) {
          quickstartDAO.update(title, repository.name, buildFromVote(guide, upvote, user.username))
        }
        else Future(None)
      }
      case None => Future(None)
    }
  }
}
