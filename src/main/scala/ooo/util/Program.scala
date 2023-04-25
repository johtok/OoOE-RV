package ooo.util

import chisel3._

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.util.Random

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

  def random(): Program = Program(
    "Random",
    Seq.fill(1024)(Random.nextInt(255))
  )

  def load(fileName: String): Program = {
    Program(
      fileName,
      Files.readAllBytes(Paths.get(fileName)).map(_ & 0xFF)
    )
  }
  def load(fileNames: Seq[String]): Seq[Program] = fileNames.map(load)
  def load(fileName: String, fileNames: String*): Seq[Program] = load(fileName +: fileNames)

  def getBinFiles(dir: String): Seq[String] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(f => f.isFile && f.getName.endsWith(".bin")).map(_.getName).toSeq
    } else {
      Seq()
    }
  }

}
