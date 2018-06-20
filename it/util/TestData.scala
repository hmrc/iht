package util

object TestData {

  def validGetCaseDetails(nino: String, reference: String): String = {
    s"""
       |{"deceased"                 : {
       | "dateOfDeath"             : "2018-05-20",
       | "firstName"               : "John",
       | "middleName"              : null,
       | "lastName"                : "Bagelson",
       | "nino"                    : "AA000000A",
       | "mainAddress"               : {
       |  "addressLine1"         : "21",
       |  "addressLine2"         : "Bagel Road",
       |  "addressLine3"         : null,
       |  "addressLine4"         : null,
       |  "postCode"               : "BA63LS",
       |  "countryCode"            : "UK"
       | },
       | "dateOfBirth"             : "1980-05-20",
       | "domicile"                : "UK",
       | "maritalStatus"           : "Single"
       |},
       |"leadExecutor"             : {
       | "firstName"               : "John",
       | "lastName"                : "Smith",
       | "nino"                    : "$nino",
       | "dateOfBirth"             : "1980-05-20",
       | "mainAddress"               : {
       |   "addressLine1"        : "21",
       |   "addressLine2"        : "Bagel Road",
       |   "addressLine3"        : null,
       |   "addressLine4"        : null,
       |   "postCode"              : "BA63LS",
       |   "countryCode"           : "UK"
       | },
       | "phoneNo"                 : null,
       | "country"                 : "England or Wales",
       | "role"                    : "Lead Executor"
       |},
       |"coExecutors"              : null,
       |"ihtReference"             : "$reference",
       |"caseStatus"               : "active",
       |"acknowledgmentReference"  : "AA000000BB111111",
       |"returns"                  : [{
       | "returnDate"              : "2016-05-14",
       | "returnId"                : "AAAABBBB",
       | "returnVersionNumber"     : "123456789",
       | "submitterRole"           : "Friend"
       | }]
       |}
          """.stripMargin
  }

  val successfulSubmissionResponse = {
    """
      |{"returnId" : "12"}
    """.stripMargin
  }

  val sumissionRequestBody = {
    """
      |{
      |  "submitter" : {
      |    "submitterRole" : "Lead Executor"
      |  },
      |  "deceased" : {
      |    "survivingSpouse" : {
      |      "firstName" : "ABCDE",
      |      "lastName" : "XYZAB",
      |      "dateOfBirth" : "2011-11-12",
      |      "dateOfMarriage" : "2018-05-19",
      |      "domicile" : "England or Wales"
      |    },
      |    "transferOfNilRateBand" : {
      |      "totalNilRateBandTransferred" : 100,
      |      "deceasedSpouses" : [ {
      |        "spouse" : {
      |          "firstName" : "ABCXYZ",
      |          "lastName" : "XYZABC",
      |          "dateOfBirth" : "1670-12-01",
      |          "dateOfMarriage" : "2008-12-13",
      |          "dateOfDeath" : "2010-10-12"
      |        },
      |        "spousesEstate" : {
      |          "domiciledInUk" : true,
      |          "whollyExempt" : false,
      |          "jointAssetsPassingToOther" : true,
      |          "otherGifts" : false,
      |          "agriculturalOrBusinessRelief" : true,
      |          "giftsWithReservation" : false,
      |          "benefitFromTrust" : true,
      |          "unusedNilRateBand" : 100
      |        }
      |      } ]
      |    }
      |  },
      |  "freeEstate" : {
      |    "estateAssets" : [ {
      |      "assetCode" : "0016",
      |      "assetDescription" : "Deceased's residence",
      |      "assetID" : "null",
      |      "assetTotalValue" : 100,
      |      "howheld" : "Standard",
      |      "liabilities" : [ {
      |        "liabilityType" : "Mortgage",
      |        "liabilityAmount" : 80,
      |        "liabilityOwner" : ""
      |      } ],
      |      "propertyAddress" : {
      |        "addressLine1" : "addr1",
      |        "addressLine2" : "addr2",
      |        "postalCode" : "AA1 1AA",
      |        "countryCode" : "GB"
      |      },
      |      "tenure" : "Freehold",
      |      "tenancyType" : "Vacant Possession",
      |      "yearsLeftOnLease" : 0,
      |      "yearsLeftOntenancyAgreement" : 0
      |    }, {
      |      "assetCode" : "0017",
      |      "assetDescription" : "Other residential property",
      |      "assetID" : "null",
      |      "assetTotalValue" : 200,
      |      "howheld" : "Joint - Beneficial Joint Tenants",
      |      "liabilities" : [ {
      |        "liabilityType" : "Mortgage",
      |        "liabilityAmount" : 150,
      |        "liabilityOwner" : ""
      |      } ],
      |      "propertyAddress" : {
      |        "addressLine1" : "addr1",
      |        "addressLine2" : "addr2",
      |        "postalCode" : "AA1 1AA",
      |        "countryCode" : "GB"
      |      },
      |      "tenure" : "Leasehold",
      |      "tenancyType" : "Vacant Possession",
      |      "yearsLeftOnLease" : 0,
      |      "yearsLeftOntenancyAgreement" : 0
      |    }, {
      |      "assetCode" : "0018",
      |      "assetDescription" : "Other land and buildings",
      |      "assetID" : "null",
      |      "assetTotalValue" : 300,
      |      "howheld" : "Joint - Tenants In Common",
      |      "propertyAddress" : {
      |        "addressLine1" : "addr1",
      |        "addressLine2" : "addr2",
      |        "postalCode" : "AA1 1AA",
      |        "countryCode" : "GB"
      |      },
      |      "tenure" : "Leasehold",
      |      "tenancyType" : "Vacant Possession",
      |      "yearsLeftOnLease" : 0,
      |      "yearsLeftOntenancyAgreement" : 0
      |    }, {
      |      "assetCode" : "9001",
      |      "assetDescription" : "Rolled up bank and building society accounts",
      |      "assetID" : "null",
      |      "assetTotalValue" : 1,
      |      "howheld" : "Standard"
      |    }, {
      |      "assetCode" : "9004",
      |      "assetDescription" : "Rolled up household and personal goods",
      |      "assetID" : "null",
      |      "assetTotalValue" : 8,
      |      "howheld" : "Standard"
      |    }, {
      |      "assetCode" : "9005",
      |      "assetDescription" : "Rolled up pensions",
      |      "assetID" : "null",
      |      "assetTotalValue" : 7,
      |      "howheld" : "Standard"
      |    }, {
      |      "assetCode" : "9010",
      |      "assetDescription" : "Rolled up unlisted stocks and shares",
      |      "assetID" : "null",
      |      "assetTotalValue" : 9,
      |      "howheld" : "Standard"
      |    }, {
      |      "assetCode" : "9008",
      |      "assetDescription" : "Rolled up quoted stocks and shares",
      |      "assetID" : "null",
      |      "assetTotalValue" : 10,
      |      "howheld" : "Standard"
      |    }, {
      |      "assetCode" : "9006",
      |      "assetDescription" : "Rolled up life assurance policies",
      |      "assetID" : "null",
      |      "assetTotalValue" : 12,
      |      "howheld" : "Standard"
      |    }, {
      |      "assetCode" : "9021",
      |      "assetDescription" : "Rolled up business assets",
      |      "assetID" : "null",
      |      "assetTotalValue" : 14,
      |      "howheld" : "Standard"
      |    }, {
      |      "assetCode" : "9099",
      |      "assetDescription" : "Rolled up nominated assets",
      |      "assetID" : "null",
      |      "assetTotalValue" : 16,
      |      "howheld" : "Nominated"
      |    }, {
      |      "assetCode" : "9098",
      |      "assetDescription" : "Rolled up foreign assets",
      |      "assetID" : "null",
      |      "assetTotalValue" : 18,
      |      "howheld" : "Foreign"
      |    }, {
      |      "assetCode" : "9013",
      |      "assetDescription" : "Rolled up money owed to deceased",
      |      "assetID" : "null",
      |      "assetTotalValue" : 15,
      |      "howheld" : "Standard"
      |    }, {
      |      "assetCode" : "9015",
      |      "assetDescription" : "Rolled up other assets",
      |      "assetID" : "null",
      |      "assetTotalValue" : 19,
      |      "howheld" : "Standard"
      |    }, {
      |      "assetCode" : "9001",
      |      "assetDescription" : "Rolled up bank and building society accounts",
      |      "assetID" : "null",
      |      "assetTotalValue" : 2,
      |      "howheld" : "Joint - Beneficial Joint Tenants"
      |    }, {
      |      "assetCode" : "9004",
      |      "assetDescription" : "Rolled up household and personal goods",
      |      "assetID" : "null",
      |      "assetTotalValue" : 10,
      |      "howheld" : "Joint - Beneficial Joint Tenants"
      |    }, {
      |      "assetCode" : "9006",
      |      "assetDescription" : "Rolled up life assurance policies",
      |      "assetID" : "null",
      |      "assetTotalValue" : 13,
      |      "howheld" : "Joint - Beneficial Joint Tenants"
      |    } ],
      |    "estateLiabilities" : [ {
      |      "liabilityType" : "Funeral Expenses",
      |      "liabilityAmount" : 20,
      |      "liabilityOwner" : ""
      |    }, {
      |      "liabilityType" : "Other",
      |      "liabilityAmount" : 90,
      |      "liabilityOwner" : ""
      |    } ],
      |    "estateExemptions" : [ {
      |      "exemptionType" : "Spouse",
      |      "overrideValue" : 25
      |    }, {
      |      "exemptionType" : "Charity",
      |      "overrideValue" : 27
      |    }, {
      |      "exemptionType" : "Charity",
      |      "overrideValue" : 28
      |    }, {
      |      "exemptionType" : "GNCP",
      |      "overrideValue" : 30
      |    }, {
      |      "exemptionType" : "GNCP",
      |      "overrideValue" : 31
      |    } ]
      |  },
      |  "gifts" : [ [ {
      |    "assetCode" : "9095",
      |    "assetDescription" : "Rolled up gifts",
      |    "assetID" : "null",
      |    "assetTotalValue" : 6000,
      |    "howheld" : "Standard",
      |    "valuePrevOwned" : 6000,
      |    "percentageSharePrevOwned" : 100,
      |    "valueRetained" : 0,
      |    "percentageRetained" : 0,
      |    "lossToEstate" : 6000,
      |    "dateOfGift" : "2010-04-05"
      |  }, {
      |    "assetCode" : "9095",
      |    "assetDescription" : "Rolled up gifts",
      |    "assetID" : "null",
      |    "assetTotalValue" : 5000,
      |    "howheld" : "Standard",
      |    "valuePrevOwned" : 5000,
      |    "percentageSharePrevOwned" : 100,
      |    "valueRetained" : 0,
      |    "percentageRetained" : 0,
      |    "lossToEstate" : 5000,
      |    "dateOfGift" : "2009-04-05"
      |  }, {
      |    "assetCode" : "9095",
      |    "assetDescription" : "Rolled up gifts minus exemption of £200",
      |    "assetID" : "null",
      |    "assetTotalValue" : 1800,
      |    "howheld" : "Standard",
      |    "valuePrevOwned" : 2000,
      |    "percentageSharePrevOwned" : 100,
      |    "valueRetained" : 0,
      |    "percentageRetained" : 0,
      |    "lossToEstate" : 1800,
      |    "dateOfGift" : "2006-04-05"
      |  }, {
      |    "assetCode" : "9095",
      |    "assetDescription" : "Rolled up gifts",
      |    "assetID" : "null",
      |    "assetTotalValue" : 1000,
      |    "howheld" : "Standard",
      |    "valuePrevOwned" : 1000,
      |    "percentageSharePrevOwned" : 100,
      |    "valueRetained" : 0,
      |    "percentageRetained" : 0,
      |    "lossToEstate" : 1000,
      |    "dateOfGift" : "2005-04-05"
      |  }, {
      |    "assetCode" : "9095",
      |    "assetDescription" : "Rolled up gifts",
      |    "assetID" : "null",
      |    "assetTotalValue" : 3000,
      |    "howheld" : "Standard",
      |    "valuePrevOwned" : 3000,
      |    "percentageSharePrevOwned" : 100,
      |    "valueRetained" : 0,
      |    "percentageRetained" : 0,
      |    "lossToEstate" : 3000,
      |    "dateOfGift" : "2007-04-05"
      |  }, {
      |    "assetCode" : "9095",
      |    "assetDescription" : "Rolled up gifts",
      |    "assetID" : "null",
      |    "assetTotalValue" : 4000,
      |    "howheld" : "Standard",
      |    "valuePrevOwned" : 4000,
      |    "percentageSharePrevOwned" : 100,
      |    "valueRetained" : 0,
      |    "percentageRetained" : 0,
      |    "lossToEstate" : 4000,
      |    "dateOfGift" : "2008-04-05"
      |  }, {
      |    "assetCode" : "9095",
      |    "assetDescription" : "Rolled up gifts",
      |    "assetID" : "null",
      |    "assetTotalValue" : 7000,
      |    "howheld" : "Standard",
      |    "valuePrevOwned" : 7000,
      |    "percentageSharePrevOwned" : 100,
      |    "valueRetained" : 0,
      |    "percentageRetained" : 0,
      |    "lossToEstate" : 7000,
      |    "dateOfGift" : "2011-04-05"
      |  } ] ],
      |  "trusts" : [ {
      |    "trustName" : "Deceased Trust",
      |    "trustAssets" : [ {
      |      "assetCode" : "9097",
      |      "assetDescription" : "Rolled up trust assets",
      |      "assetID" : "null",
      |      "assetTotalValue" : 17,
      |      "howheld" : "Standard"
      |    } ]
      |  } ],
      |  "declaration" : {
      |    "reasonForBeingBelowLimit" : "Excepted Estate",
      |    "declarationAccepted" : true,
      |    "coExecutorsAccepted" : true
      |  }
      |}
    """.stripMargin
  }

  def invalidResultBodyForSubmission(status: Int, nino: String, ihtRef: String): String = {
    s"""
      |{
      |"statusCode" : $status,
      |"message" : "uk.gov.hmrc.http.Upstream5xxResponse: GET of 'http://localhost:11111/inheritance-tax/individuals/$nino/cases/$ihtRef' returned $status. Response body: 'Returned $status'"
      |}
    """.stripMargin
  }

  val invalidResultBodyIndividualReturn = {
    "{\"statusCode\":502,\"message\":\"POST of 'http://localhost:11111/inheritance-tax/individuals/AA123456A/cases/A0000A0000A0000/returns' returned 503. Response body: '\\n{\\\"returnId\\\" : \\\"12\\\"}\\n    'des_error_code_503\"}"
  }

  val invalidResultBodyGetCase = {
    "{\"statusCode\":502,\"message\":\"GET of 'http://localhost:11111/inheritance-tax/individuals/AA123456A/cases/A0000A0000A0000' returned 503. Response body: '\\n{\\\"deceased\\\"                 : {\\n \\\"dateOfDeath\\\"             : \\\"2018-05-20\\\",\\n \\\"firstName\\\"               : \\\"John\\\",\\n \\\"middleName\\\"              : null,\\n \\\"lastName\\\"                : \\\"Bagelson\\\",\\n \\\"nino\\\"                    : \\\"AA000000A\\\",\\n \\\"mainAddress\\\"               : {\\n  \\\"addressLine1\\\"         : \\\"21\\\",\\n  \\\"addressLine2\\\"         : \\\"Bagel Road\\\",\\n  \\\"addressLine3\\\"         : null,\\n  \\\"addressLine4\\\"         : null,\\n  \\\"postCode\\\"               : \\\"BA63LS\\\",\\n  \\\"countryCode\\\"            : \\\"UK\\\"\\n },\\n \\\"dateOfBirth\\\"             : \\\"1980-05-20\\\",\\n \\\"domicile\\\"                : \\\"UK\\\",\\n \\\"maritalStatus\\\"           : \\\"Single\\\"\\n},\\n\\\"leadExecutor\\\"             : {\\n \\\"firstName\\\"               : \\\"John\\\",\\n \\\"lastName\\\"                : \\\"Smith\\\",\\n \\\"nino\\\"                    : \\\"AA123456A\\\",\\n \\\"dateOfBirth\\\"             : \\\"1980-05-20\\\",\\n \\\"mainAddress\\\"               : {\\n   \\\"addressLine1\\\"        : \\\"21\\\",\\n   \\\"addressLine2\\\"        : \\\"Bagel Road\\\",\\n   \\\"addressLine3\\\"        : null,\\n   \\\"addressLine4\\\"        : null,\\n   \\\"postCode\\\"              : \\\"BA63LS\\\",\\n   \\\"countryCode\\\"           : \\\"UK\\\"\\n },\\n \\\"phoneNo\\\"                 : null,\\n \\\"country\\\"                 : \\\"England or Wales\\\",\\n \\\"role\\\"                    : \\\"Lead Executor\\\"\\n},\\n\\\"coExecutors\\\"              : null,\\n\\\"ihtReference\\\"             : \\\"A0000A0000A0000\\\",\\n\\\"caseStatus\\\"               : \\\"active\\\",\\n\\\"acknowledgmentReference\\\"  : \\\"AA000000BB111111\\\",\\n\\\"returns\\\"                  : [{\\n \\\"returnDate\\\"              : \\\"2016-05-14\\\",\\n \\\"returnId\\\"                : \\\"AAAABBBB\\\",\\n \\\"returnVersionNumber\\\"     : \\\"123456789\\\",\\n \\\"submitterRole\\\"           : \\\"Friend\\\"\\n }]\\n}\\n          'des_error_code_503\"}"
  }

}
