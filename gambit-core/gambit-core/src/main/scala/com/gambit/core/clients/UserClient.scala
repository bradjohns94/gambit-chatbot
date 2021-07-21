package com.gambit.core.clients

import scala.concurrent.Future

/** User
 *  Type objects which represents the user for a given client
 */
case class User (userId: String, gambitUserId: Option[Int])

/** User Client
 *  Generic trait to encompass shared functionality for Chat clients
 *  (e.g. slack, hangouts, etc.)
 */
trait UserClient extends Client {

  /** Get Gambit User ID
   *  Resolve the client-specific user ID to a generic gambit user ID
   *  if the client user is linked to a gambit user
   *  @param clientUserId the ID of the user specific to the client
   *  @return a gambit User ID if one can be resolved
   */
  def getGambitUserId(clientUserId: String): Future[Option[Int]]

  /** Get Unlinked Users
   *  List all users that do not have an associated gambit user for the client
   *  @return a future sequence of all unlinked users
   */
  def getUnlinkedUsers: Future[Seq[User]]

  /** Set Gambit User
   *  Given the ID of a client user and the ID of a gambit user, associate the client user with
   *  the provided gambit user
   *  @param clientId the String ID of the client
   *  @param userId the Gambit user ID
   *  @return the updated user if successful
   */
  def setGambitUser(clientId: String, userId: Int): Future[Option[User]]
}
