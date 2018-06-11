package utils.exception

case class DESInternalServerError(cause: Throwable) extends Exception(cause)
