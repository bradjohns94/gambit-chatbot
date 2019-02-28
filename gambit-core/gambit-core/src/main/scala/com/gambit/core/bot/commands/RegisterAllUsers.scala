package com.gambit.core.bot.commands

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

import com.typesafe.scalalogging.Logger

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.ClientReference

/** Register All Users Command
 *  Bulk create and link all users currently in the database that are not
 *  otherwise registered for the calling client
 */
class RegisterAllUsers(clientMapping: Map[String, ClientReference]) extends Command {
  val logger = Logger("Register All Users")

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
          case Some(clientReference) => registerUsers(clientReference)
          case None => {
            logger.info(s"No client reference found for client: ${message.client}")
            val response = s"${botName} ${message.client} client does not currently support " +
                            "bulk user registration"
            Future(Some(CoreResponse(response)))
          }
        }
      }
      case false => Future(None)
    }
  }

  private def registerUsers(clientReference: ClientReference): Future[Option[CoreResponse]] = {
    clientReference.registerUnlinkedUsers.map{ numRegistered =>
      if (numRegistered > 0) {
        Some(CoreResponse(s"Successfully registered ${numRegistered} users"))
      } else {
        Some(CoreResponse("Failed to register any new users"))
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