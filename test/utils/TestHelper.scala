/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

/**
 *
 * Created by Vineet Tyagi on 26/11/15.
 *
 */

object TestHelper {

  // Schema paths
  val schemaPathRegistrationSubmission = "/schemas/Registration_RequestSchema_v0.7.json"
  private val nino = CommonBuilder.DefaultNino
  val JsEmptyListCases = """{}"""
  val JsListCases=NinoBuilder.replacePlaceholderNinoWithDefault(AcknowledgementRefGenerator.replacePlaceholderAckRefWithDefault("""{
                              "deathEvents": [
                              {
                               "acknowledgmentReference": "<ACKREF>",
                                "ihtReference": "1234567890ABCDE",
                                "deceased": {
                                  "firstName": "ABCDE",
                                  "lastName": "ABCXY",
                                  "dateOfBirth": "1972-01-01",
                                  "dateOfDeath": "2014-11-01",
                                  "nino": "<NINO>"
                                },
                                "caseSummary": {
                                  "entryType": "Free Estate",
                                  "roleOfSubject": "Lead Executor",
                                  "registrationDate": "2015-05-01",
                                  "status": "Awaiting Return"
                                }
                              },
                              {
                               "acknowledgmentReference": "<ACKREF>",
                                "ihtReference": "1234567890ABCDE",
                                "deceased": {
                                  "firstName": "XYZAB",
                                  "lastName": "DEFGH",
                                  "dateOfBirth": "1972-01-01",
                                  "dateOfDeath": "2014-10-05",
                                  "nino": "<NINO>"
                                },
                                "caseSummary": {
                                  "entryType": "Free Estate",
                                  "roleOfSubject": "Executor",
                                  "registrationDate": "2015-05-01",
                                  "status": "Awaiting Return"
                                }
                              },
                              {
                               "acknowledgmentReference": "<ACKREF>",
                                "ihtReference": "1234567890ABCDE",
                                "deceased": {
                                  "firstName": "AAABBB",
                                  "lastName": "CCCDDD",
                                  "dateOfBirth": "1972-01-01",
                                  "dateOfDeath": "2014-11-09",
                                  "nino": "<NINO>"
                                },
                                "caseSummary": {
                                  "entryType": "Free Estate",
                                  "roleOfSubject": "Executor",
                                  "registrationDate": "2015-05-01",
                                  "status": "In Review"
                                }
                              },
                              {
                               "acknowledgmentReference": "<ACKREF>",
                                "ihtReference": "1234567890ABCDE",
                                "deceased": {
                                  "firstName": "FFFTTT",
                                  "lastName": "YYYZZZ",
                                  "dateOfBirth": "1972-01-01",
                                  "dateOfDeath": "2014-10-05",
                                  "nino": "<NINO>"
                                },
                                "caseSummary": {
                                  "entryType": "Free Estate",
                                  "roleOfSubject": "Executor",
                                  "registrationDate": "2014-10-05",
                                  "status": "Closed"
                                }
                              }
                            ]
                          }"""))

  val JsListCasesNoNINO=NinoBuilder.replacePlaceholderNinoWithDefault(AcknowledgementRefGenerator.replacePlaceholderAckRefWithDefault("""{
                              "deathEvents": [
                              {
                               "acknowledgmentReference": "<ACKREF>",
                                "ihtReference": "1234567890ABCDE",
                                "deceased": {
                                  "firstName": "XYZABC",
                                  "lastName": "CCCRRR",
                                  "dateOfBirth": "1972-01-01",
                                  "dateOfDeath": "2014-11-01"
                                },
                                "caseSummary": {
                                  "entryType": "Free Estate",
                                  "roleOfSubject": "Lead Executor",
                                  "registrationDate": "2015-05-01",
                                  "status": "Awaiting Return"
                                }
                              },
                              {
                               "acknowledgmentReference": "<ACKREF>",
                                "ihtReference": "1234567890ABCDE",
                                "deceased": {
                                  "firstName": "CCCAAA",
                                  "lastName": "RRRTTT",
                                  "dateOfBirth": "1972-01-01",
                                  "dateOfDeath": "2014-10-05"
                                },
                                "caseSummary": {
                                  "entryType": "Free Estate",
                                  "roleOfSubject": "Executor",
                                  "registrationDate": "2015-05-01",
                                  "status": "Awaiting Return"
                                }
                              },
                              {
                               "acknowledgmentReference": "<ACKREF>",
                                "ihtReference": "1234567890ABCDE",
                                "deceased": {
                                  "firstName": "WWWQQQ",
                                  "lastName": "RRRSSS",
                                  "dateOfBirth": "1972-01-01",
                                  "dateOfDeath": "2014-11-09"
                                },
                                "caseSummary": {
                                  "entryType": "Free Estate",
                                  "roleOfSubject": "Executor",
                                  "registrationDate": "2015-05-01",
                                  "status": "In Review"
                                }
                              },
                              {
                               "acknowledgmentReference": "<ACKREF>",
                                "ihtReference": "1234567890ABCDE",
                                "deceased": {
                                  "firstName": "JJJNNN",
                                  "lastName": "NNNMMM",
                                  "dateOfBirth": "1972-01-01",
                                  "dateOfDeath": "2014-10-05"
                                },
                                "caseSummary": {
                                  "entryType": "Free Estate",
                                  "roleOfSubject": "Executor",
                                  "registrationDate": "2014-10-05",
                                  "status": "Closed"
                                }
                              }
                            ]
                          }"""))


  val JsSampleCaseDetailsString=NinoBuilder.replacePlaceholderNinoWithDefault(AcknowledgementRefGenerator.replacePlaceholderAckRefWithDefault("""{
                                    "acknowledgmentReference": "<ACKREF>",
                                    "ihtReference":"1234567890ABCDE",
                                    "event": {
                                      "eventType": "Death",
                                      "entryType": "Free Estate"
                                    },
                                    "leadExecutor": {
                                      "firstName": "VVVTTT",
                                      "lastName": "TTTRRR",
                                      "gender": "Male",
                                      "dateOfBirth": "1995-01-01",
                                      "mainAddress": {
                                      "addressLine1": "100, Testaddress Street",
                                      "addressLine2": "Testline2",
                                      "addressLine3": "Testline3",
                                      "addressLine4": null,
                                      "postalCode": "AA1 1AA",
                                      "countryCode": "GB"
                                    }
                                    },
                                    "deceased": {
                                      "firstName": "Abc",
                                      "lastName": "Xyz",
                                      "nino": "<NINO>",
                                      "gender": "Male",
                                      "dateOfBirth": "1995-01-01",
                                      "dateOfDeath": "2015-01-01",
                                      "mainAddress": {
                                      "addressLine1": "100, Testaddress Street",
                                      "addressLine2": "Testline2",
                                      "addressLine3": "Testline3",
                                      "addressLine4": null,
                                      "postalCode": "AA1 1AA",
                                      "countryCode": "GB"
                                    },
                                      "maritalStatus": "Married or in Civil Partnership",
                                      "domicile": "England or Wales"
                                    },
                                    "coExecutors": [
                                    {
                                      "firstName": "Aaa",
                                      "lastName": "Bbb",
                                      "gender": "Male",
                                      "dateOfBirth": "1995-01-01",
                                      "mainAddress": {
                                        "addressLine1": "100, Testaddress Street",
                                        "addressLine2": "Testline2",
                                        "addressLine3": "Testline3",
                                        "addressLine4": null,
                                        "postalCode": "AA1 1AA",
                                        "countryCode": "GB"
                                      }
                                    },
                                    {
                                      "firstName": "Xxx",
                                      "lastName": "Yyy",
                                      "gender": "Male",
                                      "dateOfBirth": "1995-01-01",
                                      "mainAddress": {
                                        "addressLine1": "100, Testaddress Street",
                                        "addressLine2": "Testline2",
                                        "addressLine3": "Testline3",
                                        "addressLine4": null,
                                        "postalCode": "AA1 1AA",
                                        "countryCode": "GB"
                                      }
                                    }
                                    ],
                                    "returns": [
                                    {
                                      "returnDate": "2115-01-01T00:00:00",
                                      "returnId": "1234567890",
                                      "returnVersionNumber": "12",
                                      "submitterRole": "Lead Executor"
                                    }
                                    ],
                                    "caseStatus": "In Review"
                                 }"""))

  val successHttpResponseForCaseDetailsWithPostCodeNull=NinoBuilder.replacePlaceholderNinoWithDefault(
    AcknowledgementRefGenerator.replacePlaceholderAckRefWithDefault("""{
                                    "acknowledgmentReference": "<ACKREF>",
                                    "ihtReference":"1234567890ABCDE",
                                    "event": {
                                      "eventType": "Death",
                                      "entryType": "Free Estate"
                                    },
                                    "leadExecutor": {
                                      "firstName": "ABCPPP",
                                      "lastName": "PPPAAA",
                                      "gender": "Male",
                                      "dateOfBirth": "1995-01-01",
                                      "mainAddress": {
                                      "addressLine1": "100, Testaddress Street",
                                      "addressLine2": "Testline2",
                                      "addressLine3": "Testline3",
                                      "addressLine4": null,
                                      "postalCode": null,
                                      "countryCode": "UZ"
                                    }
                                    },
                                    "deceased": {
                                      "firstName": "CCCVVV",
                                      "lastName": "VVVBBB",
                                      "nino": "<NINO>",
                                      "gender": "Male",
                                      "dateOfBirth": "1995-01-01",
                                      "dateOfDeath": "2015-01-01",
                                      "mainAddress": {
                                      "addressLine1": "100, Testaddress Street",
                                      "addressLine2": "Testline2",
                                      "addressLine3": "Testline3",
                                      "addressLine4": null,
                                      "postalCode": null,
                                      "countryCode": "ROM"
                                    },
                                      "maritalStatus": "Married or in Civil Partnership",
                                      "domicile": "England or Wales"
                                    },
                                    "coExecutors": [
                                    {
                                      "firstName": "MMMKKK",
                                      "lastName": "KKKLLL",
                                      "gender": "Male",
                                      "dateOfBirth": "1995-01-01",
                                      "mainAddress": {
                                        "addressLine1": "100, Testaddress Street",
                                        "addressLine2": "Testline2",
                                        "addressLine3": "Testline3",
                                        "addressLine4": null,
                                        "postalCode": "",
                                        "countryCode": "GB"
                                      }
                                    },
                                    {
                                      "firstName": "VVVRRR",
                                      "lastName": "TTTUUU",
                                      "gender": "Male",
                                      "dateOfBirth": "1995-01-01",
                                      "mainAddress": {
                                        "addressLine1": "100, Testaddress Street",
                                        "addressLine2": "Testline2",
                                        "addressLine3": "Testline3",
                                        "addressLine4": null,
                                        "postalCode": "AA1 1AA",
                                        "countryCode": "GB"
                                      }
                                    }
                                    ],
                                    "returns": [
                                    {
                                      "returnDate": "2115-01-01T00:00:00",
                                      "returnId": "1234567890",
                                      "returnVersionNumber": "12",
                                      "submitterRole": "Lead Executor"
                                    }
                                    ],
                                    "caseStatus": "In Review"
                                 }"""))


  val json = NinoBuilder.replacePlaceholderNinoWithDefault(AcknowledgementRefGenerator.replacePlaceholderAckRefWithDefault("{\n  \"acknowledgmentReference\":\"<ACKREF>\",\n  \"event\":\n    {\n    \"eventType\": \"death\",\n    \"entryType\":\"Free Estate\"\n    },\n  \"leadExecutor\":\n    {\n      \"title\": \"Mr\",\n      \"firstName\": \"XXXYYY\",\n      \"lastName\": \"ZZZCCC\",\n      \"nino\": \"<NINO>\",\n      \"utr\": \"0123456789\",\n      \"dateOfBirth\": \"1978-12-27\",\n      \"gender\": \"Male\",\n      \"mainAddress\":\n        {\n        \"addressLine1\": \"1, Testtest Street\",\n        \"addressLine2\": \"Testline2\",\n        \"addressLine3\": \"Testline3\",\n        \"addressLine4\":\"Testcity\",\n        \"postalCode\": \"AA1 1AA\",\n        \"countryCode\": \"GB\"\n        },\n      \"contactDetails\": {\n        \"phoneNumber\": \"0712 345678\",\n        \"emailAddress\": \"a@example.com\"\n      }\n    },\n    \"coExecutors\":\n    [\n    {\n      \"title\": \"Mr\",\n      \"firstName\": \"PPPTTT\",\n      \"lastName\": \"TTTRRR\",\n      \"nino\": \"<NINO>\",\n      \"utr\": \"1234567890\",\n      \"dateOfBirth\": \"1976-03-26\",\n      \"gender\": \"Male\",\n      \"mainAddress\":\n        {\n        \"addressLine1\": \"2, Adressline1 Street\",\n        \"addressLine2\": \"Testaddress2\",\n        \"postalCode\": \"AA11AA\",\n        \"countryCode\": \"GB\"\n        },\n      \"contactDetails\":\n            {\n            \"phoneNumber\": \"01700 1234567\",\n            \"emailAddress\": \"a@b.com\"\n            }\n    }\n    ],\n     \n  \"deceased\": {\n    \"title\": \"Mr\",\n    \"firstName\": \"SSSWWW\",\n    \"lastName\": \"WWWQQQ\",\n    \"nino\": \"<NINO>\",\n    \"utr\": \"1234567890\",\n    \"dateOfBirth\": \"1976-03-26\",\n    \"dateOfDeath\": \"2004-10-05\",\n    \"gender\": \"Male\",\n    \"domicile\": \"England or Wales\",\n    \"maritalStatus\": \"Single\",\n    \"mainAddress\":\n        {\n        \"addressLine1\": \"3, Testestaddress Street\",\n        \"addressLine2\": \"Testaddres2line\",\n        \"postalCode\": \"AA1 1AA\",\n        \"countryCode\": \"GB\"\n        }\n  }\n}"))

  val JsSampleProbateDetails = """{
                              "probateTotals": {
                                  "grossEstateforIHTPurposes": 123456.78,
                                  "grossEstateforProbatePurposes": 123456.78,
                                  "totalDeductionsForProbatePurposes": 123456.78,
                                  "netEstateForProbatePurposes": 123456.78,
                                  "valueOfEstateOutsideOfTheUK": 123456.78,
                                  "valueOfTaxPaid": 0,
                                  "probateReference": "12345678A01-123"
                                  }
                                }"""
}
