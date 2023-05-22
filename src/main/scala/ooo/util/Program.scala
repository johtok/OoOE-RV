package ooo.util

import chisel3._

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.util.Random

case class Program(name: String, bytes: Seq[Int], result: Seq[BigInt]) {
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
    Seq.fill(1024)(Random.nextInt(255)),
    Seq.fill(32)(Random.nextInt(255))
  )

  def load(fileName: String): Program = {
    Program(
      fileName,
      loadBinary(fileName),
      loadResult(fileName.replace(".bin",".res"))
    )
  }
  def loadBinary(fileName: String): Seq[Int] = {
    val bin = Files.readAllBytes(Paths.get(fileName)).toIndexedSeq.map(_ & 0xFF)
    bin.padTo(bin.length + (bin.length % 4), 0)
  }
  def loadResult(fileName: String): Seq[BigInt] = {
    if(new File(fileName).exists()) {
      Files.readAllBytes(Paths.get(fileName))
        .toIndexedSeq
        .map(b => BigInt(b) & 0xFF)
        .grouped(4)
        .map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24))
        .toSeq
    } else Seq.fill(32)(BigInt(0))

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
