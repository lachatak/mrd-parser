package org.kaloz

import java.time.LocalDate

import cats.data.Validated.{invalidNel, valid}
import cats.data.ValidatedNel

package object mrd {

  case class ValidationError(fieldName: String, error: String)

  type Validation[R] = ValidatedNel[ValidationError, R]

  sealed trait Sex

  object Sex {
    def apply(input: String): Validation[Sex] = input match {
      case "M" => valid(Male)
      case "F" => valid(Female)
      case x => invalidNel(ValidationError("Sex", s"Invalid data '$x' for sex"))
    }

    def unapply(sex: Sex): Option[String] = sex match {
      case Male => Some("M")
      case Female => Some("F")
    }
  }

  case object Male extends Sex

  case object Female extends Sex

  case class PassportMetadata(documentType: String,
                              documentSubtype: String,
                              country: String,
                              surname: String,
                              passportNumber: String,
                              nationality: String,
                              dateOfBirth: LocalDate,
                              sex: Sex,
                              expirationDate: LocalDate,
                              personalNumber: String,
                              checksum: String)

  case class IdentityCardMetadata(documentType: String,
                                  documentSubtype: String,
                                  country: String,
                                  documentNumber: String,
                                  dateOfBirth: LocalDate,
                                  sex: Sex,
                                  expirationDate: LocalDate,
                                  nationality: String,
                                  checksum: String,
                                  surname: String)

  object CheckSum {

    def mapChar(input: Char): Int = {
      input match {
        case x if x >= 'A' && x <= 'Z' => x - 'A' + 10
        case x if x >= '0' && x <= '9' => x - '0'
        case x if x == '<' || x == ' ' => 0
        case _ => 0
      }
    }

    def checksum(text: String): Int = text
      .map(mapChar)
      .zip(Stream.continually(Seq(7, 3, 1).toStream).flatten)
      .collect { case (x, y) => x * y }
      .sum % 10

  }

}
