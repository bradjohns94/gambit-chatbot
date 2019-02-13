package com.gambit.tools.admin

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import com.typesafe.scalalogging.Logger
import org.postgresql.ds.PGSimpleDataSource
import slick.basic.DatabasePublisher
import slick.jdbc.PostgresProfile.api._
import slick.sql.FixedSqlAction
import slick.lifted.TableQuery

import com.gambit.tools.admin.models._


/** Main
 *  Parse out CLI parameters and run the main function to add the admin
 *  user to the gambit database
 */
object Main {
  val logger = Logger("Main")
  type OptionMap = Map[Symbol, String]

  val usage = "Usage: add-admin [--username name] [--client client type] [--client-id user id]"

  /** Next Option
   *  Recursive function to parse out commandline arguments or exit if the
   *  expected configuration isn't provided.
   *  @param map an OptionMap type of the current config
   *  @param list the list of arguments the function has yet to parse
   *  @return a mapping of cli arguments to some type of value
   */
  def nextOption(map : OptionMap, list: List[String]): OptionMap = list match {
    case Nil => (map contains 'username) && (map contains 'client) && (map contains 'clientId) match {
      case true => map
      case false => {
        logger.error("Error: missing arguments")
        logger.info(usage)
        sys.exit(1)
      }
    }
    case "--client" :: value :: tail => nextOption(map ++ Map('client -> value), tail)
    case "--username" :: value :: tail => nextOption(map ++ Map('username -> value), tail)
    case "--client-id" :: value :: tail => nextOption(map ++ Map('clientId -> value), tail)
    case option :: tail => {
      logger.error("Error: unknown option " + option)
      logger.info(usage)
      sys.exit(1)
    }
  }

  /** Get Database From Environment
   *  Setup a connection to the gambit postgres database from a set of variables
   *  stored in the system environment.
   *  @return a slick database connection object
   */
  val getDatabaseFromEnvironment: Database = {
    val dataSource = new PGSimpleDataSource()
    dataSource.setServerName(sys.env("PG_URL"))
    dataSource.setUser(sys.env("PG_USER"))
    dataSource.setPassword(sys.env("PG_PASSWORD"))
    dataSource.setDatabaseName(sys.env("PG_DB"))
    Database.forDataSource(dataSource, None)
  }

  val db: Database = getDatabaseFromEnvironment

  /** Make Gambit User
   *  Make a new administrator user with the given nickname in the gambit
   *  database and return a DBIO object containing the number of users created
   *  @param users the TableQuery object to create gambit users within
   *  @param nickname the admin user's desired nickname
   *  @return a DBIO object ready to be executed and return the number of
   *          created users
   */
  def makeGambitUser(gambitUsers: TableQuery[GambitUsers], nickname: String): Future[Int] = {
    val insertQuery = gambitUsers.map{ user =>
      (user.nickname, user.isAdmin)
    } returning gambitUsers.map{_.id} += (nickname, true)
    db.run(insertQuery)
  }

  /** Make Slack User
   *  Make a new slack user with the given client ID and gambit user ID in the
   *  gambit database and return a DBIO object containing the number of users
   *  created
   *  @param clientId a unique slack identifier for the user
   *  @param userId a unique reference to the gambit user table to link to
   *  @return a DBIO object ready to be executed and return the number of
   *          created users
   */
  def makeSlackUser(slackId: String, userId: Int): Future[_] = {
    val slackUsers = TableQuery[SlackUsers]
    db.run(
      slackUsers.map{user => (user.slackId, user.gambitUserId)}
      insertOrUpdate (slackId, Some(userId))
    )
  }

  /** Main
   *  Parse out the arguments, load a database connection, and created the
   *  gambit core and client users to be the new administrator
   *  @param args an array of arguments containing the following:
   *         --username - the desired nickname of the new administrator
   *         --client - the primary client the admin wants to be initialized in
   *         --client-id - a unique identifier for the specified client
   */
  def main(args: Array[String]): Unit = {
    val options = nextOption(Map(), args.toList)
    logger.info("Running add-admin...")

    Try{
      val gambitUsers = TableQuery[GambitUsers]

      logger.info("Creating gambit user")
      val transaction = makeGambitUser(gambitUsers, options.apply('username)).flatMap{ userId =>
        logger.info(s"Adding slack client for user ID ${userId}")
        val client = options.apply('client)
        client.toLowerCase match {
          case "slack" => makeSlackUser(options.apply('clientId), userId)
          case _ => Future(logger.error(s"Error: Invalid client ${client}"))
        }
      }
      Await.result(transaction, 1 minutes)
    } match {
      case Success(_) => logger.info("Successfully created admin user")
      case Failure(_) => logger.error("Failed to create admin user")
    }

    logger.info("Finishing add-admin...")
    db.close
  }
}
