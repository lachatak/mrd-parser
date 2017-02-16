package org.kaloz.mrd.mrdscodec

import java.time.LocalDate

import org.kaloz.mrd.{IdentityCardMetadata, Male, PassportMetadata}
import org.scalatest.{FunSuite, GivenWhenThen, Matchers}
import scodec.bits.{BitVector, _}
import scodec.codecs.ChecksumMismatch
import scodec.{Attempt, Err}

class MRDParserTest extends FunSuite with Matchers with GivenWhenThen {

  test("It should be possible to parse valid passport") {

    Given("we have valid passport data")

    val sampleData =
      """P<HUNLACHATA<<KRISZTIAN<<<<<<<<<<<<<<<<<<<<<
        |HG45654029HUN7906075M2508201<<<<<<<<<<<<<<04""".stripMargin

    When("we parse valid the data")

    val parsedData = passportMetadataCodec.decode(BitVector(sampleData.filterNot(_.isWhitespace).getBytes()))

    Then("we should get proper content")

    parsedData.require.value should be(PassportMetadata("P", "", "HUN", "LACHATA  KRISZTIAN", "HG4565402", "HUN", LocalDate
      .parse("1979-06-07"), Male, LocalDate.parse("2025-08-20"), "", "4"))

  }

  test("It should be possible to parse valid identity card") {

    Given("we have valid identity card data")

    val sampleData =
      """I<HUF592492SB<0<<<<<<<<<<<<<<<
        |7906075M2405296HUF<<<<<<<<<<<4
        |LACHATA<<KRISZTIAN<<<<<<<<<<<<""".stripMargin

    When("we parse valid data")

    val parsedData = identityCardMetadataCodec.decode(BitVector(sampleData.filterNot(_.isWhitespace).getBytes()))

    Then("we should get proper content")

    parsedData.require.value should be(IdentityCardMetadata("I", "", "HUF", "592492SB", LocalDate
      .parse("1979-06-07"), Male, LocalDate.parse("2024-05-29"), "HUF", "4", "LACHATA  KRISZTIAN"))
  }

    test("Parser should fail if some of field checksums are not correct") {
      Given("we have a passport data with invalid dob checksum")

      val sampleData =
        """P<HUNLACHATA<<KRISZTIAN<<<<<<<<<<<<<<<<<<<<<
          |HG45654029HUN7906078M2508201<<<<<<<<<<<<<<04""".stripMargin

      When("we parse valid the data")

      val parsedData = passportMetadataCodec.decode(BitVector(sampleData.filterNot(_.isWhitespace).getBytes()))

      Then("we should get proper list of errors")

      parsedData shouldBe Attempt.failure(ChecksumMismatch(hex"0x373930363037".bits, hex"0x35".bits, hex"0x38".bits).pushContext("dateOfBirth"))
    }

    test("Parser should fail for invalid dates") {
      Given("we have passport data with invalid dob date")

      val sampleData =
        """P<HUNLACHATA<<KRISZTIAN<<<<<<<<<<<<<<<<<<<<<
          |HG45654029HUN79060X1M2508201<<<<<<<<<<<<<<04""".stripMargin

      When("we parse valid the data")

      val parsedData = passportMetadataCodec.decode(BitVector(sampleData.filterNot(_.isWhitespace).getBytes()))

      Then("we should get a notification about invalid date")

      parsedData shouldBe Attempt.failure(Err("Text '79060X' could not be parsed at index 4").pushContext("dateOfBirth"))

    }

    test("Parser should fail for invalid values for sex field") {
      Given("we have passport data with invalid sex")

      val sampleData =
        """P<HUNLACHATA<<KRISZTIAN<<<<<<<<<<<<<<<<<<<<<
          |HG45654029HUN7906075X2508201<<<<<<<<<<<<<<04""".stripMargin

      When("we parse valid the data")

      val parsedData =  passportMetadataCodec.decode(BitVector(sampleData.filterNot(_.isWhitespace).getBytes()))

      Then("we should get a notification about invalid sex")

      parsedData shouldBe Attempt.failure(Err("Invalid data 'X' for sex").pushContext("sex"))

    }

}