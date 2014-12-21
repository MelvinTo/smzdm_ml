name := "smzdm"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.1.1"

libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "2.2.0"

libraryDependencies += "org.mongodb" % "mongo-java-driver" % "2.11.4"

libraryDependencies += "org.mongodb" % "mongo-hadoop-core" % "1.3.0"

libraryDependencies += "org.mongodb" %% "casbah" % "2.7.4"

libraryDependencies += "rome" % "rome" % "1.0"

libraryDependencies  ++= Seq(
            // other dependencies here
            "org.scalanlp" %% "breeze" % "0.8.1",
            // native libraries are not included by default. add this if you want them (as of 0.7)
            // native libraries greatly improve performance, but increase jar sizes.
            "org.scalanlp" %% "breeze-natives" % "0.8.1"
)

resolvers ++= Seq(
            // other resolvers here
            // if you want to use snapshot builds (currently 0.8-SNAPSHOT), use this.
            "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
            "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

libraryDependencies += "org.scalanlp" % "nak" % "1.2.1"

libraryDependencies += "org.scalanlp" % "chalk" % "1.3.0"

libraryDependencies += "com.huaban" % "jieba-analysis" % "1.0.0"

libraryDependencies += "org.clapper" % "grizzled-slf4j_2.10" % "1.0.2"

libraryDependencies += "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.6.1"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.1"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.2" % "test"

libraryDependencies += "com.typesafe" % "config" % "1.2.1"

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"