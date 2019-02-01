package com.gambit.core.common

/** Common Types
 *  A common place to store any object types that are shared between the API
 *  and the engine core. We may want to break this into multiple files if it
 *  gets unruly
 */
object Types {
  /** Core Message
   *  A subset of fields from api.MessageAPI.ClientMessage that are used by the
   *  commands run through MessageEngine to produce a response from the bot
   *  @param username the username who sent the initial message
   *  @param messageText the actual text body of the message
   */
  case class CoreMessage(
    username: String,
    messageText: String
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
}
