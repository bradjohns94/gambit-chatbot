package com.gambit.core.bot.commands

import scala.util.Random // TODO this is dirty and we should be using functional state
import scala.util.matching.Regex

import com.typesafe.scalalogging.Logger

import com.gambit.core.common.Types.{CoreMessage, CoreResponse}

/** Hello Command
 *  Simple ping command to respond to friendly users who say hello
 */
object Hello extends Command {
  val logger = Logger("Hello")

  val help = s"Give ${botName} a friendly greeting! Simply say: 'Hi, ${botName}!'"

  private val greetings = Seq(
    "hi",
    "hello",
    "hey",
    "howdy",
    "salutations",
    "greetings",
    "'ello'",
    "hola"
  )

  /** Run Command
   *  Parse the message text and, if it successfully returns a result then
   *  return the result of make message, otherwise return None
   *  @param message the CoreMessage object sent from MessageAPI
   *  @return the result of makeMessage if the message parses, otherwise None
   */
  def runCommand(message: CoreMessage): Option[CoreResponse] =
    parse(message.messageText) match {
      case true => {
        logger.info("Message matched command: Hello")
        Some(makeMessage(message.username, None))
      }
      case false => None
    }

  /** Parse
   *  Determine whether or not the provided message matches the regex pattern
   *  for this command.
   *  @param messageText the incoming message to parse
   *  @return a boolean indicating whether or not the command matches
   */
  private def parse(messageText: String): Boolean = {
    val safeGreetings = greetings.map(Regex.quoteReplacement).mkString("|")
    val matchString = """(?i)^(%s)[:,]?\s+%s[.!?]?$""".format(safeGreetings, botName).r
    matchString.pattern.matcher(messageText).matches
  }

  /** Make Message
   *  Determine a random message (potentially based off a seed) to respond with
   *  I'd feel a lot worse about this using scala.util.Random if it wasn't a
   *  one off in an asynchronous thread, so state wouldn't go anywhere.
   *  @param username the username to respond to
   *  @param randomSeed the optional random seed to use (nice for testing)
   *  @return a CoreMessage generated from the random seed
   */
  private def makeMessage(username: String, randomSeed: Option[Long]): CoreResponse = {
    val rand = randomSeed match {
      case Some(seed) => new Random(seed)
      case None => new Random()
    }
    val randomGreeting = greetings(rand.nextInt(greetings.length))
    CoreResponse(s"${randomGreeting}, ${username}!".capitalize)
  }
}
