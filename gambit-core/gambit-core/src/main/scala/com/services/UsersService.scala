package com.gambit.core.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.gambit.core.clients.{GambitUser, GambitUserClient, User, UserClient}

/** Users Service
 *  Utility functions for helping convert and register between various client users
 *  and gambit users
 */
object UsersService {
  def getGambitUserFromClient(
    clientUserId: String,
    userClient: UserClient,
    gambitClient: GambitUserClient
  ): Future[Option[GambitUser]] = userClient.getGambitUserId(clientUserId).flatMap{ _ match {
    case Some(gambitUserId) => gambitClient.getGambitUser(gambitUserId)
    case None => Future(None)
  }}

  def linkUserFromNickname(
    clientUserId: String,
    nickname: String,
    userClient: UserClient,
    gambitUserClient: GambitUserClient
  ): Future[Option[User]] = gambitUserClient.getGambitUserByNickname(nickname).flatMap{ _ match {
    case Some(user) => userClient.setGambitUser(clientUserId, user.userId)
    case None => Future(None)
  }}

  def registerUnlinkedUsers(
    userClient: UserClient,
    gambitUserClient: GambitUserClient
  ): Future[Int] = userClient.getUnlinkedUsers.flatMap{ users => Future.sequence(
    users.map{ user => createAndRegisterUser(user.userId, userClient, gambitUserClient) }
  )}.map{ _.flatten.length }

  private def createAndRegisterUser(
    clientUserId: String,
    userClient: UserClient,
    gambitUserClient: GambitUserClient
  ): Future[Option[User]] = gambitUserClient.createGambitUser(clientUserId).flatMap{ _ match {
    case Some(gambitUser) => userClient.setGambitUser(clientUserId, gambitUser.userId)
    case None => Future(None)
  }}
}