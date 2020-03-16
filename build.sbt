name := "sclera-plugin-weka"

description := "Add-on that enables Sclera to perform machine learning using the WEKA library from within SQL"

homepage := Some(url(s"https://github.com/scleradb/${name.value}"))

scmInfo := Some(
    ScmInfo(
        url(s"https://github.com/scleradb/${name.value}"),
        s"scm:git@github.com:scleradb/${name.value}.git"
    )
)

version := "4.0-SNAPSHOT"

startYear := Some(2012)

scalaVersion := "2.13.1"

licenses := Seq("GNU GPLv3" -> url("http://www.gnu.org/licenses/gpl-3.0.html"))

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
    "nz.ac.waikato.cms.weka" % "weka-stable" % "3.8.4",
    "com.scleradb" %% "sclera-config" % "4.0-SNAPSHOT" % "provided",
    "com.scleradb" %% "sclera-core" % "4.0-SNAPSHOT" % "provided"
)

scalacOptions ++= Seq(
    "-Werror", "-feature", "-deprecation", "-unchecked"
)

exportJars := true
