package com.gambit.core.clients

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future._
import com.softwaremill.sttp.json4s._
import com.typesafe.scalalogging.Logger

/** Karma Response Object
 *  API response object corresponding to a row in the karma table.
 *  @param name the string identifier of the karma row
 *  @param value the integer value of the karma row
 *  @param linkedUser the user ID linked to the karma value if any
 *  @param createdAt the timestamp of creation of the row if it exists.
 *  @param updatedAt the timestamp of the last update on the row if it exists.
 */
case class Karma(
  name: String,
  value: Int,
  linkedUser: Option[Int],
  createdAt: Option[String],
  updatedAt: Option[String]
)

/** Karma Client
 *  Client class to issue requests to the karma API.
 */
class KarmaClient extends Client {
  val logger = Logger("KarmaClient")

  val apiName = "Karma API"
  private val karmaApiUrl = sys.env("KARMA_API_URL")
  private val karmaApiPort = sys.env("KARMA_API_PORT")
  implicit val backend = AsyncHttpClientFutureBackend()
  implicit val serialization =  org.json4s.native.Serialization


  /** Get Karma
   *  Get the karma object associated with the given karma name from the karma API
   *  @param name the name to get karma for
   *  @return a future karma object associated with the name if it exists
   */
  def getKarma(name: String): Future[Option[Karma]] =
    sttp.get(uri"${karmaApiUrl}:${karmaApiPort}/v1/karma/${name}")
        .response(asJson[Karma])
        .send()
        .map{ unpackResponse[Karma] _ }

  /** Get Karma For User
   *  Fetch the karma object for a vgiven user ID
   *  @param userId the ID of the user to fetch karma for
   *  @return a future karma object associated with the user if it exists
   */
  def getKarmaForUser(userId: Int): Future[Seq[Karma]] =
    sttp.get(uri"${karmaApiUrl}:${karmaApiPort}/v1/karma/user/${userId}")
        .response(asJson[Seq[Karma]])
        .send()
        .map{ unpackResponse[Seq[Karma]](_).getOrElse(Seq.empty[Karma]) }

  /** Update Karma
   *  Send a karma update to the karma API and return the list of updated karma objects
   *  @param updates a mapping of name -> increments
   *  @return the list of updated karma objects
   */
  def updateKarma(updates: Map[String, Int]): Future[Seq[Karma]] =
    sttp.post(uri"${karmaApiUrl}:${karmaApiPort}/v1/karma")
        .body(updates)
        .response(asJson[Seq[Karma]])
        .send()
        .map{ unpackResponse[Seq[Karma]](_).getOrElse(Seq.empty[Karma]) }


}
