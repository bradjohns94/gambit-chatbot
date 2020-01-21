package com.gambit.user.models

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import slick.dbio.SuccessAction
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

/** Slack User
 *  Case class corresponding to the database schema of the slack_users table.
 *  @param clientId the unique identifier of the user ganerated by slack
 *  @param gambitUserId the unique identifier of the user in gambit_users
 *  @param createdAt a timestamp of when the row was created
 *  @param updatedAt a timestamp of when the row was last updated
 */
case class SlackUser(
  clientId: String,
  gambitUserId: Option[Int],
  createdAt: Option[Timestamp],
  updatedAt: Option[Timestamp]
) extends ClientUser

/** Slack Users
 *  Reference object for the Slack Users table
 *  @param tag the tag to link with
 */
class SlackUsers(tag: Tag) extends Table[SlackUser](tag, "slack_users") {
  val timestampType = O.SqlType("timestamp default now()")

  // Unique slack identifier
  def clientId: Rep[String] = column[String]("slack_id", O.PrimaryKey)
  // Unique gambit_users identifier
  def gambitUserId: Rep[Option[Int]] = column[Option[Int]]("gambit_user_id")
  // Creation timestamp column
  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at", timestampType)
  // Last update timestamp column
  def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at", timestampType)

  // Implicit conversion of case class
  def * = (clientId, gambitUserId, createdAt.?, updatedAt.?) <>  // scalastyle:ignore
          (SlackUser.tupled, SlackUser.unapply)

  // Foreign key linkage to gambit_users table
  def gambitUser: ForeignKeyQuery[GambitUsers, GambitUser] = foreignKey(
    "slack_users_gambit_user_id_fkey",
    gambitUserId,
    TableQuery[GambitUsers]
  )(_.id.?, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
}

/** Slack user Reference
 *  Accessor functions to gather information from the slack users table
 *  @param db the database to connect to
 */
class SlackUserReference(override val db: Database) extends ClientReference[SlackUser] {
  private val slackUsers = TableQuery[SlackUsers]

  /** Get User By ID
   *  Fetch the client user object by its respective client ID
   *  @param clientId the ID used to reference the user if they exist
   *  @return a distinct client user
   */
  def getUserByIdAction(clientId: String): DBIO[Option[SlackUser]] =
    slackUsers.filter{_.clientId === clientId}.result.map{ _.headOption }

  /** Get All Unlinked Usernames
   *  Get the unique ID of all users who aren't linked to a gambit user
   *  @return A list of users who aren't currently linked to gambit users
   */
  def getAllUnlinkedUsersAction: DBIO[Seq[SlackUser]] =
    slackUsers.filter{_.gambitUserId.isEmpty}.result

  /** Create user Action
   * Create a new user from the provided slack user
   * @param user the slack user to create a user from
   * @return the created user
   */
  def createUserAction(user: SlackUser): DBIO[SlackUser] =
    slackUsers returning slackUsers += user

  /** Update User Action
   *  Update an existing slack user to match the provided parameters
   *  @param upsdatedUser the user to update with the given parameters
   *  @return the updated user
   */
  def updateUserAction(updatedUser: SlackUser): DBIO[Option[SlackUser]] = {
    val subquery = for { user <- slackUsers if user.clientId === updatedUser.clientId } yield user
    subquery.update(updatedUser).flatMap{
      case 0 => new SuccessAction(None)
      case _ => getUserByIdAction(updatedUser.clientId)
    }
  }

  /** Set Gambit User
   *  Set the gambit user ID for the client to a given user in the gambit users table
   *  @param clientId the ID used to reference the user if they exist
   *  @param userId the unique identifier of the gambit user
   *  @return the gambit user ID the user was linked to if successful
   */
  def setGambitUserAction(clientId: String, userId: Int): DBIO[Int] = {
    val subQuery = for { user <- slackUsers if user.clientId === clientId} yield user.gambitUserId
    subQuery.update(Some(userId))
  }
}
