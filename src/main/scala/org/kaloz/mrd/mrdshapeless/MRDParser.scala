package org.kaloz.mrd.mrdshapeless

import java.time.LocalDate

import cats.data.Validated._
import cats.implicits._
import org.kaloz.mrd.{Sex, Validation, ValidationError}
import shapeless.labelled._
import shapeless.ops.hlist
import shapeless.syntax.std.tuple._
import shapeless.{HNil, LabelledGeneric, Witness, _}

trait Parser[F] {
  type R

  def parse(input: String, f: F): (Validation[R], String)
}

object Parser {
  type Aux[F0, R0] = Parser[F0] {type R = R0}

  def apply[F0, R0](implicit parser: Parser.Aux[F0, R0]) = parser

  def createParser[F0 <: Field[R0], R0]: Aux[F0, R0] = new Parser[F0] {
    type R = R0

    def parse(input: String, field: F0): (Validation[R], String) = {
      val (value, rest) = input.splitAt(field.length)
      (field.convert(value), rest)
    }
  }

  implicit val stringFieldParser = createParser[StringField, String]

  implicit val dateFieldParser = createParser[DateField.type, LocalDate]

  implicit val sexFieldParser = createParser[SexField.type, Sex]

  implicit val hnilParser = new Parser[HNil] {
    type R = HNil

    def parse(input: String, field: HNil): (Validation[R], String) = {
      if (input.length == 0) {
        (valid(HNil), "")
      } else {
        (invalidNel(ValidationError("EOF", s"Parsable string has not finished. Still has '$input'")), input)
      }
    }
  }

  implicit def hlistParser[K <: Symbol, H <: Field[HR], L <: HList, HR, LR <: HList](implicit witness: Witness.Aux[K],
                                                                                     head: Lazy[Parser.Aux[H, HR]],
                                                                                     tail: Parser.Aux[L, LR]): Aux[FieldType[K, H] :: L, FieldType[K, HR] :: LR] = new Parser[FieldType[K, H] :: L] {
    type R = FieldType[K, HR] :: LR

    def parse(input: String, a: FieldType[K, H] :: L): (Validation[R], String) = {
      val (headValue, headRest) = head.value.parse(input, a.head)
      val (tailValue, rest) = tail.parse(headRest, a.tail)
      ((headValue.leftMap(error => error.map(_.copy(fieldName = witness.value.name))) |@| tailValue).map { case (h, t) => field[K](h) :: t }, rest)
    }
  }

  implicit def genericParser[T, PRepr <: HList, Result <: HList, TRepr <: HList, Common <: HList](implicit parser: Parser.Aux[PRepr, Result],
                                                                                                  targetLabel: LabelledGeneric.Aux[T, TRepr],
                                                                                                  inter: hlist.Intersection.Aux[Result, TRepr, Common],
                                                                                                  align: hlist.Align[Common, TRepr]): Aux[PRepr, T] = new Parser[PRepr] {
    type R = T

    override def parse(input: String, p: PRepr): (Validation[R], String) = {
      val (validation, rest) = parser.parse(input.filterNot(_.isWhitespace), p)
      (validation.map(r => targetLabel.from(align(inter(r)))), rest)
    }
  }
}

object StringOps {

  implicit class StringParser(input: String) {
    def parseTo[T] = new {
      def apply[PRepr <: HList](parserDefinition: PRepr)(implicit parser: Parser.Aux[PRepr, T]): Validation[T] = {
        parser.parse(input, parserDefinition).head
      }
    }
  }
}