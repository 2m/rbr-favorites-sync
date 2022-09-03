package lt.dvim.rbr

import io.circe.generic.auto._
import cats.data.EitherT
import cats.instances.list._
import cats.syntax.traverse._
import cats.instances.future._
import cats.implicits._

import sttp.client3._
import sttp.client3.circe._
import sttp.client3.logging.scribe.ScribeLoggingBackend
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import io.circe.Decoder

type Http = SttpBackend[Future, Any]
case class Config(token: String)

object Notion:
  def authRequest(using Config) =
    basicRequest.auth.bearer(summon[Config].token).header("Notion-Version", "2022-06-28")

  def uri = uri"https://api.notion.com/v1"

  object Database:
    case class Response(results: List[Result])
    case class Result(id: String, properties: Map[String, Property])
    case class Property(id: String)
    def request(id: String)(using Config) =
      authRequest
        .post(uri.addPath("databases", id, "query"))
        .response(asJson[Response])

  object Page:
    case class Number(number: Int)

    case class Title(results: List[TitleResult])
    case class TitleResult(title: Text)
    case class Text(plain_text: String)

    case class MultiSelect(multi_select: List[Select])
    case class Select(name: String)

    def property[R: Decoder](id: String, page: String)(using Config) =
      authRequest
        .get(uri.addPath("pages", page, "properties", id))
        .response(asJson[R])

case class Stage(title: String, id: Int, tags: List[String])

def stages(token: String, database: String)(using Config, ExecutionContext, Http) =
  (for {
    db <- EitherT(Notion.Database.request(database).send().map(_.body))
    stages <- db.results.map { result =>
      val idResult = EitherT(Notion.Page.property[Notion.Page.Number]("ID", result.id).send().map(_.body))
      val nameResult = EitherT(Notion.Page.property[Notion.Page.Title]("Name", result.id).send().map(_.body))
      val tagsResult = EitherT(Notion.Page.property[Notion.Page.MultiSelect]("Tags", result.id).send().map(_.body))
      for {
        id <- idResult
        name <- nameResult
        tags <- tagsResult
      } yield Stage(name.results.head.title.plain_text, id.number, tags.multi_select.map(_.name))
    }.sequence
  } yield {
    stages
  }).value

def tags(token: String, database: String)(using ExecutionContext) =
  given Config(token)
  given Http = ScribeLoggingBackend(HttpClientFutureBackend())

  EitherT(stages(token, database)).map { stages =>
    stages.flatMap(_.tags).toSet.toList.sorted
  }.value

@main def printStages(token: String, database: String) =
  given Config(token)
  given ExecutionContext = ExecutionContext.global
  given Http = ScribeLoggingBackend(HttpClientFutureBackend())

  println(Await.result(stages(token, database), 30.seconds))
