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
}
