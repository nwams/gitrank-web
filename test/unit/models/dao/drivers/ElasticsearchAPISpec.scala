package unit.models.dao.drivers

import models.daos.OAuth2InfoDAO
import models.daos.drivers.{ElasticsearchAPI, GitHubAPI}
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Context
import play.api.libs.json.Json
import play.api.libs.ws.{WSResponse, WSClient}
import play.api.test.WithApplication

@RunWith(classOf[JUnitRunner])
class ElasticsearchAPISpec extends Specification with Mockito {

  val jsonNoHits = "{\"took\":1,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":0,\"max_score\":null,\"hits\":[]}}"
  val jsonOneHit = "{\"took\":2,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":1,\"max_score\":4.042899,\"hits\":[{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"45281920\",\"_score\":4.042899,\"_source\":{\"repo\": {\"name\": \"ReSearchITEng/grandmother-hosts-ad-blocking\"}}}]}}"
  val jsonMultipleHits ="{\"took\":9,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":29329,\"max_score\":1.0,\"hits\":[{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"15126632\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"tkawajir/lge-kernel-gproj\"}}},{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"19886872\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"tcort/dijkstrajs\"}}},{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"45087905\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"audio-lab/waveform\"}}},{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"32232148\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"prathmeshpore/D3-Project\"}}},{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"45252696\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"larjen/WPRestApiExtensions\"}}},{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"45282754\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"trebmuh/qjackctl\"}}},{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"45255433\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"coIorZ/jSlider\"}}},{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"15624775\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"lontoken/lontoken.github.com\"}}},{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"45282423\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"peteward44/auto-usb-backup\"}}},{\"_index\":\"github\",\"_type\":\"repository\",\"_id\":\"43999840\",\"_score\":1.0,\"_source\":{\"repo\": {\"name\": \"anqif/cvxr\"}}}]}}"

  "elasticsearchAPI#parseResponse" should {
    "return empty seq if no repo present" in new WithApplication with Context{
      val ws = mock[WSClient]
      val elasticSearchAPI = new ElasticsearchAPI(ws)
      val response = mock[WSResponse]
      response.json returns Json.parse(jsonNoHits)
      val result = elasticSearchAPI.parseResponse(response)
      result must beEmpty
    }

    "return seq with one repo present" in new WithApplication with Context{
      val ws = mock[WSClient]
      val elasticSearchAPI = new ElasticsearchAPI(ws)
      val response = mock[WSResponse]
      response.json returns Json.parse(jsonOneHit)
      val result = elasticSearchAPI.parseResponse(response)
      result.size shouldEqual 1
    }


    "return seq with many repos present" in new WithApplication with Context{
      val ws = mock[WSClient]
      val elasticSearchAPI = new ElasticsearchAPI(ws)
      val response = mock[WSResponse]
      response.json returns Json.parse(jsonMultipleHits)
      val result = elasticSearchAPI.parseResponse(response)
      result.size shouldEqual 10
    }


  }

}
