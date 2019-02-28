package com.gambit.core.models

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

/** Slack User
 *  Case class corresponding to the database schema of the slack_users table.
 *  @param slack_id the unique identifier of the user ganerated by slack
 *  @param gambitUserId the unique identifier of the user in gambit_users
 *  @param createdAt a timestamp of when the row was created
 *  @param updatedAt a timestamp of when the row was last updated
 */
case class SlackUser(
  slackId: String,
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
  def slackId: Rep[String] = column[String]("slack_id", O.PrimaryKey)
  // Unique gambit_users identifier
  def gambitUserId: Rep[Option[Int]] = column[Option[Int]]("gambit_user_id")
  // Creation timestamp column
  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at", timestampType)
  // Last update timestamp column
  def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at", timestampType)

  // Implicit conversion of case class
  def * = (slackId, gambitUserId, createdAt.?, updatedAt.?) <>  // scalastyle:ignore
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
class SlackUsersReference(override val db: Database) extends ClientReference {
  private val slackUsers = TableQuery[SlackUsers]

  /** Get User By ID
   *  Fetch the client user object by its respective client ID
   *  @param clientId the ID used to reference the user if they exist
   *  @return a distinct client user
   */
  def getUserById(clientId: String): Future[Option[ClientUser]] = db.run(
    slackUsers.filter{_.slackId === clientId}.result.map{ _.headOption }
  )

  /** Get All Unlinked Usernames
   *  Get the unique ID of all users who aren't linked to a gambit user
   *  @return A list of users who aren't currently linked to gambit users
   */
  def getAllUnlinkedIdsAction: DBIO[Seq[String]] =
    slackUsers.filter{_.gambitUserId.isEmpty}.map{_.slackId}.result

  /** Set Gambit User
   *  Set the gambit user ID for the client to a given user in the gambit users table
   *  @param clientId the ID used to reference the user if they exist
   *  @param userId the unique identifier of the gambit user
   *  @return the gambit user the user was linked to if successful
   */
  def setGambitUserAction(clientId: String, userId: Int): DBIO[Int] = {
    val subQuery = for { user <- slackUsers if user.slackId === clientId} yield user.gambitUserId
    subQuery.update(Some(userId))
  }
}
