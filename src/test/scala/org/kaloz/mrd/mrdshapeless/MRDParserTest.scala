package org.kaloz.mrd.mrdshapeless

import java.time.LocalDate

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import org.kaloz.mrd.mrdshapeless.StringOps._
import org.kaloz.mrd.{IdentityCardMetadata, Male, PassportMetadata, ValidationError}
import org.scalatest.{FunSuite, GivenWhenThen, Matchers}

class MRDParserTest extends FunSuite with Matchers with GivenWhenThen {

  test("It should be possible to parse valid passport") {

    Given("we have valid passport data")

    val sampleData =
      """P<HUNLACHATA<<KRISZTIAN<<<<<<<<<<<<<<<<<<<<<
        |HG45654029HUN7906075M2508201<<<<<<<<<<<<<<04""".stripMargin

    When("we parse valid the data")

    val parsedData = sampleData.parseTo[PassportMetadata](passportFormat)

    Then("we should get proper content")

    parsedData should be(Valid(PassportMetadata("P", "", "HUN", "LACHATA  KRISZTIAN", "HG4565402", "HUN", LocalDate
      .parse("1979-06-07"), Male, LocalDate.parse("2025-08-20"), "", "4")))

  }

  test("It should be possible to parse valid identity card") {

    Given("we have valid identity card data")

    val sampleData =
      """I<HUF592492SA<7<<<<<<<<<<<<<<<
        |7906075M2405296HUF<<<<<<<<<<<4
        |LACHATA<<KRISZTIAN<<<<<<<<<<<<""".stripMargin

    When("we parse valid data")

    val parsedData = sampleData.parseTo[IdentityCardMetadata](identityCardFormat)

    Then("we should get proper content")

    parsedData should be(Valid(IdentityCardMetadata("I", "", "HUF", "592492SA", LocalDate
      .parse("1979-06-07"), Male, LocalDate.parse("2024-05-29"), "HUF", "4", "LACHATA  KRISZTIAN")))
  }

  test("Parser should fail if some of field checksums are not correct") {
    Given("we have a passport data with invalid dob checksum")

    val sampleData =
      """P<HUNLACHATA<<KRISZTIAN<<<<<<<<<<<<<<<<<<<<<
        |HG45654029HUN7906078M2508201<<<<<<<<<<<<<<04""".stripMargin

    When("we parse valid the data")

    val parsedData = sampleData.parseTo[PassportMetadata](passportFormat)

    Then("we should get proper list of errors")

    parsedData should be(Invalid(NonEmptyList.of(ValidationError("dateOfBirth", "Invalid checksum '8' for '790607'"))))
  }

  test("Parser should fail for invalid dates") {
    Given("we have passport data with invalid dob date")

    val sampleData =
      """P<HUNLACHATA<<KRISZTIAN<<<<<<<<<<<<<<<<<<<<<
        |HG45654029HUN79060X1M2508201<<<<<<<<<<<<<<04""".stripMargin

    When("we parse valid the data")

    val parsedData = sampleData.parseTo[PassportMetadata](passportFormat)

    Then("we should get a notification about invalid date")

    parsedData should be(Invalid(NonEmptyList.of(ValidationError("dateOfBirth", "Text '79060X' could not be parsed at index 4"))))

  }

  test("Parser should fail for invalid values for sex field") {
    Given("we have passport data with invalid sex")

    val sampleData =
      """P<HUNLACHATA<<KRISZTIAN<<<<<<<<<<<<<<<<<<<<<
        |HG45654029HUN7906075X2508201<<<<<<<<<<<<<<04""".stripMargin

    When("we parse valid the data")

    val parsedData = sampleData.parseTo[PassportMetadata](passportFormat)

    Then("we should get a notification about invalid sex")

    parsedData should be(Invalid(NonEmptyList.of(ValidationError("sex", "Invalid data 'X' for sex"))))

  }

}