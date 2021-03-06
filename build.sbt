import sbt._
import sbt.Keys._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "iht"

lazy val appDependencies : Seq[ModuleID] = AppDependencies()
lazy val plugins : Seq[Plugins] = Seq()
lazy val playSettings : Seq[Setting[_]] = Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys

  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;models/.data/..*;.*BuildInfo.*;prod.Routes;app.Routes.*;testOnlyDoNotUseInAppConf.Routes;connectors.*;config.*;testOnlyDoNotUseInAppConf.*",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

val wartRemovedExcludedClasses = Seq()

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin) ++ plugins : _*)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(playSettings ++ scoverageSettings : _*)
  .settings(majorVersion := 5)
  .settings(publishingSettings: _*)
  .settings(isPublicArtefact := true)
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true
  )
  .settings(
    resolvers += Resolver.jcenterRepo,
    scalaVersion := "2.12.12"
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork                  in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    parallelExecution          in IntegrationTest := false
  )
  .settings(wartremover.WartRemover.projectSettings : _*)
  .settings(
    wartremoverWarnings ++= Warts.unsafe,
    wartremoverExcluded ++= wartRemovedExcludedClasses,
    wartremoverExcluded ++= WartRemoverConfig.makeExcludedFiles(baseDirectory.value))
// ***************
// Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
scalacOptions += "-P:silencer:pathFilters=routes"
scalacOptions += "-P:silencer:lineContentFilters=^\\w"
libraryDependencies ++= Seq(
  compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.1" cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib" % "1.7.1" % Provided cross CrossVersion.full
)
// ***************
scalacOptions += "-feature"