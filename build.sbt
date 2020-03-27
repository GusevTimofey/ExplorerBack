name := "ExplorerBack"

version := "0.1"

scalaVersion := "2.12.11"

val doobieVersion      = "0.8.8"
val fs2Version         = "2.1.0"
val catsVersion        = "2.0.0"
val catsEffectsVersion = "2.0.0"
val http4sVersion      = "0.21.0"
val circeVersion       = "0.12.3"
val simulacrumVersion  = "0.19.0"
val catsRetryVersion   = "1.1.0"

val doobie: Seq[ModuleID] = Seq(
  "org.tpolecat" %% "doobie-core"      % doobieVersion,
  "org.tpolecat" %% "doobie-postgres"  % doobieVersion,
  "org.tpolecat" %% "doobie-hikari"    % doobieVersion,
  "org.tpolecat" %% "doobie-refined"   % doobieVersion,
  "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test
)

val circe: Seq[ModuleID] = Seq(
  "io.circe" %% "circe-core"    % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser"  % circeVersion,
  "io.circe" %% "circe-refined" % circeVersion
)

val cats: Seq[ModuleID] = Seq(
  "org.typelevel" %% "cats-core"   % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectsVersion,
  "co.fs2"        %% "fs2-core"    % fs2Version,
  "co.fs2"        %% "fs2-io"      % fs2Version
)

val http4s: Seq[ModuleID] = Seq(
  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe"        % http4sVersion
)

libraryDependencies ++= Seq(
  "io.chrisdavenport"     %% "log4cats-slf4j"         % "0.4.0-M2",
  "io.estatico"           %% "newtype"                % "0.4.3",
  "com.github.pureconfig" %% "pureconfig"             % "0.12.2",
  "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.12.2",
  "com.google.guava"      % "guava"                   % "28.2-jre",
  "org.encry"             %% "encry-common"           % "0.9.3",
  "com.github.mpilquist"  %% "simulacrum"             % simulacrumVersion,
  "eu.timepit"            %% "refined"                % "0.9.13",
  "com.github.cb372"      %% "cats-retry"             % catsRetryVersion,
  "org.scalatest"         %% "scalatest"              % "3.1.0" % Test
) ++ doobie ++ cats ++ http4s ++ circe

scalacOptions ++= Seq(
  "-language:postfixOps",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Ypartial-unification"
)

addCompilerPlugin("com.olegpy"      %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel"   %% "kind-projector"     % "0.11.0" cross CrossVersion.patch)
addCompilerPlugin("org.scalamacros" % "paradise"            % "2.1.1" cross CrossVersion.full)
