package com.gambit.core.clients

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future._
import com.softwaremill.sttp.json4s._
import com.typesafe.scalalogging.Logger

/** Slack User
 *  Case class describing the JSON response for Slack Users
 *  @param slackId the unique ID provided by slack for the user
 *  @param gambitUserId the foreign key ID referencing the gambit users table
 *  @param createdAt a timestamp of when the row was created
 *  @param updatedAt a timestamp of when the row was last updated
 */
case class SlackUser (
  slackId: String,
  gambitUserId: Option[Int],
  createdAt: Option[String],
  updatedAt: Option[String]
)

/** Slack User Body
 *  Case class describing POST/PATCH inputs for slack users
 *  @param slackId the unique ID provided by slack for the user
 *  @param gambitUserId the foreign key ID referencing the gambit users table
 */
case class SlackUserBody(
  slackId: String,
  gambitUserId: Option[Int]
)

/** Slack User Client
 *  Client to communicate with the Slack Users API
 */
class SlackUserClient extends UserClient {
  val logger = Logger("SlackUserClient")

  val apiName = "Slack User API"
  private val userApiUrl = sys.env("USER_API_URL")
  private val userApiPort = sys.env("USER_API_PORT")
  implicit val backend = AsyncHttpClientFutureBackend()
  implicit val serialization =  org.json4s.native.Serialization

  /** To User
  *  Implicitly convert API responses to the common user type for shared operations
  *  XXX is there a way to do this with generics without renaming the API responses?
  *  @param slackUser a slack user to be converted to a generic user
  *  @return a User object constructed from the slack user
  */
  private def toUser(slackUser: SlackUser): User =
    User(slackUser.slackId, slackUser.gambitUserId)

  /** Get Slack User
   *  Fetch a user from the database with the given slack ID
   *  @param slackId the id of the slack user to lookup
   *  @return a future reference to the user if it is found in the database
   */
  def getSlackUser(slackId: String): Future[Option[SlackUser]] =
    sttp.get(uri"${userApiUrl}:${userApiPort}/v1/slack-users/${slackId}")
        .response(asJson[SlackUser])
        .send()
        .map{ unpackResponse[SlackUser] _ }

  /** Get Gambit User ID
   *  Resolve a slack ID into a gambit user ID if possible
   *  @param slackId the slack ID of the user in question
   *  @return a gambit user eventually if one can be resolved for the slack user
   */
  def getGambitUserId(slackId: String): Future[Option[Int]] =
    getSlackUser(slackId).map{ maybeUser => maybeUser.flatMap{ _.gambitUserId } }

  /** Get Unlinked Slack Users
   *  Get a list of all slack users in the database that do not have an associated gambit user
   *  @return a future list of slack users with no gambit user linked to them
   */
  def getUnlinkedUsers: Future[Seq[User]] =
    sttp.get(uri"${userApiUrl}:${userApiPort}/v1/slack-users/unlinked")
        .response(asJson[Seq[SlackUser]])
        .send()
        .map{ unpackResponse[Seq[SlackUser]](_).getOrElse(Seq.empty[SlackUser]) }
        .map{ slackUsers => slackUsers.map{ toUser _ } }

  /** Set Gambit User
   *  Update a slack user to associate it with a given gambit user ID
   *  @param clientId the slack ID of the user to update
   *  @param userId the gambit user ID to associate the slack user with
   *  @return a future reference to the updated user
   */
  def setGambitUser(clientId: String, userId: Int): Future[Option[User]] =
    sttp.patch(uri"${userApiUrl}:${userApiPort}/v1/slack-users")
        .body(SlackUserBody(clientId, Some(userId)))
        .response(asJson[SlackUser])
        .send()
        .map{ unpackResponse[SlackUser] _ }
        .map{ maybeUser => maybeUser.map{ toUser _ } }

  /** Create Slack User
   *  Create a new slack user with the associated gambit user ID
   *  @param slackId the slack ID of the user to update
   *  @param userId the gambit user ID to associate the slack user with
   *  @return a future reference to the created user
   */
  def createSlackUser(slackId: String, userId: Int): Future[Option[SlackUser]] =
    sttp.post(uri"${userApiUrl}:${userApiPort}/v1/slack-users")
        .body(SlackUserBody(slackId, Some(userId)))
        .response(asJson[SlackUser])
        .send()
        .map{ unpackResponse[SlackUser] _ }

  /** Create Slack User
   *  Create a new slack user with no associated gambit user ID
   *  @param slackId the slack ID of the user to update
   *  @return a future reference to the created user
   */
  def createSlackUser(slackId: String): Future[Option[SlackUser]] =
    sttp.post(uri"${userApiUrl}:${userApiPort}/v1/slack-users")
        .body(SlackUserBody(slackId, None))
        .response(asJson[SlackUser])
        .send()
        .map{ unpackResponse[SlackUser] _ }
}
