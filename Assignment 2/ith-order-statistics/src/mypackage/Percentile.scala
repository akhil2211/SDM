package mypackage

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import scala.io.Source


object Percentile {
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "C:\\hadoop");
    
    val spConfig = (new SparkConf).setAppName("Spark Matrix Multiplication").setMaster("local[*]");
    val sc = new SparkContext(spConfig);
    val listOfLines = Source.fromFile("input_list.txt").getLines.toArray;
    val ls = listOfLines.map(_.toInt);
    
    val list = sc.parallelize(ls, 3).cache();
    
    val sorted = list.sortBy(x=> x);
    val indexed = sorted.zipWithIndex().map(x => x.swap);
    val percentiles = List(.25, .5, 1);
    
    var value = new Array[Int](percentiles.size);
    var i = 0;
    percentiles.foreach{x =>
      val id = (indexed.count()*x).toLong;
      value(i) = indexed.lookup(id-1)(0);
      i = i+1;
    }
    sc.stop();
    println(value.mkString(","));
  }
}