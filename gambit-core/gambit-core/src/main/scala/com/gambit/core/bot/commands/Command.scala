package com.gambit.core.bot.commands

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.gambit.core.common.Types.{CoreMessage, CoreResponse}

trait Command {
   // Require a string to explain how to use the command
   val help: String

   val botName = sys.env("BOT_NAME")

  /** Run Async Command
   *  Async wrapper around Run Command
   *  @param message a case class containing all information needed to parse a
   *                 command
   *  @return a future result of runCommand
   */
  def runAsyncCommand(message: CoreMessage): Future[Option[CoreResponse]] =
    Future(runCommand(message))

  /** Run Async Command
   *  Given an incoming message from the core API equipped with the information
   *  required to parse a command, respond with a response if the message
   *  meets the trigger requirements for the command
   *  @param message a case class containing all information needed to parse a
   *                 command
   *  @return Nothing if the command does not parse or a response message
   */
   def runCommand(message: CoreMessage): Option[CoreResponse]
}
