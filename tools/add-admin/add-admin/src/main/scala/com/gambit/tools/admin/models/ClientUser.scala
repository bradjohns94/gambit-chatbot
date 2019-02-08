package com.gambit.tools.admin.models

import java.sql.Timestamp

/** Client User
 *  Common trait to define any table-describing case classes for what is needed
 *  to be a client user
 */
trait ClientUser {
  val gambitUserId: Option[Int]
  val createdAt: Option[Timestamp]
  val updatedAt: Option[Timestamp]
}
