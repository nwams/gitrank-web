package models.dao

import java.io.ByteArrayInputStream

import com.fasterxml.jackson.core.JsonFactory
import models.User
import models.daos.UserDAO
import models.daos.drivers.Neo4J
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.Future


/**
 * Created by brunnoattorre1 on 8/30/15.
 */
@RunWith(classOf[JUnitRunner])
class UsersDAOSpec extends  Specification with Mockito{

  val neo4jMock = mock[Neo4J]
  val mockedFunction = mock[UsersDAOSpec]


  def callback (user:Any): Future[Unit] ={
    user.asInstanceOf[Some[User]].get shouldNotEqual null
    return null
  }

  "userDAO#parseJson" should {
      "ParseJsonWithSingleUser" in {
        var goodJsonParser = new JsonFactory().createParser("{\"results\":[{\"columns\":[\"n\"],\"data\":[{\"row\":[{\"email\":\"brattorre@gmail.com\",\"username\":\"brunnoattorre\",\"fullName\":\"Brunno Attorre\",\"avatarURL\":\"https://avatars.githubusercontent.com/u/5482242?v=3\",\"loginInfo\":\"github:5482242\",\"karma\":0}]}]}],\"errors\":[]}");
        val dao = new UserDAO(neo4jMock)
        dao.parseJson(goodJsonParser, callback) mustEqual null
      }
    "ParseJsonWithMultipleUsers" in {
      var goodJsonParserTwoUsers = new JsonFactory().createParser("{\"results\":[{\"columns\":[\"n\"],\"data\":[{\"row\":[{\"email\":\"brattorre@gmail.com\",\"username\":\"brunnoattorre\",\"fullName\":\"Brunno Attorre\",\"avatarURL\":\"https://avatars.githubusercontent.com/u/5482242?v=3\",\"loginInfo\":\"github:5482242\",\"karma\":0},{\"email\":\"brattorre@gmail.com\",\"username\":\"brunnoattorre\",\"fullName\":\"Brunno Attorre\",\"avatarURL\":\"https://avatars.githubusercontent.com/u/5482242?v=3\",\"loginInfo\":\"github:5482242\",\"karma\":0}]}]}],\"errors\":[]}");
      val dao = new UserDAO(neo4jMock)
      dao.parseJson(goodJsonParserTwoUsers, mockedFunction.callback)
      there was two(mockedFunction).callback(any[Any])
    }
    "ParseJsonWithNoUsers" in {
      var goodJsonParserNoUsers = new JsonFactory().createParser("{\"results\":[{\"columns\":[\"n\"],\"data\":[{\"row\":[]}]}],\"errors\":[]}");
      val dao = new UserDAO(neo4jMock)
      dao.parseJson(goodJsonParserNoUsers, mockedFunction.callback)
      there was no(mockedFunction).callback(any[Any])
    }
  }



}
