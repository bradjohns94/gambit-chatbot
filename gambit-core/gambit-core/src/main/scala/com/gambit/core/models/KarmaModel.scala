package com.gambit.core.models

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.sql.Timestamp

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

/** Karma
 *  Case class corresponding to the database schema of the karma table
 *  @param name the unique name the karma is associated with
 *  @param value the numerical value the karma is associated with
 *  @param linkedUser the user the karma is associated with if any
 *  @param createdAt a timestamp of when the row was created
 *  @param updatedAt a timestamp of when the row was last updated
 */
case class Karma(
  name: String,
  value: Option[Int],
  linkedUser: Option[Int],
  createdAt: Option[Timestamp],
  updatedAt: Option[Timestamp]
)

/** Karma Table
 *  Reference object for the karma table
 *  @param tag the tag to link with
 */
class KarmaTable(tag: Tag) extends Table[Karma](tag, "karma") {
  val timestampType = O.SqlType("timestamp default now()")

  // Unique name identifier for the row
  def name: Rep[String] = column[String]("name", O.PrimaryKey)
  // Value in fake internet points
  def value: Rep[Int] = column[Int]("value", O.Default(0))
  // Prepended text column
  def linkedUser: Rep[Option[Int]] = column[Option[Int]]("linked_user")
  // Creation timestamp column
  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at", timestampType)
  // Last update timestamp column
  def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at", timestampType)

  // Implicit conversion of case class
  def * = (name, value.?, linkedUser, createdAt.?, updatedAt.?) <>  // scalastyle:ignore
          (Karma.tupled, Karma.unapply)

  // Foreign key linkage to gambit_users table
  def gambitUser: ForeignKeyQuery[GambitUsers, GambitUser] = foreignKey(
    "karma_linked_user_fkey",
    linkedUser,
    TableQuery[GambitUsers]
  )(_.id.?)
}

/** Karma Reference
 *  Utility functions around accessing the karma table
 */
class KarmaReference(db: Database) {
  private val karmaTable = TableQuery[KarmaTable]
  private val aliasReference = new AliasReference(db)

  /** Get Karma Value
   *  Get the karma value (if any) from a potentially alias-mapped karma name,
   *  if the karma name is not in the database return 0.
   *  @param name the name of the karma key to lookup
   *  @return the value in the database or 0
   */
  def getKarmaValue(name: String): Future[Int] = db.run(
    aliasReference.getPrimaryNameAction(name).flatMap{ mappedName =>
      karmaTable.filter{_.name === mappedName}.map{_.value}.result.map{
        _.headOption.getOrElse(0)
      }
    }
  )
}
