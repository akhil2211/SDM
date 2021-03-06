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
    
    val lsh = new LSHash(5, 120, 60, para_indexed, 0.1);
    lsh.createHash();
    val similar_doc = para_indexed.map(p => (lsh.findSimilar(p._1).toArray, p._2)).flatMap{ 
      ele => ele._1.map(e => (ele._2.toLong, e._1.toLong, e._2)) }.filter(ele => ele._1 != ele._2);
    
    //similar_doc.foreach(ele => println("("+ele._1+","+ele._2+","+ele._3+")"));
    val clster_map = sc.parallelize(similar_doc, 3).cache();
    val cl = new BookCluster(clster_map, 2, 5, sc);
    
    val top_para = clster_map.collect().sortWith(_._3 > _._3).take(5)//.map(ele => (ele._1, ele._2));
    val lsh_similar = new OverlapRegion(5, para_indexed);
    
    //top_para.foreach(ele => println(ele._1 + "," + ele._2 + " ," + ele._3));
    
    var overlap_list = List[(Int, Int, List[(String, Int, Int)])]();
    
    top_para.foreach{ index => 
      overlap_list = overlap_list ++ List((index._1.toInt, index._2.toInt, 
          lsh_similar.findOverlap(para(index._1.toInt), para(index._2.toInt)))) };
    
    var final_list = List[String]();
          
    overlap_list.foreach(ele => ele._3.foreach{x => 
      final_list = final_list ++ List(("<para"+(ele._1+1)+"-para"+(ele._2+1)+"> "+"<"+x._1+"> "+"<"+x._2+"-"+x._3+">").mkString)});
    sc.parallelize(final_list,1).saveAsTextFile("output_ques_2");
    }
}