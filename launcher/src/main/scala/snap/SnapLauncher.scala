package snap

import xsbti.{ AppMain, AppConfiguration }
import snap.properties.SnapProperties
import java.io.File

/** Expose for SBT launcher support. */
class SnapLauncher extends AppMain {

  def run(configuration: AppConfiguration) =
    // TODO - Detect if we're running against a local project.
    try configuration.arguments match {
      case Array("ui") => RebootToUI(configuration)
      case Array("new") => Exit(SnapCli(configuration))
      case Array("shell") => RebootToSbt(configuration, useArguments = false)
      case _ if Sbt.looksLikeAProject(new File(".")) => RebootToSbt(configuration, useArguments = true)
      case _ => displayHelp(configuration)
    } catch {
      case e: Exception => generateErrorReport(e)
    }
  // Wrapper to return exit codes.
  case class Exit(val code: Int) extends xsbti.Exit

  def displayHelp(configuration: AppConfiguration) = {
    System.err.println("""| Warning:  Could not detect a local snap project.
                          |
                          | If you'd like to run snap in this directory, please:
                          |
                          | 1. Run the UI with `snap ui`
                          | 2. Create a project with `snap create`
                          | 3. Move into a snap project directory and re-run snap.
                          |""".stripMargin)
    Exit(1)
  }

  def generateErrorReport(e: Exception) = {
    // TODO - Make a real error report.
    e.printStackTrace()
    Exit(2)
  }
}
// Wrapper to return the UI application.
// TODO - Generate this via SBT code, so the hard-coded settings come
// from the build.
case class RebootToUI(configuration: AppConfiguration) extends xsbti.Reboot {
  val arguments = Array.empty[String]
  val baseDirectory = configuration.baseDirectory
  val scalaVersion = SnapProperties.APP_SCALA_VERSION
  val app = ApplicationID(
    groupID = configuration.provider.id.groupID,
    name = "snap-ui",
    version = SnapProperties.APP_VERSION,
    mainClass = "snap.UIMain")
}

// Wrapper to reboot into SBT.
// TODO - Generate this via the SBT build code, so the hardcoded SBT version
// lives in one spot.
// OR we can even detect the SBT version...
case class RebootToSbt(configuration: AppConfiguration, useArguments: Boolean = false) extends xsbti.Reboot {
  val arguments = if (useArguments) configuration.arguments else Array.empty[String]
  val baseDirectory = configuration.baseDirectory
  val scalaVersion = SnapProperties.SBT_SCALA_VERSION
  val app = ApplicationID(
    groupID = "org.scala-sbt",
    name = "sbt",
    version = SnapProperties.SBT_VERSION,
    mainClass = "sbt.xMain",
    mainComponents = Array("xsbti", "extra"))
}

// Helper class to make using ApplicationID in xsbti easier.
case class ApplicationID(
  groupID: String,
  name: String,
  version: String,
  mainClass: String,
  mainComponents: Array[String] = Array("xsbti"),
  crossVersioned: Boolean = false,
  classpathExtra: Array[java.io.File] = Array.empty) extends xsbti.ApplicationID
