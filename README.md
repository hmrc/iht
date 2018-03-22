# Inheritance Tax Microservice
 
[![Build Status](https://travis-ci.org/hmrc/iht.svg?branch=master)](https://travis-ci.org/hmrc/iht) [![Download](https://api.bintray.com/packages/hmrc/releases/iht/images/download.svg)](https://bintray.com/hmrc/releases/iht/_latestVersion)

This is the microservice for Inheritance Tax Service. The microservice acts a communication layer between the various HODS and Storage Layers. The microservice is based on the RESTful API structure, receives and sends data using JSON to either a HOD or Mongo Storage Layer.

All data received is validated against the relevant schema to ensure correct format of the data being received.

## Secure Storage Component

The Secure storage component is akka based service which takes a JSON model and encrypts it and then stores the data in a Mongo DB Instance. The component also checks all records in the mongo instance periodically to make sure that they have not passed the allocated time limit of 13 months.

## HMRC HOD communication

The microservice faciliates the communication between the service and HMRC. This includes sending and receiving information related to the applicants, Inheritance Tax estate Report.

## APIs

| Event | URI | HTTP METHOD | Request Body | Response Body |
|---|---|---|---|---|
| Register For IHT | /inheritance-tax/individuals/[NINO]/cases/ | POST | YES | YES |
| IHT Tax Return (Application) | /inheritance-tax/individuals/[NINO]/cases/[IHTref]/returns | POST | YES | YES |
| Get Return Details | /inheritance-tax/individuals/[NINO]/cases/[IHTref]/returns/[returnId] | GET | NO | YES |
| Get Case Details | /inheritance-tax/individuals/[NINO]/cases/[IHTref] | GET | NO | YES |
| List Cases | /inheritance-tax/individuals/[NINO]/cases/ | GET | NO | YES |
| Request Clearance | /inheritance-tax/individuals/[NINO]/cases/[IHTref]/clearance | POST | YES | YES |
| Get Probate Details | /inheritance-tax/individuals/[NINO]/cases/[IHTref]/returns/[returnId]/probate | GET |	NO |	YES |

## How to run

You will need to clone the project first then navigate to the main folder and run the following sbt command ```sbt "run 9071"```.

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
