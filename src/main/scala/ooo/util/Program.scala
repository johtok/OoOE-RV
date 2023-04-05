package ooo.util

import chisel3._

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

case class Program(name: String, bytes: Seq[Int]) {
  def getBytes: Seq[UInt] = bytes.map(_.U(8.W))
  def getWords: Seq[UInt] = bytes
    .map(BigInt(_))
    .grouped(4)
    .map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24))
    .map(_.U(32.W))
    .toSeq
}
object Program {

  def load(fileName: String): Program = {
    Program(
      fileName,
      Files.readAllBytes(Paths.get(fileName)).map(_ & 0xFF)
    )
  }
  def load(fileNames: Seq[String]): Seq[Program] = fileNames.map(load)
  def load(fileName: String, fileNames: String*): Seq[Program] = load(fileName +: fileNames)

  private def getFiles(dir: String): Seq[String] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).map(_.getName).toSeq
    } else {
      Seq()
    }
  }

}