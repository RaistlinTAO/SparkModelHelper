import sbt.Keys.libraryDependencies

ThisBuild / version := "1.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "SparkModelHelper",
    idePackagePrefix := Some("io.github.RaistlinTao"),
    // https://mvnrepository.com/artifact/net.liftweb/lift-json
    libraryDependencies += "net.liftweb" %% "lift-json" % "3.5.0",
    // https://mvnrepository.com/artifact/com.alibaba/fastjson
    libraryDependencies += "com.alibaba" % "fastjson" % "1.2.79",
    // https://mvnrepository.com/artifact/org.apache.spark/spark-mllib
    libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.4.8"
  )
