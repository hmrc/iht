import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val jsonSchemaValidatorVersion = "2.2.6"
  val bootstrapVersion = "5.25.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "simple-reactivemongo"      % "8.0.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"       %% "http-caching-client"       % "9.5.0-play-28",
    "uk.gov.hmrc"       %% "domain"                    % "6.0.0-play-28",
    "com.github.fge"    % "json-schema-validator"      % jsonSchemaValidatorVersion,
    "com.typesafe.play" %% "play-json-joda"            % "2.9.2"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  val reactiveMongoTestVersion = "5.0.0-play-28"
  val pegdownVersion = "1.6.0"

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc"            %% "reactivemongo-test"        % reactiveMongoTestVersion   % scope,
        "org.pegdown"            % "pegdown"                    % pegdownVersion             % scope,
        "com.typesafe.play"      %% "play-test"                 % PlayVersion.current        % scope,
        "com.github.fge"         % "json-schema-validator"      % jsonSchemaValidatorVersion % scope,
        "org.mockito"            % "mockito-core"               % "3.3.3"                    % scope,
        "uk.gov.hmrc"            %%  "bootstrap-test-play-28"   % bootstrapVersion           % scope,
        "org.scalatestplus"      %%  "scalatestplus-mockito"    % "1.0.0-M2"                 % scope,
        "org.scalatestplus"      %%  "scalatestplus-scalacheck" % "3.1.0.0-RC2"              % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test = Seq(
        "uk.gov.hmrc"               %% "reactivemongo-test"        % reactiveMongoTestVersion   % scope,
        "org.pegdown"               % "pegdown"                    % pegdownVersion             % scope,
        "com.typesafe.play"         %% "play-test"                 % PlayVersion.current        % scope,
        "com.github.fge"            % "json-schema-validator"      % jsonSchemaValidatorVersion % scope,
        "org.mockito"               % "mockito-all"                % "1.10.19"                  % scope,
        "uk.gov.hmrc"               %%  "bootstrap-test-play-28"   % bootstrapVersion           % scope,
        "org.scalatestplus"         %%  "scalatestplus-mockito"    % "1.0.0-M2"                 % scope,
        "org.scalatestplus"         %%  "scalatestplus-scalacheck" % "3.1.0.0-RC2"              % scope,
        "com.github.tomakehurst"    % "wiremock-jre8"              % "2.26.3"                   % "test,it"
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
