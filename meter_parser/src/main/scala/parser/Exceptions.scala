package parser

sealed abstract class ParserException(message: String) extends Exception(message)
case class GeneralParserException(message: String) extends ParserException(message)
case class InvalidFormatException(message: String) extends ParserException(message)
case class InvalidDataException(message: String) extends ParserException(message)
