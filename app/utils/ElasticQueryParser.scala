package utils

import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil

/**
 * Created by brunnoattorre1 on 12/8/15.
 */
object ElasticQueryParser {

  /**
   * Github uses "-" and "/" on their repo names, which is forbidden on lucene query syntax.
   * We've created this custom query parser as the lucene one simply escaped the characters, which did not work on our case.
   * This method is responsible for escaping and grouping the query
    *
   * @param query query to be escaped
   * @return query string escaped.
   */
  def  escapeCharsForQuery(query: String): String = {
    QueryParserUtil.escape(query).replaceAll("\\\\(.)","(\\\\'$1')").replaceAll("'","")
  }
}
