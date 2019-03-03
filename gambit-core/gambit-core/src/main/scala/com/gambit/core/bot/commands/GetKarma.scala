package com.gambit.core.bot.commands

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

import com.typesafe.scalalogging.Logger

import com.gambit.core.bot.commands.common.KarmaConstants
import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.KarmaReference

/** Get Karma Command
 *  Fetch and return to the user the karma value in fake internet points of a
 *  given entity in the database
 */
class GetKarma(karmaTable: KarmaReference) extends Command {
  val logger = Logger("Get Karma")

  val help = s"Get the associated value in fake internet points for an arbitrary name"
  val example = s"${botName}: karma foo"

  /** Run Command
   *  Determine whether the received message matches the command and, if so, fetch
   *  the database karma value of the given entity
   *  @param message the received message to be parsed
   *  @return a confirmation to the user if the command parsed
   */
  def runCommand(message: CoreMessage): Future[Option[CoreResponse]] = {
    parse(message.messageText) match {
      case Some(karmaName) => {
        logger.info("Message matched command: GetKarma")
        karmaTable.getKarmaValue(karmaName.toLowerCase).map{ value =>
          Some(CoreResponse(s"Karma for ${karmaName} is ${value}"))
        }
      }
      case None => Future(None)
    }
  }

  /** Parse
   *  If the message provided matches the link user command, return the entity
   *  name to fetch karma for
   *  @param messageText the incoming message to parse
   *  @return the name of the entity to fetch karma for
   */
  private def parse(messageText: String): Option[String] = {
    val matchRegex = """(?i)^(?:%s)[:,]?\s+karma\s+%s$""".format(
      botName, KarmaConstants.karmaRegex).r
    messageText match {
      case matchRegex(karmaEntity) => Some(karmaEntity.replace("(", "").replace(")", ""))
      case _ => None
    }
  }
}
