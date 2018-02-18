import sbtcrossproject.{crossProject, CrossType}
import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys.scalacOptions
import sbt.addCompilerPlugin

name               in ThisBuild := "utest"
organization       in ThisBuild := "com.github.xenoby"
scalaVersion       in ThisBuild := "2.11.12"
crossScalaVersions in ThisBuild := Seq("2.10.6", "2.11.11", "2.12.2", "2.13.0-M2")
updateOptions      in ThisBuild := (updateOptions in ThisBuild).value.withCachedResolution(true)
incOptions         in ThisBuild := (incOptions in ThisBuild).value.withNameHashing(true).withLogRecompileOnMacro(false)
//triggeredMessage   in ThisBuild := Watched.clearWhenTriggered
releaseTagComment  in ThisBuild := s"v${(version in ThisBuild).value}"
releaseVcsSign     in ThisBuild := true

lazy val utest = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    name                  := "utest",
    scalacOptions         := Seq("-Ywarn-dead-code"),
    scalacOptions in Test -= "-Ywarn-dead-code",
    libraryDependencies  ++= macroDependencies(scalaVersion.value),
    scalacOptions        ++= (scalaVersion.value match {
      case x if x startsWith "2.13." => "-target:jvm-1.8" :: Nil
      case x if x startsWith "2.12." => "-target:jvm-1.8" :: "-opt:l:method" :: Nil
      case x if x startsWith "2.11." => "-target:jvm-1.6" :: Nil
      case x if x startsWith "2.10." => "-target:jvm-1.6" :: Nil
    }),

    unmanagedSourceDirectories in Compile += {
      val v = if (scalaVersion.value startsWith "2.10.") "scala-2.10" else "scala-2.11"
      baseDirectory.value/".."/"shared"/"src"/"main"/v
    },
    testFrameworks += new TestFramework("test.utest.CustomFramework"),

    // Release settings
    releasePublishArtifactsAction := publishSigned.value,
    publishArtifact in Test := false,
    publishTo := Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
    resolvers += "Sonatype staging" at "https://oss.sonatype.org/content/repositories/staging",
    credentials ++= {
      lazy val credentials = sys.props("credentials")
      val credentialsFile = if (credentials != null) new File(credentials) else null
      if (credentialsFile != null) List(new FileCredentials(credentialsFile))
      else Nil
    },
    homepage := Some(url("https://github.com/lihaoyi/utest")),
    scmInfo := Some(ScmInfo(
      browseUrl = url("https://github.com/lihaoyi/utest"),
      connection = "scm:git:git@github.com:lihaoyi/utest.git"
    )),
    licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/mit-license.html")),
    developers += Developer(
      email = "haoyi.sg@gmail.com",
      id = "lihaoyi",
      name = "Li Haoyi",
      url = url("https://github.com/lihaoyi")
    )//,
//    autoCompilerPlugins := true,
//
//    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.7"),
//
//    scalacOptions += "-P:acyclic:force"

)
  .jsSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % "1.0",
      "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
    ),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
  .nativeSettings(
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12"),
    libraryDependencies ++= Seq(
      "com.github.xenoby" %%% "test-interface" % "0.3.6-20-g0afae98f36"
    ),
    nativeLinkStubs := true
  )

def macroDependencies(version: String) =
  ("org.scala-lang" % "scala-reflect" % version) +:
  (if (version startsWith "2.10.")
     Seq(compilerPlugin("org.scalamacros" % s"paradise" % "2.1.0" cross CrossVersion.full),
         "org.scalamacros" %% s"quasiquotes" % "2.1.0")
   else
     Seq())

lazy val utestJS = utest.js
lazy val utestJVM = utest.jvm
lazy val utestNative = utest.native

lazy val root = project.in(file("."))
  .aggregate(utestJS, utestJVM, utestNative)
  .settings(
    publishTo := Some(Resolver.file("Unused transient repository", target.value / "fakepublish")),
    publishArtifact := false,
    publishLocal := (),
    publishLocalSigned := (),       // doesn't work
    publishSigned := (),            // doesn't work
    packagedArtifacts := Map.empty) // doesn't work - https://github.com/sbt/sbt-pgp/issues/42

