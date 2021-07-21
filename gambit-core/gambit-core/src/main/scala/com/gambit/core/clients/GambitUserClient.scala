package com.gambit.core.clients

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future._
import com.softwaremill.sttp.json4s._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/** Gambit User
 *  Case class describing the JSON response for Gambit Users
 *  @param id the unique identifier of the user
 *  @param nickname a string nickname to reference the user by
 *  @param isAdmin a boolean indicating whether or not the user is an admin
 *  @param prefix any text the bot should put before the user's name
 *  @param createdAt a timestamp of when the row was created
 *  @param updatedAt a timestamp of when the row was last updated
 */
case class GambitUser(
  userId: Int,
  nickname: String,
  isAdmin: Boolean,
  prefix: String,
  createdAt: Option[String],
  updatedAt: Option[String]
)

case class CreateGambitUserRequest(
  nickname: String
)

/** Gambit User Client
 *  Client to communicate with the Gambit Users API
 */
class GambitUserClient extends Client {
  val logger = Logger(LoggerFactory.getLogger(classOf[GambitUserClient]))

  val apiName = "Gambit User API"
  private val userApiUrl = sys.env("USER_API_URL")
  private val userApiPort = sys.env("USER_API_PORT")
  implicit val backend = AsyncHttpClientFutureBackend()
  implicit val serialization =  org.json4s.native.Serialization


  /** Get Gambit User
   *  Lookup a gambit user by their ID
   *  @param userId the ID of the gambit user to lookup
   *  @return a future gambit user object associated with the ID if it exists
   */
  def getGambitUser(userId: Int): Future[Option[GambitUser]] =
    sttp.get(uri"${userApiUrl}:${userApiPort}/v1/gambit-users/${userId}")
        .response(asJson[GambitUser])
        .send()
        .map{ unpackResponse[GambitUser] _ }

  /** Get Gambit User By Nickname
   *  Lookup a gambit user by their nickname
   *  @param nickname the nickname of the user to lookup
   *  @return a future gambit user object associated with the nickname if it exists
   */
  def getGambitUserByNickname(nickname: String): Future[Option[GambitUser]] =
    sttp.get(uri"${userApiUrl}:${userApiPort}/v1/gambit-users/nickname/${nickname}")
        .response(asJson[GambitUser])
        .send()
        .map{ unpackResponse[GambitUser] _ }

  /** Create Gambit User
   *  Create a new gambit user from the provided nickname
   *  @param nickname the nickname of the user to be created
   *  @return a future object which may contain the created gambit user
   */
  def createGambitUser(nickname: String): Future[Option[GambitUser]] =
    sttp.post(uri"${userApiUrl}:${userApiPort}/v1/gambit-users")
        .body(CreateGambitUserRequest(nickname))
        .response(asJson[GambitUser])
        .send()
        .map{ unpackResponse[GambitUser] _ }
}
