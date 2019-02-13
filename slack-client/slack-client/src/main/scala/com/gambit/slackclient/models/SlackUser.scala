package com.gambit.slackclient.models

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.typesafe.scalalogging.Logger
import slack.models.User
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
)

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
}

/** User Utilities
 *  Common utility functions associated with the SlackUsers slick model
 */
object UserUtils {
  /** Sync Users
   *  Get the list of users in the given user list that isn't already in th
   *  database and bulk insert their IDs
   *  @param usersTable the table to run the database query against
   *  @param users a list of slack users to add
   *  @return a future containing how many users were inserted if any
   */
  def syncUsers(usersTable: TableQuery[SlackUsers], users: Seq[User]): DBIO[Option[Int]] = {
    usersTable.map{_.slackId}.result.flatMap{ slackIds =>
      usersTable.map{(_.slackId)} ++= users.map{_.id}.filter{!slackIds.contains(_)}
    }
  }
}
