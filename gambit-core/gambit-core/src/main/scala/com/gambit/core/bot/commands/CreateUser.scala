package com.gambit.core.bot.commands

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.matching.Regex

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import com.gambit.core.clients.GambitUserClient
import com.gambit.core.common.{CoreMessage, CoreResponse}

/** Create User Command
 *  Register a new user into the gambit users table
 */
class CreateUser(userClient: GambitUserClient) extends Command {
  val logger = Logger(LoggerFactory.getLogger(classOf[CreateUser]))

  val help = s"Create a new user in the ${botName} database unlinked to any client"
  val example = s"${botName}: create user [nickname]"

  /** Run Command
   *  Determine whether the received message matches the command and, if so, create
   *  a new user in the gambit database with the provided nickname
   *  @param message the received message to be parsed
   *  @return a confirmation to the user if the command parsed
   */
  def runCommand(message: CoreMessage): Future[Option[CoreResponse]] = {
    parse(message.messageText) match {
      case Some(nickname) => {
        logger.info("Message matched command: CreateUser")
        userClient.createGambitUser(nickname).map{ _ match {
          case Some(user) => Some(CoreResponse(
            s"Successfully created user ID ${user.userId}",
            message.channel
          ))
          case None => {
            logger.info(s"Failed to create user due to database error")
            Some(CoreResponse(s"Failed to create user ${nickname}", message.channel))
          }}
        }
      }
      case None => Future(None)
    }
  }

  /** Parse
   *  If the message provided matches the create user command return the nickname for the
   *  user to be created
   *  @param messageText the incoming message to parse
   *  @return the nickname of the user to be created if the command matches
   */
  private def parse(messageText: String): Option[String] = {
    val matchRegex = """(?i)^(?:%s)[:,]?\s+create\s+user\s+([\w\d]+)$""".format(botName).r
    messageText match {
      case matchRegex(nickname) => Some(nickname)
      case _ => None
    }
  }
}
