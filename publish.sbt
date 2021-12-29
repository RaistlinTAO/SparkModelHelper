ThisBuild / organization := "io.github.raistlintao"
ThisBuild / organizationName := "io.github.raistlintao"
ThisBuild / organizationHomepage := Some(url("https://github.com/RaistlinTAO/"))
ThisBuild / versionScheme := Some("pvp")

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/RaistlinTAO/SparkModelHelper"),
    "scm:git@github.com:RaistlinTAO/SparkModelHelper.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "RasitlinTAO",
    name  = "Rasitlin TAO",
    email = "Demonvk@gmail.com",
    url   = url("https://github.com/RaistlinTAO/")
  )
)

ThisBuild / description := "A helper that extracting useful information from trained Spark Model"
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/RaistlinTAO/SparkModelHelper"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true