val redis4catsVersion = "0.10.2"

lazy val `redis4cats-transaction` = (project in file("."))
  .settings(
    organization := "com.ikempf",
    name         := "redis4cats-transaction",
    scalaVersion := "2.13.1",
    libraryDependencies ++= List(
      "org.typelevel"     %% "cats-core"           % "2.1.0",
      "io.chrisdavenport" %% "log4cats-slf4j"      % "1.1.1",
      "dev.profunktor"    %% "redis4cats-effects"  % redis4catsVersion,
      "dev.profunktor"    %% "redis4cats-log4cats" % redis4catsVersion,
      "ch.qos.logback"     % "logback-classic"     % "1.2.3",
      "org.scalatest"     %% "scalatest"           % "3.1.0" % Test
    ),
    scalafmtOnCompile := true,
    addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full)),
    scalacOptions ++= List(
      "-target:11",
      "-feature",
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-deprecation",
      "-language:higherKinds",
      "-language:existentials",
      "-Wdead-code",
      "-Wvalue-discard",
      "-Wunused:imports",
      "-Wunused:patvars",
      "-Wunused:implicits",
      "-Wunused:locals",
      "-Wunused:explicits",
      "-Wunused:params",
      "-Wunused:privates",
      "-Woctal-literal",
      "-Xlint:adapted-args",
      "-Xlint:infer-any",
      "-Xlint:nullary-unit",
      "-Xlint:nullary-override",
      "-Xlint:inaccessible",
      "-Xlint:constant"
    ),
    javaOptions += "--illegal-access=warn"
  )
