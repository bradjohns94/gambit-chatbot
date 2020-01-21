package com.gambit.core.bot.engines

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api.Database

import com.redis.RedisClient
import com.typesafe.scalalogging.Logger

import com.gambit.core.bot.commands._
import com.gambit.core.common.{ClientMessage, ClientMessageResponse, CoreMessage, CoreResponse}
import com.gambit.core.clients._
import com.gambit.core.models.RedisReference

trait MessageConfig {
  // Command list for unregistered users
  val unregisteredCommands: Seq[Command]
  // Command list for registered users
  val registeredCommands: Seq[Command]
  // Command list for administrators
  val adminCommands: Seq[Command]
  // Mapping of client identifier to table reference
  val clientMapping: Map[String, Client]
}

/** Message Engine Config
 *  Copnfiguration object containing the static lists of permissioned commands
 *  @param db the database to connect to
 */
case class MessageEngineConfig(db: Database, redis: RedisClient) extends MessageConfig {
  private val aliasClient = new AliasClient
  private val gambitUserClient = new GambitUserClient
  private val karmaClient = new KarmaClient
  private val redisReference = new RedisReference(redis)

  private val userKarmaPerMinute = 10
  private val channelKarmaPerMinute = 100
  private val minuteSeconds = 60
  private val karmaRateLimit = new ChangeKarmaRateLimitConfig(
    userKarmaPerMinute, minuteSeconds, channelKarmaPerMinute, minuteSeconds)

  // Mapping of client identifier to table reference
  val clientMapping = Map(
    "slack" -> new SlackUserClient
  )

  // Command list for unregistered users
  val unregisteredCommands = Seq(
    new Hello,
    new GetKarma(karmaClient)
  )

  // Command list for registered users
  val registeredCommands = Seq[Command](
    new ChangeKarma(aliasClient, karmaClient, redisReference, karmaRateLimit)
  )

  // Command list for administrators
  val adminCommands = Seq[Command](
    new CreateUser(gambitUserClient),
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
  def parseMessage(message: ClientMessage): Future[ClientMessageResponse] = {
    logger.info(s"Recieved message: ${message.messageText} from user: ${message.username}")
    translateMessage(message).flatMap{ coreMessage =>
      Future.sequence(
        getPermittedCommands(coreMessage.gambitUser).map{ command =>
          command.runCommand(coreMessage)
        }
      ).flatMap{ maybeResponses => Future(maybeResponses.flatten)}
    }.map{ translateResponse(_) }
  }

  /** Translate Client Message
   *  Convert a client message into a core message consumed by the message
   *  engine.
   *  @param message a client message recieved from an API client
   *  @return a core message derived from the client message
   */
  private def translateMessage(message: ClientMessage): Future[CoreMessage] =
    getMessageUser(message).map{ gambitUser =>
      CoreMessage(
        message.userId,
        message.username,
        message.channel,
        message.messageText,
        message.client,
        gambitUser
      )
    }

  /** Translate Core Response
   *  Given a core message passed back from the message engine, convert it into
   *  a ClientMessageResponse returnable from the API
   *  @param response a CoreResponse to translate
   *  @return a ClientMessageResponse derived from the CoreResponse
   */
  private def translateResponse(responses: Seq[CoreResponse]): ClientMessageResponse =
    ClientMessageResponse(responses)

  /** Get Message User
   *  Resolve the gambit user (if any) from the given message
   *  @param message the client message to fetch the user from
   *  @return the associated gambit user if one is found
   */
  private def getMessageUser(message: ClientMessage): Future[Option[GambitUser]] = {
    config.clientMapping.get(message.client) match {
      case Some(clientReference) => clientReference.getGambitUserById(message.userId)
      case None => Future(None)
    }
  }

  /** Get Permitted Commands
   *  Get a list of permitted commands for a user defaulting to the unregistered
   *  command list if the user is not found
   *  @param maybeUser an option potentially containing a gambti user
   *  @return a sequnece of commands permitted to be used by the user
   */
  private def getPermittedCommands(maybeUser: Option[GambitUser]): Seq[Command] = maybeUser match {
    case Some(user) => user.isAdmin match {
      case true => {
        logger.info(s"Gambit User ID ${user.userId} resolved as an administrative user")
        config.unregisteredCommands ++
        config.registeredCommands ++
        config.adminCommands
      }
      case _ => {
        logger.info(s"Gambit User ID ${user.userId} resolved as a registered user")
        config.unregisteredCommands ++
        config.registeredCommands
      }
    }
    case None => {
      logger.info(s"User resolved as an unregistered user")
      config.unregisteredCommands
    }
  }
}
