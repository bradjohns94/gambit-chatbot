package com.gambit.core.bot.engines

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.typesafe.scalalogging.Logger

import com.gambit.core.bot.commands._
import com.gambit.core.common.Types.{CoreMessage, CoreResponse}

/** MessageEngine
 *  The core object for converting an input message object from the client
 *  facing API into any number of responses for the bot to output. The object
 *  maintains a list of accepted input commands and matches the message to
 *  all that return a value from runCommand
 */
object MessageEngine {
  val logger = Logger("MessageEngine")

  val enabledCommands = Seq(
    Hello
  )

  /** Parse Message
   *  Map each command to a potential async response, then filter out any
   *  command responses that are either None or had a failed future
   *  @param message the message to be parsed and run
   *  @return a future sequence of all commands that returned a successful
   *          response
   */
  def parseMessage(message: CoreMessage): Future[Seq[CoreResponse]] = {
    logger.info(s"Recieved message: ${message.messageText} from user: ${message.username}")
    Future.sequence(enabledCommands.map{ _.runAsyncCommand(message) })
          .flatMap{ maybeResponses => Future(maybeResponses.flatten) }
  }
}
