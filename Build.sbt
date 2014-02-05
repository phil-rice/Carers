
resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

name := "Carers"

version := "1.0"

scalaVersion := "2.10.1"

EclipseKeys.withSource := true

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "src/main/resources") }

libraryDependencies ++= Seq(
    "org.cddcore" %% "website" % "1.8.5.5",
    "org.cddcore" %% "legacy" % "1.8.5.5",
    "com.sun.jersey" % "jersey-server" % "1.2",
    "com.sun.jersey" % "jersey-json" % "1.2",
    "org.eclipse.jetty" % "jetty-server" % "8.0.0.M0",
    "org.eclipse.jetty" % "jetty-servlet" % "8.0.0.M0"
)

libraryDependencies += "com.novocode" % "junit-interface" % "0.9" % "test"

testOptions in Test += Tests.Argument("DemoTest$")  // Use HtmlReporter