package unit.models.dao.drivers

import java.io.ByteArrayInputStream

import com.fasterxml.jackson.core.JsonFactory
import models.User
import models.daos.drivers.NeoParsers
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by brunnoattorre1 on 8/30/15.
 */
@RunWith(classOf[JUnitRunner])
class NeoParsersSpec extends Specification with Mockito{

  def callback (user:Any): Future[Unit] ={
    Future(
      user.asInstanceOf[Some[User]].get shouldNotEqual null
    )
  }

  "NeoParsers#parseJson" should {
    "ParseJsonWithSingleUser" in {
      val mockedFunction = mock[NeoParsersSpec]
      val goodJsonParser = new JsonFactory().createParser(new ByteArrayInputStream("{\"results\":[{\"columns\":[\"n\"],\"data\":[{\"row\":[{\"email\":\"brattorre@gmail.com\",\"username\":\"brunnoattorre\",\"fullName\":\"Brunno Attorre\",\"avatarURL\":\"https://avatars.githubusercontent.com/u/5482242?v=3\",\"loginInfo\":\"github:5482242\",\"karma\":0}]}]}],\"errors\":[]}".getBytes()));
      val parser = new NeoParsers()
      parser.parseJson(goodJsonParser, mockedFunction.callback)
      there was one(mockedFunction).callback(any[Any])
    }
    "ParseJsonWithMultipleUsers" in {
      val mockedFunction = mock[NeoParsersSpec]
      val goodJsonParserTwoUsers = new JsonFactory().createParser("{\"results\":[{\"columns\":[\"n\"],\"data\":[{\"row\":[{\"email\":\"brattorre@gmail.com\",\"username\":\"brunnoattorre\",\"fullName\":\"Brunno Attorre\",\"avatarURL\":\"https://avatars.githubusercontent.com/u/5482242?v=3\",\"loginInfo\":\"github:5482242\",\"karma\":0},{\"email\":\"brattorre@gmail.com\",\"username\":\"brunnoattorre\",\"fullName\":\"Brunno Attorre\",\"avatarURL\":\"https://avatars.githubusercontent.com/u/5482242?v=3\",\"loginInfo\":\"github:5482242\",\"karma\":0}]}]}],\"errors\":[]}");
      val parser = new NeoParsers()
      parser.parseJson(goodJsonParserTwoUsers, mockedFunction.callback)
      there was two(mockedFunction).callback(any[Any])

    }
    "ParseJsonWithNoUsers" in {
      val mockedFunctionInner = mock[NeoParsersSpec]
      val goodJsonParserNoUsers = new JsonFactory().createParser("{\"results\":[{\"columns\":[\"n\"],\"data\":[{\"row\":[]}]}],\"errors\":[]}");
      val parser = new NeoParsers()
      parser.parseJson(goodJsonParserNoUsers, mockedFunctionInner.callback)
      there was no(mockedFunctionInner).callback(any[Any])
    }
  }
}
