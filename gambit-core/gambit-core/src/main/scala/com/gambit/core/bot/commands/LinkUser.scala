package com.gambit.core.bot.commands

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.matching.Regex

import com.typesafe.scalalogging.Logger

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.ClientReference

/** Link User Command
 *  Link an existing client user to a user in the gambit database
 */
class LinkUser(clientMapping: Map[String, ClientReference]) extends Command {
  val logger = Logger("Link User")

  val help = s"Link an existing user in the ${botName} database to a specified client user"
  val example = s"${botName}: link [client ID] to [nickname]"

  /** Run Command
   *  Determine whether the received message matches the command and, if so, link
   *  the provided gambit user to the specified client user and respond appropriately
   *  @param message the received message to be parsed
   *  @return a confirmation to the user if the command parsed
   */
  def runCommand(message: CoreMessage): Future[Option[CoreResponse]] = {
    parse(message.messageText) match {
      case Some((clientId, nickname)) => {
        logger.info("Message matched command: LinkUser")
        clientMapping.get(message.client) match {
          case Some(clientReference) => linkClientUser(
            clientReference, nickname, clientId, message.channel)
          case None => {
            logger.info(s"No client reference found for client: ${message.client}")
            val response = s"${botName} ${message.client} client does not currently support " +
                            "linking users"
            Future(Some(CoreResponse(response, message.channel)))
          }
        }
      }
      case None => Future(None)
    }
  }

  /** Link Client User
   *  Given a ClientUser object to link the user from, a client ID to be reference,
   *  and the nickname of the gambit user to be linked, update the client table to
   *  reference the associated gambit user or log a failure if something goes wrong
   *  @param clientReference the reference object to the client table
   *  @param nickname the unique nickname for the gambit user
   *  @param clientId the unique clientId of the user
   *  @return a core response with an appropriate response message
   */
  private def linkClientUser(
    clientReference: ClientReference,
    nickname: String,
    clientId: String,
    channel: String
  ): Future[Option[CoreResponse]] = {
    clientReference.setGambitUserFromNickname(clientId, nickname).map{ _ match {
      case Success(_) => {
        Some(CoreResponse(
          s"Successfully linked client ID ${clientId} to ${nickname}",
          channel
        ))
      }
      case Failure(err) => {
        logger.info(s"Failed to client ID ${clientId} to ${nickname}: ${err.getMessage}")
        Some(CoreResponse(
          s"Failed to link client ID ${clientId} to ${nickname}",
          channel
        ))
      }
    }}
  }

  /** Parse
   *  If the message provided matches the link user command, return the gambit nickname
   *  and client ID to be linked
   *  @param messageText the incoming message to parse
   *  @return a (client ID, nickname) tuple to link if the command parsed
   */
  private def parse(messageText: String): Option[(String, String)] = {
    val baseRegex = """(?i)^(?:%s)[:,]?\s+link\s+([\w\d]+)\s+to\s+([\w|\d]+)$"""
    val matchRegex = baseRegex.format(botName).r
    messageText match {
      case matchRegex(clientId, nickname) => Some((clientId, nickname))
      case _ => None
    }
  }
}
