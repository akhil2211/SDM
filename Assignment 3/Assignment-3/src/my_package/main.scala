 package my_package

import scala.io.Source
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import java.nio.charset.CodingErrorAction
import scala.io.Codec
import shapeless.ops.coproduct.ZipWithIndex

object main {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Friends Reco").setMaster("local[*]");
    val sc = new SparkContext(conf);
    
    implicit val codec = Codec("UTF-8");
    codec.onMalformedInput(CodingErrorAction.REPLACE);
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE);
    
    val raw_data = Source.fromFile("Data.txt")(codec).getLines().drop(1).mkString.trim;
    val para = raw_data.split("(?=\\t\\d{1,3}\\t)").map(lines => lines.trim.toLowerCase())
      .map(lines => lines.split("\\t")).filter(lines => lines.size == 2).map(line =>
        line(1).replaceAll("[^\\w\\s0-9]", ""));
    
    val para_indexed = para.toIndexedSeq.zipWithIndex;
    
    val lsh1 = new LSHash(5, 500, 10, para_indexed, .005);
    lsh1.createHash();
    val similar_doc = lsh1.findSimilar(para(0));
    
    similar_doc.foreach(ele => print(ele+" "));
    
  }
}