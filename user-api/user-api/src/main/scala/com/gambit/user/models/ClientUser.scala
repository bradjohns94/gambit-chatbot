package com.gambit.user.models

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import slick.dbio.SuccessAction
import slick.jdbc.PostgresProfile.api._

/** Client User
 *  Common trait to define any table-describing case classes for what is needed
 *  to be a client user
 */
trait ClientUser {
  val clientId: String
  val gambitUserId: Option[Int]
  val createdAt: Option[Timestamp]
  val updatedAt: Option[Timestamp]
}


/** Client Reference
 *  Trait for any class that holds utility functions to interact with a client
 *  model to help resolve messages into a single common format for command
 *  parsing.
 */
abstract trait ClientReference[T <: ClientUser] {
  val db: Database
  private val gambitUsers = new GambitUserReference(db)

  /** Get User By ID
   *  DB running wrapper around getUserByIdAction
   *  @param clientId the ID used to reference the user if they exist
   *  @return a distinct client user
   */
  def getUserById(clientId: String): Future[Option[T]] = db.run(
    getUserByIdAction(clientId)
  )

  /** Get User By ID Action
   *  Fetch the client user object by its respective client ID
   *  @param clientId the ID used to reference the user if they exist
   *  @return a distinct client user
   */
  def getUserByIdAction(clientId: String): DBIO[Option[T]]

  /** Get Gambit User By ID
   *  DB running wrapper around getGambitUserByIdAction
   *  @param clientId the ID of the client user
   *  @return the associated gambit user if one is found
   */
  def getGambitUserById(clientId: String): Future[Option[GambitUser]] = db.run(
    getGambitUserByIdAction(clientId)
  )

  /** Get Gambit User By ID Action
   *  Get the gambit user associated with the client ID if any exists
   *  @param clientId the ID of the client user
   *  @return the associated gambit user if one is found
   */
  def getGambitUserByIdAction(clientId: String): DBIO[Option[GambitUser]] =
    getUserByIdAction(clientId).flatMap{ maybeUser =>
      maybeUser match {
        case Some(clientUser) => clientUser.gambitUserId match {
          case Some(userId) => gambitUsers.getUserByIdAction(userId)
          case None => new SuccessAction(None)
        }
        case None => new SuccessAction(None)
      }
    }

  /** Get All Unlinked Usernames
   *  Get the unique ID of all users who aren't linked to a gambit user
   *  @return the result of DBIO operation which returns all unlinked usernames
   */
  def getAllUnlinkedUsers: Future[Seq[T]] = db.run(getAllUnlinkedUsersAction)

  /** Get All Unlinked Usernames
   *  Get the unique ID of all users who aren't linked to a gambit user
   *  @return a DBIO operation which returns all unlinked usernames
   */
  def getAllUnlinkedUsersAction: DBIO[Seq[T]]

  /** Update User
   *  Update the user with the given payload to match the provided payload
   *  @param user the updated user body
   *  @return the updated user body
   */
  def updateUser(user: T): Future[Option[T]] = db.run(updateUserAction(user))

  /** Update User Action
   *  Abstract helper function containing logic on how to update the user
   *  @param user the user body to be updated in the database
   *  @return the updated user body as a database action
   */
  def updateUserAction(user: T): DBIO[Option[T]]

  /** Create user
   *  Create a new user and return the future instance of that user
   *  @param user the user to be created
   *  @return the created user
   */
  def createUser(user: T): Future[Try[T]] = db.run(createUserAction(user).asTry)

  /** Create user action
   *  Abstract database action to create a user in the database
   *  @param user the user to create
   *  @return the created user
   */
  def createUserAction(user: T): DBIO[T]

  /** Register Unlinked Users
   *  Create and link all unlinked users as gambit internal users
   *  @return the number of users created if it succeeded
   */
  def registerUnlinkedUsers: Future[Int] = db.run(
    getAllUnlinkedUsersAction.flatMap{ users =>
      DBIO.fold(users.map{ user => registerUser(user.clientId) }, 0) { _ + _ }
    }
  )

  /** Register User
   *  Create a new gambit user with the given clientId
   *  @param clientId the unique client identifier to register
   *  @return a DBIO action indicating how many users were created (0 or 1)
   */
  private def registerUser(clientId: String): DBIO[Int] = {
    gambitUsers.createGambitUserAction(clientId).flatMap{ createdUser =>
      createdUser.id match {
        case Some(userId) => setGambitUserAction(clientId, userId)
        case None => throw new Exception(f"Failed to create gambit user for client ${clientId}")
      }
    }
  }

  /** Set Gambit User From Nickname
   *  Wrapper function for setGambitUser to make it more consumable by free-text
   *  users, fetches the gambit ID from its unique nickname and passes it to
   *  the inhereting class's setGambitUser
   *  @param clientId the ID used to reference the user if they exist
   *  @param nickname the unique nickname for the gambit user to link to
   *  @return the gambit user the user was linked to if successful
   */
  def setGambitUserFromNickname(clientId: String, nickname: String): Future[Try[Int]] =
    gambitUsers.getUserByNickname(nickname).flatMap{ user =>
      user.flatMap{_.id} match { // Convert Option[User] -> Option[UserId]
        case Some(userId) => setGambitUser(clientId, userId)
        case None => Future(throw new IllegalArgumentException("Failed to get ID from nickname"))
      }
    }

  /** Set Gambit User Action
   *  Set the gambit user ID for the client to a given user in the gambit users table
   *  @param clientId the ID used to reference the user if they exist
   *  @param userId the unique identifier of the gambit user
   *  @return the gambit user the user was linked to if successful
   */
  def setGambitUserAction(clientId: String, userId: Int): DBIO[Int]

  /** Set Gambit User
   *  Execution wrapper around setGambitUserAction
   *  @param clientId the ID used to reference the user if they exist
   *  @param userId the unique identifier of the gambit user
   *  @return the gambit user the user was linked to if successful
   */
  def setGambitUser(clientId: String, userId: Int): Future[Try[Int]] =
    db.run(setGambitUserAction(clientId, userId).asTry)
}
