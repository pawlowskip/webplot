name := """WebPlot"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

resolvers ++= Seq(
    "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/",
    "RoundEights" at "http://maven.spikemark.net/roundeights",
    Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.6.play24",
  "jp.t2v" %% "play2-auth" % "0.14.1",
  "jp.t2v" %% "play2-auth-test" % "0.14.1" % "test",
  "com.softwaremill.macwire" %% "macros" % "1.0.5",
  "com.softwaremill.macwire" %% "runtime" % "1.0.5",
  "com.github.t3hnar" % "scala-bcrypt_2.11" % "2.4",
  play.sbt.Play.autoImport.cache,
  specs2 % Test
)

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.

routesGenerator := InjectedRoutesGenerator

scalacOptions += "-feature"

fork in run := true