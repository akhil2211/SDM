package my_package

import org.apache.spark.mllib.clustering.PowerIterationClustering
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import java.io._

class BookCluster(data: RDD[(Long, Long, Double)], num_clusters: Int, n_iteration: Int, sc: SparkContext){
  val model = new PowerIterationClustering()
  .setK(num_clusters).setMaxIterations(n_iteration).setInitializationMode("degree").run(data);

  val clusters = model.assignments.collect().groupBy(_.cluster).mapValues(_.map(_.id));
  val assignments = clusters.toList.sortBy { case (k, v) => v.length }.map(ele => (ele._1+1, ele._2.map(e => e+1)));
  val assignmentsStr = "Book"+assignments.map { case (k, v) => s"$k: ${v.sorted.mkString(",")}"}.mkString("\nBook");
  val sizesStr = "\nCluster Sizes"+assignments.map {_._2.length}.sorted.mkString("(", ",", ")");
  
  val pw = new PrintWriter(new File("output_ques_1.txt"));
  pw.write(assignmentsStr.concat(sizesStr));
  pw.close;
  
  //sc.parallelize(assignmentsStr.concat(sizesStr), 1).saveAsTextFile("output_ques_1");
  
  //println(s"Books: $assignmentsStr cluster sizes: $sizesStr");
}