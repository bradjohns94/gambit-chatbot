package com.gambit.core.clients

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future._
import com.softwaremill.sttp.json4s._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/** Alias Response Object
 *  API response object corresponding to a row in the alias table.
 *  @param primaryName the unique source name of the alias
 *  @param aliasedName the name for the source name to be redirected to
 *  @param createdAt the timestamp of creation of the row if it exists.
 *  @param updatedAt the timestamp of the last update on the row if it exists.
 */
case class Alias(
  primaryName: String,
  aliasedName: String,
  createdAt: Option[String],
  updatedAt: Option[String]
)

/** Alias Client
 *  Client class to issue requests to the alias API.
 */
class AliasClient extends Client {
  val logger = Logger(LoggerFactory.getLogger(classOf[AliasClient]))
  val apiName = "Alias API"

  private val karmaApiUrl = sys.env("KARMA_API_URL")
  private val karmaApiPort = sys.env("KARMA_API_PORT")
  implicit val backend = AsyncHttpClientFutureBackend()
  implicit val serialization =  org.json4s.native.Serialization


  /** Get Primary Name
   *  Fetch the alias associated with the provided alias name
   *  @param aliasedName the name to look up a primary alias from
   *  @return an alias object if it can be unpacked
   */
  def getPrimaryName(aliasedName: String): Future[Option[Alias]] =
      sttp.get(uri"${karmaApiUrl}:${karmaApiPort}/v1/aliases/${aliasedName}")
          .response(asJson[Alias])
          .send()
          .map{ unpackResponse[Alias] _ }
}
