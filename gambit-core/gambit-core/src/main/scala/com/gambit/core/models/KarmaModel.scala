package com.gambit.core.models

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.sql.Timestamp

import com.typesafe.scalalogging.Logger
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

  val logger = Logger("Karma Reference")

  /** Get Karma Value
   *  DB Wrapper around Get Karma Value Action
   *  @param name the name of the karma key to lookup
   *  @return the value in the database or 0
   */
  def getKarmaValue(name: String): Future[Int] = db.run(getKarmaValueAction(name))

  /** Get Karma Value Action
   *  Get the karma value (if any) from a potentially alias-mapped karma name,
   *  if the karma name is not in the database return 0.
   *  @param name the name of the karma key to lookup
   *  @return the value in the database or 0
   */
  def getKarmaValueAction(name: String): DBIO[Int] =
    aliasReference.getPrimaryNameAction(name).flatMap{ mappedName =>
      karmaTable.filter{_.name === mappedName}.map{_.value}.result.map{
        _.headOption.getOrElse(0)
      }
    }

  /** Get User Linked Karma
   *  Get a list of karma names that are linked to the specified user
   *  @param userId the ID of the user in the gambit users table
   *  @return a sequence of any karma names linked to the user
   */
  def getUserLinkedKarma(userId: Int): Future[Seq[String]] = {
    logger.info(s"Getting karma linked to user ${userId}")
    db.run(karmaTable.filter{ _.linkedUser === userId }.map{ _.name }.result)
  }

  /** Increment Karma Action
   *  Given a mapping of karma name -> increment, update the values of all
   *  specified rows creating them if they don't exist
   *  @param changeMap the mapping of name -> increment updates
   *  @return a name -> karma value mapping for all updated items
   */
  def incrementKarma(changeMap: Future[Map[String, Int]]): Future[Map[String, (Int, Int)]] = {
    changeMap.flatMap{changes =>
      val actions = changes.map{ case (name, inc) => incrementSingleKarma(name, inc) }.toSeq
      db.run(DBIO.fold(actions, Map.empty[String, (Int, Int)]) { _ ++ _ })
    }
  }

  /** Increment Single Karma
   *  Fetch the current value for the given name defaulting to 0 and upsert
   *  the incremented value into the karma table
   *  @param name the unique name to be updated
   *  @param increment the amount to increment by
   *  @return a mapping of the name to the updated value
   */
  def incrementSingleKarma(name: String, increment: Int): DBIO[Map[String, (Int, Int)]] = {
    logger.info(s"Incrementing karma for ${name} by ${increment}")
    getKarmaValueAction(name).flatMap{ currentValue =>
      karmaTable.map{ karma =>
        (karma.name, karma.value)
      }.insertOrUpdate(name, currentValue + increment).flatMap{ _ =>
        getKarmaValueAction(name).map{ fetchedValue => Map(name -> (increment, fetchedValue))}
      }
    }
  }
}
