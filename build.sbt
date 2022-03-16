import ReleaseTransformations._

ThisBuild / version       := "0.1.0"
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / scalaVersion  := "2.12.15"

ThisBuild / organizationHomepage := Some(url("http://pinecone.io/"))
ThisBuild / licenses := Seq(
  ("Pinecone EULA", url("https://www.pinecone.io/thin-client-user-agreement/"))
)

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

val sparkVersion = "3.2.0"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "spark-pinecone",
    Defaults.itSettings,
    organization       := "io.pinecone",
    crossScalaVersions := Seq("2.12.15", "2.13.8"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    libraryDependencies ++= Seq(
      "io.pinecone"       % "pinecone-client" % "0.2.1",
      "org.scalatest"    %% "scalatest"       % "3.2.11"     % "it,test",
      "org.apache.spark" %% "spark-core"      % sparkVersion % "provided,test",
      "org.apache.spark" %% "spark-sql"       % sparkVersion % "provided,test",
      "org.apache.spark" %% "spark-catalyst"  % sparkVersion % "provided,test"
    ),
    Test / fork       := true,
    releaseCrossBuild := true, // true if you cross-build the project for multiple Scala versions
    publishTo         := sonatypePublishToBundle.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepCommand("sonatypeBundleRelease"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
