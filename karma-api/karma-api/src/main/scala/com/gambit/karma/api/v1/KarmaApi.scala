package com.gambit.karma.api.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.effect.IO
import com.typesafe.scalalogging.Logger
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.finch._
import io.finch.catsEffect._
import io.finch.circe._

import com.gambit.karma.api.GambitEndpoint
import com.gambit.karma.models.{Karma, KarmaReference}


/** Karma Response Object
 *  API response object corresponding to a row in the karma table.
 *  @param name the string identifier of the karma row
 *  @param value the integer value of the karma row
 *  @param linkedUser the user ID linked to the karma value if any
 *  @param createdAt the timestamp of creation of the row if it exists.
 *  @param updatedAt the timestamp of the last update on the row if it exists.
 */
case class KarmaResponse(
  name: String,
  value: Int,
  linkedUser: Option[Int],
  createdAt: Option[String],
  updatedAt: Option[String]
)

/** Karma API
 *  Class containing reference functions on the karma database table.
 *  @param table a reference object to the karma table
 */
class KarmaApi(table: KarmaReference) extends GambitEndpoint {
  val logger = Logger("KarmaApi")

  val basePath = path("v1") :: path("karma")

  implicit val decoder: Decoder[KarmaResponse] = deriveDecoder[KarmaResponse]
  implicit val encoder: Encoder[KarmaResponse] = deriveEncoder[KarmaResponse]

  /** Get Karma
   *  Endpoint to fetch karma from the postgres database and return it as a
   *  jsonified karma object. If the object is not found in the database return
   *  an empty object with a karma value of 0.
   *  @param name the name of the karma value to lookup
   *  @return a response object containing a karma object
   */
  def getKarma: Endpoint[IO, KarmaResponse] = get(basePath :: path[String]) { name: String =>
    table.getKarmaByName(name).map{ maybeKarma =>
      maybeKarma match {
        case Some(karma) => Ok(translateKarma(karma))
        case None => Ok(KarmaResponse(name, 0, None, None, None))
      }
    }
  }

  /** Get Karma For User
   *  Endpoint to fetch karma from the postgres database and return it as a
   *  jsonified list of karma objects for any karma entries linked to the
   *  provided user ID.
   *  @param id the ID of the user to get linked karma for
   *  @return a list of karma objects linked to the user ID
   */
  def getKarmaForUser: Endpoint[IO, Seq[KarmaResponse]] =
    get(basePath :: path("user") :: path[Int]) { getKarmaForUserAction _ }

  /** Update Karma
   *  Endpoint to increment/decerement karma values from a provided json mapping
   *  of {name -> change}.
   *  @param changeMap a mapping of {name -> increment}
   *  @return a sequence of karma objects with the updated karma values
   */
  def updateKarma: Endpoint[IO, Seq[KarmaResponse]] =
    post(basePath :: jsonBody[Map[String, Int]]) { updateKarmaAction _ }

  /** Get Karma For User Action
   *  Action function for the get karma for user endpoint.
   *  @param userId the ID of the user to get linked karma for
   *  @return a list of karma objects linked to the user ID
   */
  private def getKarmaForUserAction(userId: Int): Future[Output[Seq[KarmaResponse]]] =
    table.getUserLinkedKarma(userId).map{ karma => Ok(karma.map{ translateKarma _ }) }

  /** Update Karma Action
   *  Action function for the update karma endpoint.
   *  @param changeMap a mapping of {name -> increment}
   *  @return a sequence of karma objects with the updated karma values
   */
  private def updateKarmaAction(changeMap: Map[String, Int]): Future[Output[Seq[KarmaResponse]]] =
    table.incrementKarma(changeMap).map{ updates => Ok(updates.map{ translateKarma _ })}

  /** Translate Karma
   *  Convert a database karma object into a response object from the API.
   *  @param karma the karma object from the database table
   *  @return a response karma object
   */
  private def translateKarma(row: Karma): KarmaResponse = new KarmaResponse(
    row.name,
    row.value.getOrElse(0),
    row.linkedUser,
    row.createdAt.map{_.toString},
    row.updatedAt.map{_.toString}
  )

  val endpoints = (getKarma :+: getKarmaForUser :+: updateKarma)
}
