import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._
import scoverage.ScoverageSbtPlugin.ScoverageKeys._
import com.typesafe.sbt.SbtGhPages.ghpages
import com.typesafe.sbt.SbtSite.site
import sbtunidoc.Plugin.UnidocKeys._
import sbtunidoc.Plugin._
import com.typesafe.sbt.SbtGit.{GitKeys => git}

object ProvideBuild extends Build {

  lazy val theVersion = "0.0.1-SNAPSHOT"
  lazy val theScalaVersion = "2.11.6"
  lazy val scalaVersions = Seq("2.11.6")
  lazy val theParadiseVersion = "2.1.0-M5"

  lazy val root = Project(id = "provide-root", base = file("."), settings = commonWithPublishSettings)
    .settings(
      name := "provide-root",
      crossScalaVersions := scalaVersions,
      crossVersion := CrossVersion.binary
    )
    .settings(unidocSettings: _*)
    .settings(site.settings ++ ghpages.settings: _*)
    .settings(
      site.addMappingsToSiteDir(
        mappings in (ScalaUnidoc, packageDoc), "latest/api"
      ),
      git.gitRemoteRepo := "git@github.com:lloydmeta/provide.git"
    )
    .settings(
      scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-no-expand",
      // Do not publish the root project (it just serves as an aggregate)
      publishArtifact := false,
      publishLocal := {}
    )
    .aggregate(macros, core)

  lazy val core = Project(id = "provide", base = file("provide-core"), settings = commonWithPublishSettings)
    .settings(
      name := "provide",
      crossScalaVersions := scalaVersions,
      crossVersion := CrossVersion.binary,
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "2.2.1" % Test,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % Test
      )
    ).dependsOn(macros)

  lazy val macros = Project(id = "provide-macros", base = file("macros"), settings = commonWithPublishSettings)
    .settings(
      name := "provide-macros",
      crossScalaVersions := scalaVersions,
      crossVersion := CrossVersion.binary,
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "org.scalatest" %% "scalatest" % "2.2.1" % "test"
      )
    )

  lazy val commonSettings = Seq(
    organization := "com.beachape",
    version := theVersion,
    scalaVersion := theScalaVersion,
    addCompilerPlugin("org.scalamacros" % "paradise" % theParadiseVersion cross CrossVersion.full)
  ) ++
    scalariformSettings ++
    scoverageSettings ++
    formatterPrefs ++
    compilerSettings ++
    resolverSettings ++
    ideSettings ++
    testSettings

  lazy val formatterPrefs = Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentClassDeclaration, true)
  )

  lazy val commonWithPublishSettings =
    commonSettings ++
      publishSettings

  lazy val resolverSettings = Seq(
    resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  )

  lazy val ideSettings = Seq(
    // Faster "sbt gen-idea"
    transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)
  )

  lazy val compilerSettings = Seq(
    // the name-hashing algorithm for the incremental compiler.
    incOptions := incOptions.value.withNameHashing(nameHashing = true),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xlint", "-Xlog-free-terms")
  )

  lazy val testSettings = Seq(Test).flatMap { t =>
    Seq(parallelExecution in t := false) // Avoid DB-related tests stomping on each other
  }

  lazy val scoverageSettings = Seq(
    coverageExcludedPackages := """com\.beachape\.annotations\.ProvideMacro""",
    coverageHighlighting := true
  )

  // Settings for publishing to Maven Central
  lazy val publishSettings = Seq(
    pomExtra :=
      <url>https://github.com/lloydmeta/provide</url>
        <licenses>
          <license>
            <name>MIT</name>
            <url>http://opensource.org/licenses/MIT</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:lloydmeta/provide.git</url>
          <connection>scm:git:git@github.com:lloydmeta/provide.git</connection>
        </scm>
        <developers>
          <developer>
            <id>lloydmeta</id>
            <name>Lloyd Chan</name>
            <url>http://lloydmeta.github.io</url>
          </developer>
        </developers>,
    publishTo <<= version { v =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false }
  )


}
