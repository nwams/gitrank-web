package unit.models.utils

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Context
import play.api.test.WithApplication
import utils.ElasticQueryParser

@RunWith(classOf[JUnitRunner])
class ElasticQueryParserSpec extends Specification with Mockito {

  "ElasticQueryParser#escapeCharsForQuery" should {
    "escape invalid characters" in new WithApplication with Context{
      val escapedString = ElasticQueryParser.escapeCharsForQuery("abc/def-")
      println(escapedString)
      escapedString must be equalTo("abc(\\/)def(\\-)")
    }

    "keep same string if chars are valid" in new WithApplication with Context{
      val escapedString = ElasticQueryParser.escapeCharsForQuery("abcdef")
      escapedString must be equalTo("abcdef")
    }

    "if string is empty, don't do nothing" in new WithApplication with Context{
      val escapedString = ElasticQueryParser.escapeCharsForQuery("")
      escapedString must be equalTo("")
    }
  }
}
