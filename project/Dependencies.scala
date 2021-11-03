import sbt._
import Keys._
import Versions._

object Dependencies {
  val zio = "dev.zio" %% "zio" % zioVersion
  val zioPrelude = "dev.zio" %% "zio-prelude" % zioPreludeVersion
  val zioConfig = "dev.zio" %% "zio-config" % zioConfigVersion
  val zioConfigRefined = "dev.zio" %% "zio-config-refined" % zioConfigVersion

  // Scalafix rules
  val organizeImports = "com.github.liancheng" %% "organize-imports" % organizeImportsVersion

  def derevo(artifact: String): ModuleID    = "tf.tofu"           %% s"derevo-$artifact"    % derevoVersion
  def circe(artifact: String): ModuleID     = "io.circe"          %% s"circe-$artifact"     % circeVersion
  def http4s(artifact: String): ModuleID    = "org.http4s"        %% s"http4s-$artifact"    % http4sVersion
  def cormorant(artifact: String): ModuleID = "io.chrisdavenport" %% s"cormorant-$artifact" % cormorantVersion

  object Misc {
    val newtype = "io.estatico"   %% "newtype"  % newtypeVersion
    val squants = "org.typelevel" %% "squants"  % squantsVersion
    val fs2Core = "co.fs2"        %% "fs2-core" % fs2Version
    val fs2IO   = "co.fs2"        %% "fs2-io"   % fs2Version
  }

  object Refined {
    val refinedCore      = "eu.timepit" %% "refined"           % refinedVersion
    val refinedCats      = "eu.timepit" %% "refined-cats"      % refinedVersion
    val refinedShapeless = "eu.timepit" %% "refined-shapeless" % refinedVersion
  }

  object Circe {
    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
    val circeRefined = circe("refined")
  }

  object Derevo {
    val derevoCore          = derevo("core")
    val derevoCiris         = derevo("ciris")
    val derevoCirce         = derevo("circe")
    val derevoCirceMagnolia = derevo("circe-magnolia")
  }

  object Ciris {
    val cirisCore    = "is.cir" %% "ciris"            % cirisVersion
    val cirisEnum    = "is.cir" %% "ciris-enumeratum" % cirisVersion
    val cirisRefined = "is.cir" %% "ciris-refined"    % cirisVersion
    val cirisCirce   = "is.cir" %% "ciris-circe"      % cirisVersion
    val cirisSquants = "is.cir" %% "ciris-squants"    % cirisVersion
  }

  // Runtime
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime

  object CompilerPlugin {
    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % betterMonadicForVersion
    )
    val kindProjector = compilerPlugin(
      "org.typelevel" %% "kind-projector" % kindProjectorVersion cross CrossVersion.full
    )
    val semanticDB = compilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % semanticDBVersion cross CrossVersion.full
    )
  }

  import CompilerPlugin._

  val commonDependencies: Seq[ModuleID] = Seq(zio, zioPrelude, zioConfigRefined, zioConfig)

  val tradeioDependencies: Seq[ModuleID] =
    commonDependencies ++ Seq(kindProjector, betterMonadicFor, semanticDB) ++
      Seq(Misc.newtype, Misc.squants) ++
      Seq(Derevo.derevoCore, Derevo.derevoCiris, Derevo.derevoCirceMagnolia) ++
      Seq(Refined.refinedCore, Refined.refinedCats, Refined.refinedShapeless) ++
      Seq(Ciris.cirisCore, Ciris.cirisEnum, Ciris.cirisRefined, Ciris.cirisCirce, Ciris.cirisSquants) ++
      Seq(logback) ++
      Seq(Circe.circeCore, Circe.circeGeneric, Circe.circeParser, Circe.circeRefined)
}
