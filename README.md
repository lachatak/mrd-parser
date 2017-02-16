#Custom Typesafe MRD parser
Typesafe parser of the data encoded on machine readable passport.

The format of data available on machine readable travel documents is
available on Wikipedia. (https://en.wikipedia.org/wiki/Machine-readable_passport)

The library allows to define different formats of documents encoded 
using ICAO 9303 standard (passports/identity cards/others) and to define
parsers that parse these documents into case classes in typesafe manner.

Sample code:
```scala
val sampleData =
      """P<HUNLACHATA<<KRISZTIAN<<<<<<<<<<<<<<<<<<<<<
        |HG45654029HUN7906075M2508201<<<<<<<<<<<<<<04""".stripMargin
    
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
    
println(sampleData.parseTo[PassportMetadata](passportFormat))
```    

#Custom Typesafe MRD parser vs Scodec MRD encoder
There are two MRD implementations in this project:
- One was implemented with pure Shapeless
- The other was implemented with Scodec

DETAILS HERE