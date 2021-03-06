package com.gambit.tools.admin.models

import java.sql.Timestamp

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

/** Gambit User
 *  Case class corresponding to the database schema of the gambit_users table.
 *  @param id the unique identifier of the user
 *  @param nickname a string nickname to reference the user by
 *  @param isAdmin a boolean indicating whether or not the user is an admin
 *  @param prefix any text the bot should put before the user's name
 *  @param createdAt a timestamp of when the row was created
 *  @param updatedAt a timestamp of when the row was last updated
 */
case class GambitUser(
  id: Option[Int],
  nickname: String,
  isAdmin: Option[Boolean],
  prefix: Option[String],
  createdAt: Option[Timestamp],
  updatedAt: Option[Timestamp]
)

/** Gambit Users
 *  Reference object for the Gambit Users table
 *  @param tag the tag to link with
 */
class GambitUsers(tag: Tag) extends Table[GambitUser](tag, "gambit_users") {
  val timestampType = O.SqlType("timestamp default now()")

  // Unique identifier column
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  // Nickname identifier column
  def nickname: Rep[String] = column[String]("nickname")
  // Admin permissions column
  def isAdmin: Rep[Boolean] = column[Boolean]("is_admin", O.Default(false))
  // Prepended text column
  def prefix: Rep[Option[String]] = column[Option[String]]("prefix")
  // Creation timestamp column
  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at", timestampType)
  // Last update timestamp column
  def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at", timestampType)

  // Implicit conversion of case class
  def * = (id.?, nickname, isAdmin.?, prefix, createdAt.?, updatedAt.?) <>  // scalastyle:ignore
          (GambitUser.tupled, GambitUser.unapply)
}
