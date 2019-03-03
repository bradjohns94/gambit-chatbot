package com.gambit.core.bot.commands.common

import scala.util.matching.Regex

/** Common constant values for karma calculation */
object KarmaConstants {
  private val nonKarmaChars = Seq(
    "\\s", // No spaces outside of paren karma text
    "\\\"", // No quotes
    "\\'", // No single quotes
    "\\`", // No backticks
    "\\+", // No +'s or -'s
    "\\-", // No +'s or -'s
    "\\=", // No ='s
    "\\(", // No parens
    "\\)", // No parens
    "\\[", // No brackets
    "\\]", // No brackets
    "\\<", // No brackets
    "\\>" // No brackets
  )
  private val karmaWordPattern = "[^%s]+".format(nonKarmaChars.mkString(""))
  final val karmaRegex = """((?:%s)|\((?:%s )*(?:%s)\))""".format(
    karmaWordPattern, karmaWordPattern, karmaWordPattern)
}
