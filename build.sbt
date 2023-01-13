import ReleaseTransformations._
lazy val sparkVersion = "3.2.0"

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name                 := "spark-pinecone",
    organizationName     := "Pinecone Systems",
    organizationHomepage := Some(url("http://pinecone.io/")),
    organization         := "io.pinecone",
    licenses := Seq(("Pinecone EULA", url("https://www.pinecone.io/thin-client-user-agreement/"))),
    description := "A spark connector for the Pinecone Vector Database",
    developers := List(
      Developer(
        "adamgs",
        "Adam Gutglick",
        "adam@pinecone.io",
        url("https://github.com/pinecone-io")
      ),
      Developer(
        "rajat08",
        "Rajat Tripathi",
        "rajat@pinecone.io",
        url("https://github.com/pinecone-io")
      )
    ),
    versionScheme := Some("semver-spec"),
    scalaVersion  := "2.12.15",
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/pinecone-io/spark-pinecone"),
        "scm:git:git@github.com:pinecone-io/spark-pinecone.git"
      )
    ),
    homepage := Some(url("https://github.com/pinecone-io/spark-pinecone")),
    Defaults.itSettings,
    crossScalaVersions := Seq("2.12.15", "2.13.8"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    libraryDependencies ++= Seq(
      "io.pinecone"       % "pinecone-client" % "0.2.2",
      "org.scalatest"    %% "scalatest"       % "3.2.11"     % "it,test",
      "org.apache.spark" %% "spark-core"      % sparkVersion % "provided,test",
      "org.apache.spark" %% "spark-sql"       % sparkVersion % "provided,test",
      "org.apache.spark" %% "spark-catalyst"  % sparkVersion % "provided,test"
    ),
    Test / fork       := true,
    assembly / assemblyShadeRules := Seq(
      ShadeRule
        .rename("com.google.protobuf.**" -> "shaded.protobuf.@1")
        .inAll,
      ShadeRule
        .rename("com.google.common.**" -> "shaded.guava.@1")
        .inAll
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs@_*) => MergeStrategy.concat
      case x => MergeStrategy.first
    },
//    Build assembly jar, this builds an uberJar with all dependencies
    assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
    assembly / artifact := {
      val art = (assembly / artifact).value
      art.withClassifier(Some("assembly"))
    },
    addArtifact(assembly / artifact, assembly),
    publishLocal / skip := true,
    ThisBuild / publishMavenStyle := true,
//  Expects credentials stored in ~/.sbt/sonatype_credentials. This is a standard practice
    credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials"),
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
