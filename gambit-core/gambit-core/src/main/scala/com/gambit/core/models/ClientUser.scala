package com.gambit.core.models

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import slick.jdbc.PostgresProfile.api._

import com.gambit.core.common.PermissionLevel

/** Client User
 *  Common trait to define any table-describing case classes for what is needed
 *  to be a client user
 */
trait ClientUser {
  val gambitUserId: Option[Int]
  val createdAt: Option[Timestamp]
  val updatedAt: Option[Timestamp]
}


/** Client Reference
 *  Trait for any class that holds utility functions to interact with a client
 *  model to help resolve messages into a single common format for command
 *  parsing.
 *  @param a database to connect to
 */
abstract trait ClientReference {
  val db: Database
  private val gambitUsers = TableQuery[GambitUsers]

  /** Get User By ID
   *  Fetch the client user object by its respective client ID
   *  @param clientId the ID used to reference the user if they exist
   *  @return a distinct client user
   */
  def getUserById(clientId: String): Future[Option[ClientUser]]

  /** Get Permission Level
   *  Given a reference to any Client User, use the associated gambit user ID
   *  to determine the permission level of the client user
   *  @param user a reference to a ClientUser
   *  @return the permission level of the client user
   */
   def getPermissionLevel(user: ClientUser): Future[PermissionLevel.Value] =
    user.gambitUserId match {
      case None => Future(PermissionLevel.Unregistered)
      case Some(userId) => {
        db.run(
          gambitUsers.filter{_.id === userId}.map{_.isAdmin}.result.map{
            _.headOption match {
              case Some(true) => PermissionLevel.Administrator
              case _ => PermissionLevel.Registered
            }
          }
        )
      }
    }
}
