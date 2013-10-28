import com.typesafe.startscript.StartScriptPlugin

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "Carers"

version := "1.0"

scalaVersion := "2.10.1"

unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "src/main/resources") }

libraryDependencies ++= Seq(
    "org.cddcore" %% "website" % "1.6.3-SNAPSHOT",
    "com.sun.jersey" % "jersey-server" % "1.2",
    "com.sun.jersey" % "jersey-json" % "1.2",
    "org.eclipse.jetty" % "jetty-server" % "8.0.0.M0",
    "org.eclipse.jetty" % "jetty-servlet" % "8.0.0.M0"
)