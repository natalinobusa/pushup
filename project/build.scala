// 2014, Natalino Busa 
// http://www.linkedin.com/in/natalinobusa

import sbt._
import Keys._
import com.typesafe.sbt.SbtAtmos.{ Atmos, atmosSettings }

object BuildSettings {
  val buildOrganization = "http://natalinobusa.com"
  val buildVersion      = "1.0.0"
  val buildScalaVersion = "2.10.4"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization  := buildOrganization,
    version       := buildVersion,
    scalaVersion  := buildScalaVersion,
    shellPrompt   := ShellPrompt.buildShellPrompt,
    scalacOptions := Seq("-deprecation", "-feature", "-encoding", "utf8")
  )
}

// Shell prompt which show the current project, 
// git branch and build version
object ShellPrompt {
  object devnull extends ProcessLogger {
    def info (s: => String) {}
    def error (s: => String) { }
    def buffer[T] (f: => T): T = f
  }
  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
      getOrElse "-" stripPrefix "## "
  )

  val buildShellPrompt = { 
    (state: State) => {
      val currProject = Project.extract (state).currentProject.id
      "%s:%s:%s> ".format (
        currProject, currBranch, BuildSettings.buildVersion
      )
    }
  }
}

object Resolvers {
  val allResolvers = Seq (
    "Spray Repository"        at "http://repo.spray.io",
    "Spray Nightlies Repo"    at "http://nightlies.spray.io",
    "Typesafe Repository"     at "http://repo.typesafe.com/typesafe/releases/"
  )
}

object Dependencies {
  val akkaVersion       = "2.2.4"
  val sprayVersion      = "1.2.1"

  val allDependencies = Seq(
    //spray
    "io.spray"           % "spray-can"     % sprayVersion,
    "io.spray"           % "spray-io"      % sprayVersion,
    "io.spray"           % "spray-httpx"   % sprayVersion,
    "io.spray"           % "spray-routing" % sprayVersion,

    // spray client
    "io.spray"           % "spray-util"    % sprayVersion,
    "io.spray"           % "spray-http"    % sprayVersion,
    "io.spray"           % "spray-client"  % sprayVersion,

    //spray-json
    "io.spray"           %% "spray-json"   % "1.2.6",

    
    //akka
    "com.typesafe.akka" %% "akka-actor"    % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit"  % akkaVersion,
    
    //testing
    "org.scalatest"     % "scalatest_2.10" % "2.2.1" % "test"
  )
}

object DefaultBuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  val appName = "pushup"

  lazy val root = Project (
    id = appName,
    base = file ("."),
    settings = buildSettings ++ Seq (resolvers ++= allResolvers, libraryDependencies ++= allDependencies)
  ) 
  .configs(Atmos)
  .settings(atmosSettings: _*)

}
