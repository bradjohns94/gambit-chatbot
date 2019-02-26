package com.gambit.core.models

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

import slick.jdbc.PostgresProfile.api._

import com.gambit.core.common.PermissionLevel

/** Client User
 *  Common trait to define any table-describing case classes for what is needed
 *  to be a client user
 */
trait ClientUser {
  val gambitUserId: Option[Int]
  val createdAt: Option[Timestamp]
  val updatedAt: Option[Timestamp]
}


/** Client Reference
 *  Trait for any class that holds utility functions to interact with a client
 *  model to help resolve messages into a single common format for command
 *  parsing.
 *  @param a database to connect to
 */
abstract trait ClientReference {
  val db: Database
  private val gambitUsers = new GambitUsersReference(db)

  /** Get User By ID
   *  Fetch the client user object by its respective client ID
   *  @param clientId the ID used to reference the user if they exist
   *  @return a distinct client user
   */
  def getUserById(clientId: String): Future[Option[ClientUser]]

  /** Set Gambit User From Nickname
   *  Wrapper function for setGambitUser to make it more consumable by free-text
   *  users, fetches the gambit ID from its unique nickname and passes it to
   *  the inhereting class's setGambitUser
   *  @param clientId the ID used to reference the user if they exist
   *  @param nickname the unique nickname for the gambit user to link to
   *  @return the gambit user the user was linked to if successful
   */
  def setGambitUserFromNickname(clientId: String, nickname: String): Future[Try[Int]] =
    gambitUsers.getIdFromNickname(nickname).flatMap{ _ match {
      case Some(userId) => setGambitUser(clientId, userId)
      case None => Future(throw new IllegalArgumentException("Failed to get ID from nickname"))
    }}

  /** Set gambit user
   *  Set the gambit user ID for the client to a given user in the gambit users table
   *  @param clientId the ID used to reference the user if they exist
   *  @param userId the unique identifier of the gambit user
   *  @return the gambit user the user was linked to if successful
   */
  def setGambitUser(clientId: String, userId: Int): Future[Try[Int]]

  /** Get Permission Level
   *  Given a reference to any Client User, use the associated gambit user ID
   *  to determine the permission level of the client user
   *  @param user a reference to a ClientUser
   *  @return the permission level of the client user
   */
   def getPermissionLevel(user: ClientUser): Future[PermissionLevel.Value] =
    user.gambitUserId match {
      case None => Future(PermissionLevel.Unregistered)
      case Some(userId) => gambitUsers.getIsAdmin(userId).map{ _ match {
        case true => PermissionLevel.Administrator
        case false => PermissionLevel.Registered
      }}
    }
}
