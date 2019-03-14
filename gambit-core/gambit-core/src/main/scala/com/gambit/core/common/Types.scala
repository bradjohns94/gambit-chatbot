package com.gambit.core.common

import com.gambit.core.models.GambitUser

/** Core Message
 *  A subset of fields from api.MessageAPI.ClientMessage that are used by the
 *  commands run through MessageEngine to produce a response from the bot
 *  @param userId the identifier of the user who sent the message
 *  @param username the username who sent the initial message
 *  @param messageText the actual text body of the message
 *  @param client a string identifying the requesting client
 *  @param gambitUser a gambit user that may be associated with the requesting user
 */
final case class CoreMessage(
  userId: String,
  username: String,
  channel: String,
  messageText: String,
  client: String,
  gambitUser: Option[GambitUser]
)

/** Core Response
 *  The response object returned by any commands that successfully parse the
 *  recieved message. This may or may not need to be wrapped by other
 *  client-specific information by the messaging API
 *  @param messageText the text of the response
 */
final case class CoreResponse(
  messageText: String,
  channel: String
)

/** ClientMessage
 *  The expected input type for the message API to come from a client
 *  @param username the username who sent the initial message
 *  @param messageText the actual string of text sent in the message
 */
final case class ClientMessage(
  userId: String,
  username: String,
  channel: String,
  messageText: String,
  client: String
)

/** ClientMessageResponse
 *  The expected output type for the message API
 *  @param messages a sequence of response messages
 */
final case class ClientMessageResponse(messages: Seq[CoreResponse])
