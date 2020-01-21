package com.gambit.karma.models

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

  def getKarmaByName(name: String): Future[Option[Karma]] = db.run(getKarmaByNameAction(name))

  private def getKarmaByNameAction(name: String): DBIO[Option[Karma]] =
    aliasReference.getPrimaryNameAction(name).flatMap{ alias =>
      val primaryName = alias.map{ _.primaryName }.getOrElse(name)
      karmaTable.filter{ _.name === primaryName }.result.map{_.headOption}
    }

  /** Get User Linked Karma
   *  Get a list of karma names that are linked to the specified user
   *  @param userId the ID of the user in the gambit users table
   *  @return a sequence of any karma names linked to the user
   */
  def getUserLinkedKarma(userId: Int): Future[Seq[Karma]] = {
    logger.info(s"Getting karma linked to user ${userId}")
    db.run(karmaTable.filter{ _.linkedUser === userId }.result)
  }

  def incrementKarma(changeMap: Map[String, Int]): Future[Seq[Karma]] = {
    val actions = changeMap.map{ case(name, inc) => incrementSingleKarma(name, inc) }.toSeq
    db.run(DBIO.sequence(actions)).map{ _.flatten }
  }

  private def incrementSingleKarma(name: String, increment: Int): DBIO[Option[Karma]] = {
    getKarmaByNameAction(name).flatMap{ currentKarma =>
      val currentValue = currentKarma.flatMap{ _.value }.getOrElse(0)
      karmaTable.map{ karma =>
        (karma.name, karma.value)
      }.insertOrUpdate(name, currentValue + increment).flatMap{ _ =>
        getKarmaByNameAction(name)
      }
    }
  }
}
