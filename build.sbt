name := "sclera-plugin-weka"

description := "Add-on that enables Sclera to perform machine learning using the WEKA library from within SQL"

version := "4.0-SNAPSHOT"

homepage := Some(url("https://github.com/scleradb/sclera-plugin-weka"))

organization := "com.scleradb"

organizationName := "Sclera, Inc."

organizationHomepage := Some(url("https://www.scleradb.com"))

startYear := Some(2012)

scalaVersion := "2.13.1"

licenses := Seq("GNU GPLv3" -> url("http://www.gnu.org/licenses/gpl-3.0.html"))

libraryDependencies ++= Seq(
    "nz.ac.waikato.cms.weka" % "weka-stable" % "3.8.0",
    "com.scleradb" %% "sclera-config" % "4.0-SNAPSHOT" % "provided",
    "com.scleradb" %% "sclera-core" % "4.0-SNAPSHOT" % "provided"
)

scalacOptions ++= Seq(
    "-Werror", "-feature", "-deprecation", "-unchecked"
)

exportJars := true
