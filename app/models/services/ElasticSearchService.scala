package models.services

import com.google.inject.Inject
import models.ElasticsearchResult
import models.daos.drivers.ElasticsearchAPI

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class ElasticSearchService @Inject()(elasticsearchAPI: ElasticsearchAPI){

  /**
   * Search for repos on elasticsearch
   * @param queryString search text
   * @return list of repo names
   */
  def searchForRepos(queryString: String): Future[Seq[ElasticsearchResult]]={
    elasticsearchAPI.searchForRepos(queryString).map(
      listResults => listResults.map(name => ElasticsearchResult(name, ""))
    )
  }

}
