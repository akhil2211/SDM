package my_package

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

class LSH(shingleLength: Int, processedDocuments: IndexedSeq[(String, Int)], sc:SparkContext) {

  val documentShingles: Map[Int, Set[String]] = processedDocuments.map {document =>
    val shingles = document._1.toList.sliding(shingleLength, 1).map(_.mkString).toSet
    (document._2, shingles)}.toMap

  val shingleVocab = documentShingles.values.flatten.toSet.toIndexedSeq.zipWithIndex.toMap
  
  val data = documentShingles.map{ elem => 
    val arr = new Array[Double](shingleVocab.size);
    shingleVocab.foreach{ text =>
      if(elem._2.contains(text._1))
        arr(text._2) = 1;
      else
        arr(text._2) = 0;
    }
    arr.toSeq;
  }.toSeq;
  
  val clusters = new BookCluster(data, 2, 20);
  clusters.kmeansFunction(sc);
}


