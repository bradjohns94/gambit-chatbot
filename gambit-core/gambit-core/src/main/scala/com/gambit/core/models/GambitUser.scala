package com.gambit.core.models

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
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


/** Gambit User Reference
 *  utility functions around accessing the gambit users table
 */
class GambitUsersReference(db: Database) {
  private val userTable = TableQuery[GambitUsers]

  /** Create Gambit User Action
   *  Create a new user from a nickname in the gambit database without actually
   *  running the command
   *  @param nickname the nickname of the user to be created
   *  @return a database action resulting in the created user's nickname
   */
  def createGambitUserAction(nickname: String): DBIO[Int] =
    userTable.map{_.nickname} returning userTable.map{ _.id } += nickname

  /** Create Gambit User
   *  Create a new gambit user from the createGambitUserAction
   *  @param nickname the nickname of the user to be created
   *  @return unknown
   */
  def createGambitUser(nickname: String): Future[Try[Int]] =
    db.run(createGambitUserAction(nickname).asTry)

  /** Get ID from nickname
   *  Parse the human-readable nickname identifier into a machine readable ID
   *  field for bot processing
   *  @param nickname the nickname to resolve the ID for
   *  @return the user ID for the user with the given nickname
   */
  def getIdFromNickname(nickname: String): Future[Option[Int]] = db.run(
    userTable.filter{_.nickname === nickname}.map{_.id}.result.map{_.headOption}
  )

  /** Get Is Administrator
   *  Get whether or not a user with a given userId is an administrator
   *  @param userId the unique identifier of the gambit user
   *  @return whether the user is an administrator
   */
  def getIsAdmin(userId: Int): Future[Boolean] = db.run(
    userTable.filter{_.id === userId}.map{_.isAdmin}.result.map{
      _.headOption match {
        case Some(true) => true
        case _ => false
      }
    }
  )
}
