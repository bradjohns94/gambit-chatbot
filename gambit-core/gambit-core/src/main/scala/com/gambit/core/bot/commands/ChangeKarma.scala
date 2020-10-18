package com.gambit.core.bot.commands

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

import cats.implicits._
import com.typesafe.scalalogging.Logger

import com.gambit.core.bot.commands.common.KarmaConstants
import com.gambit.core.clients.{AliasClient, Karma, KarmaClient}
import com.gambit.core.common.{CoreMessage, CoreResponse}
import com.gambit.core.models.RedisReference

/** Change Karma Rate Limit Config
 *  Set of configuration params to determine how rate limiting works on the
 *  change karma command
 *  @param maxUserRequests the number of allowed requests per user in a time period
 *  @param userRequestTimeout the ttl period in the cache for user updates
 *  @param maxChannelRequests the number of allowed requests per channel in a time period
 *  @param channelRequestTimeout the ttl period in the cache for channel updates
 */
case class ChangeKarmaRateLimitConfig(
  maxUserRequests: Int,
  userRequestTimeout: Int,
  maxChannelRequests: Int,
  channelRequestTimeout: Int
)

/** Change Karma Command
 *  Update or create a value in the karma database, incrementing or decrementing
 *  the given entity by one assuming it passes the rate limit checks
 */
class ChangeKarma(
  aliasClient: AliasClient,
  karmaClient: KarmaClient,
  redis: RedisReference,
  rateLimitConfig: ChangeKarmaRateLimitConfig
) extends Command {
  val logger = Logger("Change Karma")

  val help = s"Increment or decrement a value in the karma database by one"
  val example = "foo++ bar--"

  private val karmaMultiplier = -3 // Punishment for self increments

  type ChangeMap = Future[Map[String, Int]]
  type IncMap = Map[String, (Int, Int)]

  /** Run Command
   *  Determine whether the received message matches the command and, if so,
   *  determine what to increment in which direciton and update the database
   *  @param message the received message to be parsed
   *  @return a confirmation to the user if the command parsed
   */
  def runCommand(message: CoreMessage): Future[Option[CoreResponse]] = {
    val (incNames, decNames) = parse(message.messageText)
    val numUpdates = incNames.length + decNames.length
    if (numUpdates > 0) {
      logger.info("Message matched command: ChangeKarma")
      if (isRateLimited(message, numUpdates)) {
        Future(Some(CoreResponse("Bitch be cool!", message.channel)))
      } else {
        val changeMap = mergeChanges(incNames, decNames)
        val userId = message.gambitUser.map{ _.userId }
        changeKarma(message, changeMap, userId)
      }
    } else {
      Future(None)
    }
  }

  /** Is Rate Limited
   *  Check whether the given message exceeds the rate limit after the number of updates
   *  @param message the message the bot received
   *  @param numUpdates the number of changes to make
   *  @return true if the user or channel is ratelimited
   */
  private def isRateLimited(message: CoreMessage, numUpdates: Int): Boolean = {
    val userLimit = message.gambitUser.flatMap{ user =>
      updateUserRateLimit(user.userId, message.client, numUpdates)
    }.getOrElse(false)
    val clientLimit = updateChannelRateLimit(
      message.channel, message.client, numUpdates).getOrElse(false)
    userLimit | clientLimit
  }

  /** Update Channel Rate Limit
   *  Update the cache counter for the channel rate limit and check if it's exceeded
   *  @param channel the channel to update the rate limit for
   *  @param client the client the message was received over
   *  @param numUpdates the amount to increment the counter by
   *  @return whether or not the channel has exceeded the rate limit
   */
  private def updateChannelRateLimit(
    channel: String,
    client: String,
    numUpdates: Int
  ): Option[Boolean] = {
    val key = s"RateLimit:${client}:Channel:${channel}"
    redis.incrbyex(key, numUpdates, rateLimitConfig.channelRequestTimeout).map{
      _ > rateLimitConfig.maxChannelRequests
    }
  }

  /** Update User Rate Limit
   *  Update the cache counter for the user rate limit and check if it's exceeded
   *  @param userId the id of the user to update the rate limit for
   *  @param client the client the message was received over
   *  @param numUpdates the amount to increment the counter by
   *  @return whether or not the user has exceeded the rate limit
   */
  private def updateUserRateLimit(
    userId: Int,
    client: String,
    numUpdates: Int
  ): Option[Boolean] = {
    val key = s"RateLimit:${client}:User:${userId}"
    redis.incrbyex(key, numUpdates, rateLimitConfig.userRequestTimeout).map{
      _ > rateLimitConfig.maxUserRequests
    }
  }

  /** Merge Changes
   *  Combine the list of items to increment and decrement into a single map of
   *  alias-resolved name -> update amount
   *  @param incNames a list of non-unique items to increment
   *  @param decNames a list of non-unique items to decrement
   *  @return a mapping of unique, alias-resolved name -> karma diff
   */
  private def mergeChanges(incNames: Seq[String], decNames: Seq[String]): ChangeMap = {
    val incMap = incNames.groupBy(identity).mapValues{_.size}
    val decMap = decNames.groupBy(identity).mapValues{_.size * -1}
    val merged = incMap combine decMap
    Future.sequence(
      merged.map{ case (name, inc) =>
        aliasClient.getPrimaryName(name).map{ maybeAlias =>
          (maybeAlias.map{ _.primaryName }.getOrElse(name), inc)
          // maybeAlias.map{ alias => (alias.primaryName, inc) }
        }
      }
    ).map{ _.toMap }
  }

  /** Change Karma
   *  Update the karma database values, get the update mapping, and build a response
   *  @param message the message to build the response to
   *  @param changes the mapping of karma database changes
   *  @param userId the ID of the user issuing the update
   *  @return a response message describing the changes
   */
  private def changeKarma(
    message: CoreMessage,
    changes: ChangeMap,
    userId: Option[Int]
  ): Future[Option[CoreResponse]] = {
    val futureChanges = userId match {
      case Some(id) => getModifiedUpdates(changes, id)
      case None => changes
    }
    futureChanges.flatMap{ modifiedChanges =>
      karmaClient.updateKarma(modifiedChanges).map{ updatedKarma =>
        mkResponse(mergeChanges(modifiedChanges, updatedKarma), message)
      }
    }
  }

  /** Make Response
   *  Convert a mapping of name -> (increment, value) into a response message
   *  @param changes the mapping of karma database changes
   *  @param message the core message to respond to
   *  @return a response message
   */
  private def mkResponse(
    changes: IncMap,
    message: CoreMessage
  ): Option[CoreResponse] = {
    val messages = changes.foldLeft(Seq.empty[String]) { case (state, (name, (inc, total))) =>
      val messageLine = inc match {
        case x if x > 0 => s"Gave ${inc} karma to ${name.capitalize}, total: ${total}"
        case x if x < 0 => s"Took ${math.abs(inc)} karma from ${name.capitalize}, total: ${total}"
        case _ => s"${message.username} giveth and ${message.username} taketh away"
      }
      state :+ messageLine
    }
    Some(CoreResponse(messages.mkString("\n"), message.channel))
  }

  /** Get Modified Updates
   *  Get a mapping of Name -> Increment mappings after resolving the linked names to the uid
   *  in the database and punish them for self-increments
   *  @param changes the non-punished mapping of changes
   *  @param uid the user ID to punish self-increments for
   *  @return a change map correcting for self-increment punishments
   */
  private def getModifiedUpdates(changes: ChangeMap, uid: Int): ChangeMap = {
    karmaClient.getKarmaForUser(uid).flatMap{ linkedKarma =>
      val linkedNames = linkedKarma.map{ _.name }
      changes.map{ changeMap =>
        changeMap.map{ case (name, increment) =>
          if (linkedNames.contains(name)) {
            (name, math.abs(increment) * karmaMultiplier)
          } else {
            (name, increment)
          }
        }
      }
    }
  }

  /** Merge Changes
   *  Combine the update map with the karma objects returned from the karma API into one IncMao
   *  @param changeMap the mapping of karma names -> increments
   *  @param updatedKarma the list of karma objects returned from the API
   *  @return an IncMap of name -> (increment, total) for all overlapping karma
   */
  def mergeChanges(changeMap: Map[String, Int], updatedKarma: Seq[Karma]): IncMap = {
    updatedKarma.foldLeft(Map.empty[String, (Option[Int], Int)]) { (merged, karma) =>
      merged ++ Map(karma.name -> (changeMap.get(karma.name), karma.value))
    }.collect{ case (name, (Some(inc), value)) => (name, (inc, value))}
  }

  /** Parse
   *  If the message provided matches the link user command, return the entity
   *  name to update karma for
   *  @param messageText the incoming message to parse
   *  @return the name of the entity to update karma for
   */
  private def parse(messageText: String): (Seq[String], Seq[String]) = {
    val matchIncrement = """(?i)(?=(?:\S\s+|^)(%s\s?\+\+)(?:\s+\S|$))""".format(
      KarmaConstants.karmaRegex).r
    val matchDecrement = """(?i)(?=(?:\S\s+|^)(%s\s?\-\-)(?:\s+\S|$))""".format(
      KarmaConstants.karmaRegex).r
    val incNames = matchIncrement.findAllIn(messageText).matchData.map{ update =>
      update.group(1).replace("+", "").replace("(", "").replace(")", "").trim.toLowerCase
    }.toSeq
    val decNames = matchDecrement.findAllIn(messageText).matchData.map{ update =>
      update.group(1).replace("-", "").replace("(", "").replace(")", "").trim.toLowerCase
    }.toSeq
    (incNames, decNames)
  }
}
