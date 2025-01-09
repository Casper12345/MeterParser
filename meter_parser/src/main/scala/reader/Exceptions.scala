package reader

sealed abstract class FileReaderException(message: String) extends Exception(message)
case class GeneralFileReaderException(message: String) extends FileReaderException(message)
case class InvalidOrderException(message: String) extends FileReaderException(message)
