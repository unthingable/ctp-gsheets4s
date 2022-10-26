package gsheets4s

import cats.effect.{Concurrent, Ref}
import gsheets4s.algebras._
import gsheets4s.http._
import gsheets4s.interpreters._
import gsheets4s.model._
import org.http4s.client.Client

case class GSheets4s[F[_]](
  spreadsheetsValues: SpreadsheetsValues[F]
)

object GSheets4s {
  def apply[F[_]: Concurrent](creds: Ref[F, Credentials], http4sClient: Client[F]): GSheets4s[F] = {
    val requester = new Http4sRequester[F](http4sClient)
    val client = new HttpClient[F](creds, requester)
    val spreadsheetsValues = new RestSpreadsheetsValues(client)
    GSheets4s(spreadsheetsValues)
  }
}
