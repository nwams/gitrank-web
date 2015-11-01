package models.services

import com.google.inject.Inject
import models.daos.drivers.ElasticsearchAPI

import scala.concurrent.Future

class ElasticSearchService @Inject()(elasticsearchAPI: ElasticsearchAPI){

  /**
   * Search for repos on elasticsearch
   * @param queryString search text
   * @return list of repo names
   */
  def searchForRepos(queryString: String): Future[Seq[String]]={
    elasticsearchAPI.searchForRepos(queryString)
  }

}
