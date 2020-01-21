package com.gambit.karma.models

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.sql.Timestamp

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

/** Alias
 *  Case class corresponding to the database schema of the aliases table
 *  @param primaryName the name of the existing karma value
 *  @param aliasedName the unique name the karma value is aliased with
 *  @param createdAt a timestamp of when the row was created
 *  @param updatedAt a timestamp of when the row was last updated
 */
case class Alias(
  primaryName: String,
  aliasedName: String,
  createdAt: Option[Timestamp],
  updatedAt: Option[Timestamp]
)

/** Aliases
 *  Reference object for the aliases table
 *  @param tag the tag to link with
 */
class Aliases(tag: Tag) extends Table[Alias](tag, "aliases") {
  val timestampType = O.SqlType("timestamp default now()")

  // Reference column to the karma table (name)
  def primaryName: Rep[String] = column[String]("primary_name")
  // Unique identifier of the alias itself
  def aliasedName: Rep[String] = column[String]("aliased_name", O.PrimaryKey)
  // Creation timestamp column
  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at", timestampType)
  // Last update timestamp column
  def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at", timestampType)

  // Implicit conversion of case class
  def * = (primaryName, aliasedName, createdAt.?, updatedAt.?) <>  // scalastyle:ignore
          (Alias.tupled, Alias.unapply)

  // Foreign key linkage to gambit_users table
  def karma: ForeignKeyQuery[KarmaTable, Karma] = foreignKey(
    "aliases_primary_name_fkey",
    primaryName,
    TableQuery[KarmaTable]
  )(_.name)
}

/** Karma Reference
 *  Utility functions around accessing the karma table
 */
class AliasReference(db: Database) {
  private val aliasTable = TableQuery[Aliases]

  /** Get Primary Name
   *  DB-executing wrapper around the get primary name action
   *  @param name the base name to lookup an alias for
   *  @return the mapped alias name if it exists, otherwise the provided name
   */
  def getPrimaryName(name: String): Future[Option[Alias]] = db.run(getPrimaryNameAction(name))

  /** Get Primary Name Action
   *  Search the alias table for a primary name to match the given base name,
   *  if the match exists return the resolved primary name, otherwise return
   *  the given name
   *  @param name the base name to lookup an alias for
   *  @return the mapped alias name if it exists, otherwise the provided name
   */
  def getPrimaryNameAction(name: String): DBIO[Option[Alias]] = {
    aliasTable.filter{ _.aliasedName === name }.result.map{ _.headOption }
  }
}
