package org.kaloz.mrd

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.data.Validated._
import shapeless.HNil
import shapeless.syntax.singleton._
import shapeless.syntax.std.tuple._

import scala.util.{Failure, Success, Try}

package object mrdshapeless {

  trait Field[A] {

    def length: Int

    def convert(input: String): Validation[A]

    protected def validateChecksum(text: String, checksum: Char): Validation[String] = {

      def mapChar(input: Char): Int = {
        input match {
          case x if x >= 'A' && x <= 'Z' => x - 'A' + 10
          case x if x >= '0' && x <= '9' => x - '0'
          case ' ' => 0
        }
      }

      val generatedChecksum = text.
        map(mapChar).
        zip(Stream.continually(Seq(7, 3, 1).toStream).flatten).
        map(x => x._1 * x._2).
        sum % 10

      if (generatedChecksum == mapChar(checksum)) {
        valid(text)
      } else {
        invalidNel(ValidationError("Checksum", s"Invalid checksum '$checksum' for '$text'"))
      }
    }
  }

  case class StringField(length: Int, checksum: Boolean = false) extends Field[String] {

    def convert(input: String): Validation[String] = {
      val textWithChecksum = input.replaceAll("<", " ")
      if (checksum) {
        val result = textWithChecksum.substring(0, textWithChecksum.length - 1)
        val checksum = textWithChecksum.substring(textWithChecksum.length - 1).charAt(0)
        validateChecksum(result, checksum).map(_.trim)
      } else {
        valid(textWithChecksum.trim)
      }
    }
  }

  case object DateField extends Field[LocalDate] {

    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd")

    val length: Int = 7

    def convert(input: String): Validation[LocalDate] =
      validateChecksum(input.substring(0, 6), input(6)).andThen { s =>
        Try(LocalDate.parse(s, dateTimeFormatter)) match {
          case Success(date) => valid(if (date.getYear > 2050) date.minusYears(100) else date)
          case Failure(error) => invalidNel(ValidationError("datefield", error.getMessage))
        }
      }
  }

  case object SexField extends Field[Sex] {

    val length: Int = 1

    def convert(input: String): Validation[Sex] = Sex(input)
  }

  val passportFormat =
    'documentType ->> StringField(1) ::
      'documentSubtype ->> StringField(1) ::
      'country ->> StringField(3) ::
      'surname ->> StringField(39) ::
      'passportNumber ->> StringField(10, checksum = true) ::
      'nationality ->> StringField(3) ::
      'dateOfBirth ->> DateField ::
      'sex ->> SexField ::
      'expirationDate ->> DateField ::
      'personalNumber ->> StringField(15, checksum = true) ::
      'checksum ->> StringField(1) :: HNil

  val identityCardFormat =
    'documentType ->> StringField(1) ::
      'documentSubtype ->> StringField(1) ::
      'country ->> StringField(3) ::
      'documentNumber ->> StringField(10, checksum = true) ::
      'reserved1 ->> StringField(15) ::
      'dateOfBirth ->> DateField ::
      'sex ->> SexField ::
      'expirationDate ->> DateField ::
      'nationality ->> StringField(3) ::
      'reserved2 ->> StringField(11) ::
      'checksum ->> StringField(1) ::
      'surname ->> StringField(30) :: HNil

}
