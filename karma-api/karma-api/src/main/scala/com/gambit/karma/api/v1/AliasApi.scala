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
import com.gambit.karma.models.{Alias, AliasReference}

/** Alias Response Object
 *  API response object corresponding to a row in the alias table.
 *  @param primaryName the unique source name of the alias
 *  @param aliasedName the name for the source name to be redirected to
 *  @param createdAt the timestamp of creation of the row if it exists.
 *  @param updatedAt the timestamp of the last update on the row if it exists.
 */
case class AliasResponse(
  primaryName: String,
  aliasedName: String,
  createdAt: Option[String],
  updatedAt: Option[String]
)

/** Alias API
 *  Class containing reference functions to the alias database table.
 *  @param table a reference object to the alias table
 */
class AliasApi(table: AliasReference) extends GambitEndpoint {
  val logger = Logger("AliasApi")

  val basePath = path("v1") :: path("aliases")

  implicit val decoder: Decoder[AliasResponse] = deriveDecoder[AliasResponse]
  implicit val encoder: Encoder[AliasResponse] = deriveEncoder[AliasResponse]


  /** Get Primary name
   *  API Endpoint to fetch an Alias based on its unique aliased name.
   *  @param name the aliased name to fetch the primary name for
   *  @return an AliasResponse endpoint object
   */
  def getPrimaryName: Endpoint[IO, AliasResponse] =
    get(basePath :: path[String]) { getPrimaryNameFromAliasAction _ }


  /** Get Primary Name From Alias Action
   *  Helper function to derive the primary name from an aliased name
   *  @param aliasedName the aliased name to fetch the primary name for
   *  @return an API output wrapping an alias response
   */
  private def getPrimaryNameFromAliasAction(aliasedName: String): Future[Output[AliasResponse]] =
    table.getPrimaryName(aliasedName).map{ futureAlias =>
      futureAlias match {
        case Some(alias) => Ok(translateAlias(alias))
        case None => Ok(AliasResponse(aliasedName, aliasedName, None, None))
      }
    }


  /** Translate Alias
   *  Translate a raw Alias database object into an API response object
   *  @param row the row object from the alias database
   *  @return a AliasResponse object derived from the row
   */
  private def translateAlias(row: Alias): AliasResponse = new AliasResponse(
    row.primaryName,
    row.aliasedName,
    row.createdAt.map{_.toString},
    row.updatedAt.map{_.toString}
  )

  val endpoints = (getPrimaryName)
}
