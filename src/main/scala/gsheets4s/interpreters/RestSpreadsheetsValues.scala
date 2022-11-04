package gsheets4s
package interpreters

import cats.syntax.show._
import gsheets4s.algebras.SpreadsheetsValues
import gsheets4s.http._
import gsheets4s.model._
import io.circe.generic.auto._
import org.http4s.Uri

class RestSpreadsheetsValues[F[_]](client: HttpClient[F]) extends SpreadsheetsValues[F] {
  def get(spreadsheetID: String, range: A1Notation): F[Either[GsheetsError, ValueRange]] =
    client.get(s"$spreadsheetID/values/${range.show}")

  def update(
    spreadsheetID: String,
    range: A1Notation,
    updates: ValueRange,
    valueInputOption: ValueInputOption
  ): F[Either[GsheetsError, UpdateValuesResponse]] =
    client.put(s"$spreadsheetID/values/${range.show}", updates,
      List(("valueInputOption", valueInputOption.value)))
}
