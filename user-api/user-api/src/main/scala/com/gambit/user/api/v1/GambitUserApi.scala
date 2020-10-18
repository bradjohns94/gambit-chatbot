package com.gambit.user.api.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.effect.IO
import com.typesafe.scalalogging.Logger
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.finch._
import io.finch.catsEffect._
import io.finch.circe._
import shapeless.Coproduct

import com.gambit.user.api.GambitEndpoint
import com.gambit.user.models.{GambitUser, GambitUserReference}

/** Gambit User Response
 *  Case class describing the JSON response for Gambit Users
 *  @param id the unique identifier of the user
 *  @param nickname a string nickname to reference the user by
 *  @param isAdmin a boolean indicating whether or not the user is an admin
 *  @param prefix any text the bot should put before the user's name
 *  @param createdAt a timestamp of when the row was created
 *  @param updatedAt a timestamp of when the row was last updated
 */
case class GambitUserResponse(
  userId: Int,
  nickname: String,
  isAdmin: Boolean,
  prefix: String,
  createdAt: Option[String],
  updatedAt: Option[String]
)

/** Gambit User Body
 *  POST request object to create a gambit user from
 *  @param nickname a string nickname to reference the user by
 */
case class GambitUserBody(
  nickname: String
)

/** Gambit User API
 *  Class containing reference functions to the gambit users database table.
 *  @param table a reference object to the gambit users table
 */
class GambitUserApi(table: GambitUserReference) extends GambitEndpoint {
  val logger = Logger("GambitUserApi")

  val basePath = path("v1") :: path("gambit-users")

  implicit val decoder: Decoder[GambitUserResponse] = deriveDecoder[GambitUserResponse]
  implicit val encoder: Encoder[GambitUserResponse] = deriveEncoder[GambitUserResponse]
  implicit val postDecoder: Decoder[GambitUserBody] = deriveDecoder[GambitUserBody]
  implicit val postEncoder: Encoder[GambitUserBody] = deriveEncoder[GambitUserBody]

  /** Get Gambit User
   *  Endpoint to get a gambit user from the database by its ID
   *  @param id the ID associated with the user
   *  @return an endpoint resulting in the user if they're found or a NotFound otherwise
   */
  def getGambitUser: Endpoint[IO, GambitUserResponse] =
    get(basePath :: path[Int]) { getGambitUserAction _ }

  /** Get Gambit User By Nickname
   *  Endpoint to fetch the gambit user associated with a provided unique nickname
   *  @param nickname the nickname associated with the user
   *  @return an endpoint resulting in the user if they're found or a NotFound otherwise
   */
  def getGambitUserByNickname: Endpoint[IO, GambitUserResponse] =
    get(basePath :: path("nickname") :: path[String]) { getGambitUserByNicknameAction _ }

  /** Create Gambit User
   *  Endpoint to take a gambit user body and create a new gambit user from it
   *  @param body the GambitUserBody being used to create the new user
   *  @return an endpoint resulting in the newly created user if it can be created
   */
  // TODO this will 500 if the user exists
  def createGambitUser: Endpoint[IO, GambitUserResponse] =
    post(basePath :: jsonBody[GambitUserBody]) { createGambitUserAction _ }

  /** Get Gambit User Action
   *  Helper function used by get gambit user to perform the main database operation
   *  @param id the ID to look up for the gambit user
   *  @return a future output with the user if they're found
   */
  private def getGambitUserAction(id: Int): Future[Output[GambitUserResponse]] =
    table.getUserById(id).map{ maybeUser =>
      maybeUser match {
        case Some(user) => Ok(translateGambitUser(user))
        case None => NotFound(new Exception(s"Failed to find gambit user with ID ${id}"))
      }
    }

  /** Get Gambit User By Nickname Action
   *  Helper function used by Get Gambit User By Nickname to perform the main operation
   *  @param name the nickname associated with the user
   *  @return a future output with the user if they're found or a NotFound otherwise
   */
  private def getGambitUserByNicknameAction(name: String): Future[Output[GambitUserResponse]] =
    table.getUserByNickname(name).map{ maybeUser =>
      maybeUser match {
        case Some(user) => Ok(translateGambitUser(user))
        case None => NotFound(new Exception(s"Failed to find gambit user with nickname ${name}"))
      }
    }

  /** Create Gambit User Action
   *  Helper function used by Create Gambit User to perform the main database operation
   *  @param body a GambitUserBody object to create the user with
   *  @return a future output with the created gambit user
   */
  private def createGambitUserAction(body: GambitUserBody): Future[Output[GambitUserResponse]] =
    table.createGambitUser(body.nickname).map{ user => Ok(translateGambitUser(user)) }

  /** Update Gambit User Action
   *  Helper function used by Update Gambit user to change various paramters about a gambit user
   *  @param userId the gambit User ID to update
   *  @param body a GambitUserBody object to update the user with
   *  @return a future output with the updated user
   */
  private def updateGambitUserAction(
    userId: String,
    body: GambitUserBody
  ): Future[Output[GambitUserResponse]] = {

  }

  /** Translate Gambit User
   *  Conversion function that creates a GambitUserResponse object from the database gambit user
   *  @param row the GambitUser object returned from the database
   *  @return a GambitUserResponse created from the database row.
   */
  private def translateGambitUser(row: GambitUser): GambitUserResponse = new GambitUserResponse(
    row.id.getOrElse(-1),  // This field is required in the database so this won't happen
    row.nickname,
    row.isAdmin.getOrElse(false),
    row.prefix.getOrElse(""),
    row.createdAt.map{_.toString},
    row.updatedAt.map{_.toString}
  )

  val endpoints = (
    getGambitUser :+:
    getGambitUserByNickname :+:
    createGambitUser
  )
}
