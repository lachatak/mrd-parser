package org.kaloz.mrd

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.data.Validated._
import scodec.bits._
import scodec.codecs._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}

import scala.util.{Failure, Success, Try}

package object mrdscodec {

  case object LocalDateCodec extends Codec[LocalDate] {

    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd")

    val localDateCodec = fixedSizeBytes(6, ascii)

    override def sizeBound: SizeBound = SizeBound.exact(48)

    override def encode(value: LocalDate): Attempt[BitVector] =
      localDateCodec.encode(value.getYear.toString + value.getMonthValue.toString + value.getDayOfMonth.toString)

    override def decode(bits: BitVector): Attempt[DecodeResult[LocalDate]] = {
      val decodedResult = localDateCodec.decode(bits)
      val result = decodedResult.flatMap { res =>
        Try(LocalDate.parse(res.value, dateTimeFormatter)) match {
          case Success(locatDate) => Attempt.successful(DecodeResult(if (locatDate.getYear > 2050) locatDate.minusYears(100) else locatDate, res.remainder))
          case Failure(error) => Attempt.failure(Err(error.getMessage))
        }
      }
      result
    }
  }

  case object SexCodec extends Codec[Sex] {

    val sexCodec = fixedSizeBytes(1, ascii)

    override def sizeBound: SizeBound = SizeBound.exact(8)

    override def encode(sex: Sex): Attempt[BitVector] = sexCodec.encode(Sex.unapply(sex).get)

    override def decode(bits: BitVector): Attempt[DecodeResult[Sex]] = {
      val decodedResult = sexCodec.decode(bits)
      val result = decodedResult.flatMap { res =>
        Sex(res.value) match {
          case Valid(x) => Attempt.successful(DecodeResult(x, res.remainder))
          case _ => Attempt.failure(Err(s"Invalid data '${res.value}' for sex"))
        }
      }
      result
    }
  }

  case class StringCodec(length: Int) extends Codec[String] {

    val stringCodec = paddedFixedSizeBytes(length, ascii, constant(ByteVector("<".getBytes)))

    override def sizeBound: SizeBound = SizeBound.exact(8 * length)

    override def encode(text: String): Attempt[BitVector] = stringCodec.encode(text.replace(" ", "<"))

    override def decode(bits: BitVector): Attempt[DecodeResult[String]] = {
      val decodedResult = stringCodec.decode(bits)
      val result = decodedResult.map { res =>
        DecodeResult(res.value.replaceAll("<", " ").trim, res.remainder)
      }
      result
    }
  }

  val checkSumFunction: BitVector => BitVector = input => {

    val valueDecoder = ascii

    val checkSum = for {
      decodedValue <- valueDecoder.decode(input)
      calculatedValue = CheckSum.checksum(decodedValue.value)
      checkSum <- valueDecoder.encode(calculatedValue.toString)
    } yield checkSum

    checkSum.require
  }

  val passportMetadataCodec: Codec[PassportMetadata] = (
    ("documentType" | StringCodec(1)) ::
      ("documentSubtype" | StringCodec(1)) ::
      ("country" | StringCodec(3)) ::
      ("surname" | StringCodec(39)) ::
      ("passportNumber" | checksummed(StringCodec(9), checkSumFunction, bits(72) ~ bits(8))) ::
      ("nationality" | StringCodec(3)) ::
      ("dateOfBirth" | checksummed(LocalDateCodec, checkSumFunction, bits(48) ~ bits(8))) ::
      ("sex" | SexCodec) ::
      ("expirationDate" | checksummed(LocalDateCodec, checkSumFunction, bits(48) ~ bits(8))) ::
      ("personalNumber" | checksummed(StringCodec(14), checkSumFunction, bits(112) ~ bits(8))) ::
      ("checksum" | StringCodec(1))
    ).as[PassportMetadata]

  val identityCardMetadataCodec: Codec[IdentityCardMetadata] = (
    ("documentType" | StringCodec(1)) ::
      ("documentSubtype" | StringCodec(1)) ::
      ("country" | StringCodec(3)) ::
      ("documentNumber" | checksummed(StringCodec(9), checkSumFunction, bits(72) ~ bits(8))) ::
      ("ignore1" | ignore(15 * 8)) ::
      ("dateOfBirth" | checksummed(LocalDateCodec, checkSumFunction, bits(48) ~ bits(8))) ::
      ("sex" | SexCodec) ::
      ("expirationDate" | checksummed(LocalDateCodec, checkSumFunction, bits(48) ~ bits(8))) ::
      ("nationality" | StringCodec(3)) ::
      ("ignore2" | ignore(11 * 8)) ::
      ("checksum" | StringCodec(1)) ::
      ("surname" | StringCodec(30))
    ).dropUnits.as[IdentityCardMetadata]

}
