package my_package

import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vectors
import shapeless.LowPriority.For

class BookCluster(data: Seq[Seq[Double]], num_clusters: Int, num_iteration: Int){
  def kmeansFunction(sc:SparkContext):Unit = {    
    
    val rdd = sc.parallelize(data);
    val vec = rdd.map(row => Vectors.dense(row.toArray));
    val x_clusters = KMeans.train(vec, 2, 5);
    val n_wsse = x_clusters.computeCost(vec);
    
    //x_clusters.clusterCenters.foreach(println);
    
    val l_cluster_assignment = x_clusters.predict(vec);
    
    l_cluster_assignment.collect.foreach(print);
    
    val book1 = l_cluster_assignment.zipWithIndex.filter(elem => elem._1 != 1).map(x => x._2).collect.mkString(",");
    val book2 = l_cluster_assignment.zipWithIndex.filter(elem => elem._1 == 1).map(x => x._2).collect.mkString(",")
    
    val string1 = "Book1: "+book1;
    val string2 = "Book2: "+book2;
    
    val string = string1+ "\\n"+string2;
    
    sc.parallelize(string, 1).saveAsTextFile("output")
    
    sc.stop();
  }
}