package com.gambit.core.common

/** Core Message
 *  A subset of fields from api.MessageAPI.ClientMessage that are used by the
 *  commands run through MessageEngine to produce a response from the bot
 *  @param userId the identifier of the user who sent the message
 *  @param username the username who sent the initial message
 *  @param messageText the actual text body of the message
 *  @param client a string identifying the requesting client
 */
case class CoreMessage(
  userId: String,
  username: String,
  messageText: String,
  client: String
)

/** Core Response
 *  The response object returned by any commands that successfully parse the
 *  recieved message. This may or may not need to be wrapped by other
 *  client-specific information by the messaging API
 *  @param messageText the text of the response
 */
case class CoreResponse(
  messageText: String
)

/** Permission Level
 *  Enumeration defining the 3 levels of gambit permissions:
 *    Unregistered denotes a client user who does not have a linked gambit user
 *    Registered denotes a linked gambit user without admin privileges
 *    Administrator denotes a linked gambit user with admin privileges
 */
object PermissionLevel extends Enumeration {
  type Level = Value
  val Unregistered, Registered, Administrator = Value
}
