package com.gambit.core.bot.commands

import com.twitter.util.Future

import com.gambit.core.common.{CoreMessage, CoreResponse}

trait Command {
   // Require a string to explain how to use the command
   val help: String
   val example: String

   val botName = sys.env("BOT_NAME")

  /** Run Async Command
   *  Given an incoming message from the core API equipped with the information
   *  required to parse a command, respond with a response if the message
   *  meets the trigger requirements for the command
   *  @param message a case class containing all information needed to parse a
   *                 command
   *  @return an eventual command response if the message matched the expected command
   */
  def runCommand(message: CoreMessage): Future[Option[CoreResponse]]
}
