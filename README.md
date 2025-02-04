# pillar2-submission-frontend

Front-end microservice for Pillar 2  project. Pillar 2 refers to the Global Minimum Tax being introduced by the Organisation for Economic Cooperation and Development (OECD).

The Pillar 2 Tax will ensure that global Multinational Enterprises (MNEs) with a turnover of >€750m are subject to a minimum Effective Tax Rate of 15%, i.e. a top-up tax for Medium to Large MNEs.

## Running the service

You can use service manage to run all dependent microservices using the command below

    sm2 --start PILLAR2_ALL
    sm2 --stop PILLAR2_ALL
Or you could run this microservice locally using

    sbt run
Test-only route:

    sbt 'run 10053'
To run locally:

Navigate to http://localhost:9949/auth-login-stub/gg-sign-in which redirects to auth-login-stub page.


***Redirect URL: http://localhost:10053/report-pillar2-submission-top-up-taxes***

***Affinity Group: Organisation***

## Starting the Below Threshold Journey Locally

To test the Below-Threshold Notification (BTN) journey locally, follow these steps:

1. Ensure all Service Manager pillar-2 processes (including MongoDB) are running.
2. Visit the Authority Wizard at: http://localhost:9949/auth-login-stub/gg-sign-in and fill in:
   - CredentialId: any value.
   - Redirect URL: http://localhost:10050/report-pillar2-top-up-taxes/
   - Affinity Group: Organisation.
   - Enrollment details:
       • Enrolment Key: HMRC-PILLAR2-ORG
       • Identifier Name: PLRID
       • Identifier Value: your chosen Pillar2ID (e.g., use `XEPLR5000000000` to simulate a 500 error).
3. After submission, change your browser URL to:
   http://localhost:10053/report-pillar2-submission-top-up-taxes/below-threshold-notification/start
   to begin the BTN journey.
4. Follow the on-screen prompts to complete the journey. When you get to submitting the BTN you will get the reponse that is associated with the Pillar2ID you used in the Authority Wizard.

## Key Terminologies


To run the unit tests:

    Run 'sbt test' from directory the project is stored in 

To check code coverage:

    sbt clean scalafmt test:scalafmt it/test coverage test it/test coverageReport  
To run Integration tests:

    sbt it:test

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").