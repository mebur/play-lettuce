name := "play-lettuce"

version := "1.1.0"

licenses += "BSD New" -> url("https://opensource.org/licenses/BSD-3-Clause")

scalaVersion := "2.13.1"
crossScalaVersions := Seq("2.12.7", "2.13.1")

libraryDependencies += "io.lettuce" % "lettuce-core" % "5.0.4.RELEASE"
libraryDependencies += "com.typesafe.play" %% "play" % "2.8.0" % "provided"
libraryDependencies += "com.typesafe.play" %% "play-cache" % "2.8.0" % "provided"
libraryDependencies += "com.typesafe.play" %% "play-test" % "2.8.0" % "provided"

autoAPIMappings := true
fork := true
javaOptions in test += "-XX:MaxMetaspaceSize=512m"
