package com.gambit.user.api.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

import cats.effect.IO
import com.typesafe.scalalogging.Logger
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.finch._
import io.finch.catsEffect._
import io.finch.circe._
import shapeless.Coproduct

import com.gambit.user.api.GambitEndpoint
import com.gambit.user.models.{SlackUser, SlackUserReference}


/** Slack User Response
 *  Case class describing the JSON response for Slack Users
 *  @param slackId the unique ID provided by slack for the user
 *  @param gambitUserId the foreign key ID referencing the gambit users table
 *  @param createdAt a timestamp of when the row was created
 *  @param updatedAt a timestamp of when the row was last updated
 */
case class SlackUserResponse(
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

/** Slack User API
 *  Class containing reference functions to the slack users database table
 *  @param table a reference object to the slack users table
 */
class SlackUserApi(table: SlackUserReference) extends GambitEndpoint {
  val logger = Logger("SlackUserApi")

  val basePath = path("v1") :: path("slack-users")

  implicit val decoder: Decoder[SlackUserResponse] = deriveDecoder[SlackUserResponse]
  implicit val encoder: Encoder[SlackUserResponse] = deriveEncoder[SlackUserResponse]
  implicit val postDecoder: Decoder[SlackUserBody] = deriveDecoder[SlackUserBody]
  implicit val postEncoder: Encoder[SlackUserBody] = deriveEncoder[SlackUserBody]

  /** Get Slack User
   *  Endpoint to get a slack user from the database by its ID
   *  @param id the slack-specific user ID string
   *  @return an endpoint resulting in the slack user object if they're found or NotFound
   */
  def getSlackUser: Endpoint[IO, SlackUserResponse] =
    get(basePath :: path[String]) { getSlackUserAction _ }

  /** Get Unlinked Slack Users
   *  Endpoint to get all slack user objects for slack users with no associated gambit user
   *  @return an endpoint resulting in a list of slack user objects
   */
  def getUnlinkedSlackUsers: Endpoint[IO, Seq[SlackUserResponse]] =
    get(basePath :: path("unlinked")) { getUnlinkedSlackUserAction }

  /** Update Slack User
   *  Endpoint to update/replace an existing slack user with a provided set of parameters
   *  @param user a SlackUserBody object to update the user with
   *  @return the endpoint returning the updated user if it can be updated
   */
  def updateSlackUser: Endpoint[IO, SlackUserResponse] =
    patch(basePath :: jsonBody[SlackUserBody]) { updateSlackUserAction _ }

  /** Create Slack User
   *  Endpoint to create a new slack user entry in the database
   *  @param user a SlackUserBody object to create a new user from
   *  @return an endpoint returning the created endpoint if it can be created
   */
  def createSlackUser: Endpoint[IO, SlackUserResponse] =
    post(basePath :: jsonBody[SlackUserBody]) { createSlackUserAction _ }

  /** Get Slack User Action
   *  Helper function to return the slack user matching the provided ID
   *  @param clientId the slack-specific user ID string
   *  @return a future API output containing the slack user if one can be found
   */
  private def getSlackUserAction(clientId: String): Future[Output[SlackUserResponse]] =
    table.getUserById(clientId).map{ maybeUser =>
      maybeUser match {
        case Some(user) => Ok(translateSlackUser(user))
        case None => NotFound(new Exception(s"Failed to find slack user with ID ${clientId}"))
      }
    }

  /** Get Unlinked Slack Users Action
   *  Helper function to return the list of slack users without linked gambit users
   *  @return a future API output containing a list of slack users
   */
  private def getUnlinkedSlackUserAction: Future[Output[Seq[SlackUserResponse]]] =
    table.getAllUnlinkedUsers.map{ users => Ok(users.map{ translateSlackUser _ }) }

  /** Update Slack User Action
   *  Helper function to update a slack user using the provided SlackUserBody
   *  @param user a SlackUserBody object to update the user with
   *  @return a future API output containing the user if it can be updated
   */
  private def updateSlackUserAction(body: SlackUserBody): Future[Output[SlackUserResponse]] = {
    table.updateUser(tranlsateSlackUserBody(body)).map{ maybeUser =>
      maybeUser match {
        case Some(user) => Ok(translateSlackUser(user))
        case None => {
          logger.warn(s"Failed to update slack user with ID ${body.slackId}")
          BadRequest(new Exception(s"Failed to update slack user with ID ${body.slackId}"))
        }
      }
    }
  }

  /** Create Slack User Action
   *  Helper function to create a new slack user in the database
   *  @param user a SlackUserBody object to create a new user from
   *  @return a future API output containing the user if it was successfully created
   */
  private def createSlackUserAction(body: SlackUserBody): Future[Output[SlackUserResponse]] =
    table.createUser(tranlsateSlackUserBody(body)).map{ maybeUser =>
      maybeUser match {
        case Success(user) => Ok(translateSlackUser(user))
        case Failure(error) => {
          logger.warn(s"Failed to create slack user ${error}")
          BadRequest(new Exception(s"Failed to create slack user ${error}"))
        }
      }
    }

  /** Translate Slack User
   *  Convert a Slack User from the database into a SlackUserResponse object returned from the API
   *  @param row a SlackUser object returned from the database
   *  @return a SlackUserResponse constructed from the database object
   */
  private def translateSlackUser(row: SlackUser): SlackUserResponse = new SlackUserResponse(
    row.clientId,
    row.gambitUserId,
    row.createdAt.map{_.toString},
    row.updatedAt.map{_.toString}
  )

  /** Translate Slack User Body
   *  Convert a SlackUserBody object into a SlackUser object to be created/updated in the db
   *  @param body a SlackUserBody API input object
   *  @return a SlackUser object ready to be consumed by the Slick DAL
   */
  private def tranlsateSlackUserBody(body: SlackUserBody): SlackUser = SlackUser(
    body.slackId,
    body.gambitUserId,
    None,
    None
  )

  val endpoints = (
    getUnlinkedSlackUsers :+:
    getSlackUser :+:
    updateSlackUser :+:
    createSlackUser
  )
}
