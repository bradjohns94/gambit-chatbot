package com.gambit.tools.admin.models

import java.sql.Timestamp

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
