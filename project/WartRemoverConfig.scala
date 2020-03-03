import sbt.File

object WartRemoverConfig {

  def findSbtFiles(rootDir: File): Seq[File] = {
    if (rootDir.getName == "project") {
      rootDir.listFiles().toSeq
    } else {
      Seq()
    }
  }

  def findPlayConfFiles(rootDir: File): Seq[File] = {
    Option {
      new File(rootDir, "conf").listFiles()
    }.getOrElse(Array[File]()).toSeq
  }

  def makeExcludedFiles(rootDir: File): Seq[File] = {
    val excluded = findPlayConfFiles(rootDir) ++ findSbtFiles(rootDir)
    println(s"[auto-code-review] excluding the following files: ${excluded.mkString(",")}")
    excluded
  }
}