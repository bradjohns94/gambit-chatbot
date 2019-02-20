package com.gambit.core.bot.commands

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

import com.typesafe.scalalogging.Logger

import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.GambitUsersReference

/** Create User Command
 *  Register a new user into the gambit users table
 */
class CreateUser(usersTable: GambitUsersReference) extends Command {
  val logger = Logger("Create User")

  val help = s"Create a new user in the ${botName} database unlinked to any client"

  def runCommand(message: CoreMessage): Future[Option[CoreResponse]] = {
    parse(message.messageText) match {
      case Some(nickname) => usersTable.createGambitUser(nickname).map{ _ match {
        case Success(nick) => Some(CoreResponse(s"Successfully created user: ${nick}"))
        case Failure(err) => {
          logger.info(s"Failed to create user due to database error: ${err.getMessage}")
          Some(CoreResponse(s"Failed to create user ${nickname}"))
        }}
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
