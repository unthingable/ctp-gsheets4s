package gsheets4s
package http

import cats.Monad
import cats.effect.{Concurrent, Ref}
import cats.syntax.flatMap._
import cats.syntax.functor._
import gsheets4s.model.{Credentials, GsheetsError}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}

trait HttpRequester[F[_]] {
  def request[O](uri: Uri, method: Method)(implicit d: Decoder[O]): F[O]
  def requestWithBody[I, O](
    uri: Uri, body: I, method: Method)(implicit e: Encoder[I], d: Decoder[O]): F[O]
}

class Http4sRequester[F[_]: Concurrent](client: Client[F]) extends HttpRequester[F] {

  def request[O](uri: Uri, method: Method)(implicit d: Decoder[O]): F[O] = {
    client.expect[O](Request[F](method, uri))
  }

  def requestWithBody[I, O](
    uri: Uri, body: I, method: Method)(implicit e: Encoder[I], d: Decoder[O]): F[O] = {
      client.expect[O](Request[F](method, uri).withEntity(body))
    }
}

class HttpClient[F[_]: Monad](creds: Ref[F, Credentials], requester: HttpRequester[F])(
    implicit urls: GSheets4sDefaultUrls) {
  def get[O](
    path: Uri,
    params: List[(String, String)] = List.empty)(
    implicit d: Decoder[O]): F[Either[GsheetsError, O]] =
      req(token => requester
        .request[Either[GsheetsError, O]](urlBuilder(token, path, params), Method.GET))

  def put[I, O](
    path: Uri,
    body: I,
    params: List[(String, String)] = List.empty)(
    implicit e: Encoder[I], d: Decoder[O]): F[Either[GsheetsError, O]] =
      req(token => requester.requestWithBody[I, Either[GsheetsError, O]](
        urlBuilder(token, path, params), body, Method.PUT))

  def post[I, O](
    path: Uri,
    body: I,
    params: List[(String, String)] = List.empty)(
    implicit e: Encoder[I], d: Decoder[O]): F[Either[GsheetsError, O]] =
      req(token => requester.requestWithBody[I, Either[GsheetsError, O]](
        urlBuilder(token, path, params), body, Method.POST))

  private def req[O](req: String => F[Either[GsheetsError, O]]): F[Either[GsheetsError, O]] = for {
    c <- creds.get
    first <- req(c.accessToken)
    retried <- first match {
      case Left(GsheetsError(401, _, _)) => reqWithNewToken(req, c)
      case o => Monad[F].pure(o)
    }
  } yield retried

  private def reqWithNewToken[O](
    req: String => F[Either[GsheetsError, O]], c: Credentials): F[Either[GsheetsError, O]] = for {
      newToken <- refreshToken(c)(Decoder.decodeString.prepare(_.downField("access_token")))
      _ <- creds.set(c.copy(accessToken = newToken))
      r <- req(newToken)
    } yield r

  private def refreshToken(c: Credentials)(implicit d: Decoder[String]): F[String] = {
    val url = urls.refreshTokenUrl.withQueryParam("refresh_token", c.refreshToken)
      .withQueryParam("client_id", c.clientId)
      .withQueryParam("client_secret", c.clientSecret)
      .withQueryParam("grant_type", "refresh_token")

    requester.request(url, Method.POST)
  }

  private def urlBuilder(
    accessToken: String,
    path: Uri,
    params: List[(String, String)]): Uri =
      urls.baseUrl
        .resolve(path)
        .withQueryParam("access_token", accessToken)
        .withQueryParams(params.toMap)
}
