val catsVersion = "2.0.0"
val catsEffectVersion = "2.0.0"
val utilVersion = "19.9.0"
val finagleVersion = "19.9.0"

organization in ThisBuild := "io.catbird"

def compilerOptions(scalaVersion: String): Seq[String] = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Yno-imports",
  "-Yno-predef"
) ++ (if (priorTo2_13(scalaVersion))
        Seq(
          "-Ywarn-unused-import",
          "-Yno-adapted-args",
          "-Xfuture"
        )
      else
        Seq(
          "-Ywarn-unused:imports",
          "-Ymacro-annotations"
        ))

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val baseSettings = Seq(
  crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.0"),
  scalacOptions ++= compilerOptions(scalaVersion.value),
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-imports", "-Yno-predef"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-imports", "-Yno-predef"))
  },
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
    "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    "org.typelevel" %% "cats-laws" % catsVersion % Test,
    "org.typelevel" %% "discipline-core" % "1.0.0" % Test,
    "org.typelevel" %% "discipline-scalatest" % "1.0.0-M1" % Test,
    compilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
  ),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  docMappingsApiDir := "api"
)

lazy val allSettings = baseSettings ++ publishSettings

lazy val root = project
  .in(file("."))
  .enablePlugins(GhpagesPlugin, ScalaUnidocPlugin)
  .settings(allSettings ++ noPublishSettings)
  .settings(
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmark),
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), docMappingsApiDir),
    git.remoteRepo := "git@github.com:travisbrown/catbird.git"
  )
  .settings(
    initialCommands in console :=
      """
        |import com.twitter.finagle._
        |import com.twitter.util._
        |import io.catbird.finagle._
        |import io.catbird.util._
      """.stripMargin
  )
  .aggregate(util, effect, finagle, benchmark)
  .dependsOn(util, effect, finagle)

lazy val util = project
  .settings(moduleName := "catbird-util")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "util-core" % utilVersion,
    scalacOptions in Test ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )

lazy val effect = project
  .settings(moduleName := "catbird-effect")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "cats-effect-laws" % catsEffectVersion % Test
    ),
    scalacOptions in Test ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .dependsOn(util, util % "test->test")

lazy val finagle = project
  .settings(moduleName := "catbird-finagle")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "finagle-core" % finagleVersion,
    scalacOptions in Test ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .dependsOn(util)

lazy val benchmark = project
  .settings(moduleName := "catbird-benchmark")
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8",
    scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(util)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/travisbrown/catbird")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://travisbrown.github.io/catbird/api/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/travisbrown/catbird"),
      "scm:git:git@github.com:travisbrown/catbird.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
        <url>https://twitter.com/travisbrown</url>
      </developer>
    </developers>
  )
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }
