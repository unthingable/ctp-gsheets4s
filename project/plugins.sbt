resolvers += Resolver.bintrayIvyRepo("rallyhealth", "sbt-plugins")
addSbtPlugin("com.rallyhealth.sbt" % "sbt-git-versioning" % "1.6.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.6")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.28")

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)