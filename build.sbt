import xerial.sbt.Sonatype.sonatypeCentralHost
import ReleaseTransformations._

// Basic project information
ThisBuild / organization := "com.leaprail"
ThisBuild / description := "SBT plugin for managing Docker images within Amazon ECR"
ThisBuild / homepage := Some(url("https://github.com/leaprail/sbt-ecr"))
ThisBuild / licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))
ThisBuild / startYear := Some(2016)

// ScalaVersion settings
ThisBuild / scalaVersion := "2.12.18" // Latest Scala 2.12 version
ThisBuild / crossScalaVersions := Seq("2.12.18")

// SBT Plugin settings
sbtPlugin := true
sbtPluginPublishLegacyMavenStyle := false

// Modern compiler options
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-encoding", "UTF-8",
  "-Xfatal-warnings",
  "-language:higherKinds",
  "-language:implicitConversions"
)

// Dependencies
val scalaTestVersion = "3.2.17"
val awsSdkVersion = "2.25.6"  // Latest stable AWS SDK v2 version

libraryDependencies ++= Seq(
  // AWS SDK v2 dependencies
  "software.amazon.awssdk" % "ecr"     % awsSdkVersion,
  "software.amazon.awssdk" % "sts"     % awsSdkVersion,
  "software.amazon.awssdk" % "sso"     % awsSdkVersion,
  "software.amazon.awssdk" % "ssooidc" % awsSdkVersion,

  // Test dependencies
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test
)

// Sonatype publishing settings
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / sonatypeCredentialHost := sonatypeCentralHost
publishMavenStyle := true
pomIncludeRepository := { _ => false }

// Developer information
ThisBuild / developers := List(
  Developer(
    id = "leaprail",
    name = "LeapRail",
    email = "shayan@leaprail.com",
    url = url("https://www.leaprail.com")
  )
)

// SCM information
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/leaprail/sbt-ecr"),
    "scm:git:git@github.com:leaprail/sbt-ecr.git"
  )
)

// Release settings
ThisBuild / versionScheme := Some("semver-spec")

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

// Support for older SBT versions when cross-building
pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.2.8" // This is the minimum SBT version we support
  }
}