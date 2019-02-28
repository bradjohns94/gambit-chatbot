package com.gambit.core.bot.engines

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api.Database

import com.typesafe.scalalogging.Logger

import com.gambit.core.bot.commands._
import com.gambit.core.common.{CoreMessage, CoreResponse, PermissionLevel}
import com.gambit.core.models._

trait MessageConfig {
  // Command list for unregistered users
  val unregisteredCommands: Seq[Command]
  // Command list for registered users
  val registeredCommands: Seq[Command]
  // Command list for administrators
  val adminCommands: Seq[Command]
  // Mapping of client identifier to table reference
  val clientMapping: Map[String, ClientReference]
}

/** Message Engine Config
 *  Copnfiguration object containing the static lists of permissioned commands
 *  @param db the database to connect to
 */
case class MessageEngineConfig(db: Database) extends MessageConfig {
  private val gambitUsersTable = new GambitUsersReference(db)

  // Mapping of client identifier to table reference
  val clientMapping = Map(
    "slack" -> new SlackUsersReference(db)
  )

  // Command list for unregistered users
  val unregisteredCommands = Seq(
    new Hello
  )

  // Command list for registered users
  val registeredCommands = Seq[Command](
  )

  // Command list for administrators
  val adminCommands = Seq[Command](
    new CreateUser(gambitUsersTable),
    new LinkUser(clientMapping),
    new RegisterAllUsers(clientMapping)
  )
}

/** MessageEngine
 *  The core object for converting an input message object from the client
 *  facing API into any number of responses for the bot to output. The object
 *  maintains a list of accepted input commands and matches the message to
 *  all that return a value from runCommand
 */
class MessageEngine(config: MessageConfig) {
  val logger = Logger("MessageEngine")

  /** Parse Message
   *  Map each command to a potential async response, then filter out any
   *  command responses that are either None or had a failed future
   *  @param message the message to be parsed and run
   *  @return a future sequence of all commands that returned a successful response
   */
  def parseMessage(message: CoreMessage): Future[Seq[CoreResponse]] = {
    logger.info(s"Recieved message: ${message.messageText} from user: ${message.username}")
    config.clientMapping.get(message.client) match {
      case None => {
        logger.info(s"Received message from unrecognized client type, ${message.client}")
        Future(Seq.empty)
      }
      case Some(clientReference) =>
        getPermittedCommands(clientReference, message.userId).flatMap{ enabledCommands =>
          Future.sequence(enabledCommands.map{ _.runCommand(message) })
                .flatMap{ maybeResponses => Future(maybeResponses.flatten) }
        }
    }
  }

  /** Get Permitted Commands
   *  Get a list of permitted commands for a user defaulting to the unregistered
   *  command list if the user is not found
   *  @param table a reference object to a client user table to query
   *  @param userId the ID of the user to get commands for
   *  @return a sequnece of commands permitted to be used by the user
   */
  private def getPermittedCommands(table: ClientReference, userId: String): Future[Seq[Command]] =
    table.getUserById(userId).flatMap { _ match {
      case Some(user) => getUserCommands(table, user)
      case None => {
        logger.info(s"Client User ${userId} was not found in the resolved client table,"
                    + "defaulting to unregistered permissions.")
        Future(config.unregisteredCommands)
      }
    }}

  /** Get User Commands
   *  Get a list of commands permitted to be used by a given user for a given client
   *  @param table a reference object to a client user table to query
   *  @param user the user object for the client table
   *  @return a sequnece of commands permitted to be used by the user
   */
  private def getUserCommands(table: ClientReference, user: ClientUser): Future[Seq[Command]] =
    table.getPermissionLevel(user).map{ _ match {
      case PermissionLevel.Unregistered => {
        logger.info(s"Gambit User ID ${user.gambitUserId} resolved as an unregistered user")
        config.unregisteredCommands
      }
      case PermissionLevel.Registered => {
        logger.info(s"Gambit User ID ${user.gambitUserId} resolved as a registered user")
        config.unregisteredCommands ++
        config.registeredCommands
      }
      case PermissionLevel.Administrator => {
        logger.info(s"Gambit User ID ${user.gambitUserId} resolved as an administrative user")
        config.unregisteredCommands ++
        config.registeredCommands ++
        config.adminCommands
      }
    }}
}
