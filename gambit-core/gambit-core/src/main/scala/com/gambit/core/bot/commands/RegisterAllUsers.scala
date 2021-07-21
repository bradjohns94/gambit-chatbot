package com.gambit.core.bot.commands

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import com.gambit.core.clients.{GambitUserClient, UserClient}
import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.services.UsersService

/** Register All Users Command
 *  Bulk create and link all users currently in the database that are not
 *  otherwise registered for the calling client
 */
class RegisterAllUsers(gambitUserClient: GambitUserClient, clientMapping: Map[String, UserClient]) extends Command {
  val logger = Logger(LoggerFactory.getLogger(classOf[RegisterAllUsers]))

  val help = s"Create a new ${botName} user for all unregistered users for the client and link " +
             s"their client user to the created ${botName} user"
  val example = s"${botName}: register all unlinked users"

  /** Run Command
   *  Determine whether the received message matches the command and, if so, create a new gambit
   *  user for them and link it with their respective client user.
   *  @param message the received message to be parsed
   *  @return a confirmation to the user if the command parsed
   */
  def runCommand(message: CoreMessage): Future[Option[CoreResponse]] = {
    parse(message.messageText) match {
      case true => {
        logger.info("Message matched command: RegisterAllUsers")
        clientMapping.get(message.client) match {
          case Some(clientReference) => registerUsers(clientReference, message.channel)
          case None => {
            logger.info(s"No client reference found for client: ${message.client}")
            val response = s"${botName} ${message.client} client does not currently support " +
                            "bulk user registration"
            Future(Some(CoreResponse(response, message.channel)))
          }
        }
      }
      case false => Future(None)
    }
  }

  /** Register User
   *  Helper function to register all unlinked users and return a response accordingly
   *  with how many users were updated
   *  @param clientReference the database helper to register users with
   *  @param channel the channel the message was sent over
   *  @return a core response to forward to the user
   */
  private def registerUsers(
    client: UserClient,
    channel: String
  ): Future[Option[CoreResponse]] = {
    UsersService.registerUnlinkedUsers(client, gambitUserClient).map{ numRegistered =>
      if (numRegistered > 0) {
        Some(CoreResponse(s"Successfully registered ${numRegistered} users", channel))
      } else {
        Some(CoreResponse("Failed to register any new users", channel))
      }
    }
  }

  /** Parse
   *  Determine whether or not the message received matches the command regex
   *  @param messageText the incoming message to parse
   *  @return whether or not the message text matches the command
   */
  private def parse(messageText: String): Boolean = {
    val matchRegex = """(?i)^(?:%s)[:,]?\s+register\s+all\s+unlinked\s+users$""".format(botName).r
    matchRegex.pattern.matcher(messageText).matches
  }
}
