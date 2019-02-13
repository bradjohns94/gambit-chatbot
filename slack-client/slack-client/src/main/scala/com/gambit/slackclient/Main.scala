package com.gambit.slackclient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import akka.actor.{ActorSystem, ActorRef}
import com.typesafe.scalalogging.Logger
import org.postgresql.ds.PGSimpleDataSource
import slack.models._
import slack.rtm.SlackRtmClient
import slick.jdbc.PostgresProfile.api._

import com.gambit.slackclient.models.{SlackUsers, UserUtils}
import com.gambit.slackclient.slackapi.SlackMessager
import com.gambit.slackclient.handlers._

/** Main
 *  Main application object for the gambit slack client
 */
object Main {
  val logger = Logger("Main")

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

  val db = getDatabaseFromEnvironment
  val usersTable = TableQuery[SlackUsers]

  /** Setup Client
   *  Create a client to establish a connection with slack
   *  @return a Slack Realtime Messaging client derived from the environment=
   */
  private def setupClient: SlackRtmClient = {
    implicit val system = ActorSystem("slack")
    val token = sys.env("SLACK_API_TOKEN")
    SlackRtmClient(token)
  }

  /** Update Users
   *  Sync any users in the client state that aren't already in the database to
   *  the postgres database
   *  @param client the current RTM client
   */
  private def updateUsers(client: SlackRtmClient): Unit = {
    Try{
      logger.info("Syncing slack users...")
      Await.result(db.run(UserUtils.syncUsers(usersTable, client.state.users)), 1 minutes)
    } match {
      case Success(maybeNumInserts) => maybeNumInserts match {
        case Some(numInserts) => logger.info(s"Inserted ${numInserts} users")
        case None => logger.info("Found no new users to sync")
      }
      case Failure(_) => logger.warn("Failed to update the users list")
    }
  }

  /** Register events
   *  Map event types to the correct responses
   *  @param client a slack client to map events against
   */
  private def registerEvents(client: SlackRtmClient): ActorRef = {
    client.onEvent {
      case message: Message => MessageHandler.processEvent(message)
        .onSuccess{ responses => responses.foreach{ SlackMessager.send(client, _) } }
        .onFailure{ error => logger.warn(s"Failed to send response ${error}") }
      case _: ChannelJoined => db.run(UserUtils.syncUsers(usersTable, client.state.users))
      case _: GroupJoined => db.run(UserUtils.syncUsers(usersTable, client.state.users))
      case _: MpImJoined => db.run(UserUtils.syncUsers(usersTable, client.state.users))
      case _: MemberJoined => db.run(UserUtils.syncUsers(usersTable, client.state.users))
      case _ => logger.info("Recieved unsupported event type")
    }
  }

  /** Main
   *  Main function of the application, setup a slack client and manage
   *  any event handlers found in the handlers directory
   *  @param args CLI args (not used)
   */
  def main(args: Array[String]): Unit = {
    logger.info("Starting Gambit Slack Client...")
    val client = setupClient
    updateUsers(client)
    registerEvents(client)
    // The bot should be configured and running now
    logger.info("Setup complete, running Gambit Slack Client...")
  }
}
