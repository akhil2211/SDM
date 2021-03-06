package my_package

class LSHash(shingleLength: Int = 5, minHashLength: Int = 1000, numberBands: Int=5,
  processedDocuments: IndexedSeq[(String, Int)], threshold: Double=0){
  
  val randomHashFunctions = randomLinearHashFunction(minHashLength);

  val documentShingles: Map[Int, Set[String]] = processedDocuments.map { document =>
    val shingles = document._1.toList.sliding(shingleLength, 1).map(_.mkString).toSet;
    (document._2, shingles);
  }.toMap

  val shingleVocab = documentShingles.values.flatten.toSet.toIndexedSeq.zipWithIndex.toMap

  var mBands: IndexedSeq[Band] = null;

  private def randomLinearHashFunction(n: Int) = {
    val slope = scala.util.Random.shuffle(0 to 1000);
    val const = scala.util.Random.shuffle(0 to 1000);
    slope.zip(const).take(minHashLength);
  }

  def findCandidates(shingles: Set[String]) = {
    val minHash = getMinHash(shingles);

    val subArrays = partitionArray(minHash).zipWithIndex;

    val candidates = subArrays.flatMap { subArray =>
      val index = subArray._2;
      val hashedBucket = mBands(index).getCollisionObjects(subArray._1)
      hashedBucket
    }.flatten.toSet

    candidates
  }

  def findSimilar(document: String) = {
    val shingles = document.toList.sliding(shingleLength, 1)
    .map(_.mkString)
    .map(shingle => shingle.toLowerCase)
    .toSet;

    val candidates = findCandidates(shingles);
    candidates.filter(candidate => JaccardSimilarity(shingles, documentShingles(candidate.toInt)) >= threshold)
      .map(ele => (ele, JaccardSimilarity(shingles, documentShingles(ele.toInt))));
  }
  
  def getMinHash(shingles: Set[String]) = {

    val minHash = Array.fill[Double](minHashLength)(Double.PositiveInfinity);

    shingles.filter(x => shingleVocab.contains(x))
      .foreach { shingle =>
        val shingleIndex = shingleVocab(shingle);
        var hashIndex = 0;
        randomHashFunctions.foreach { function =>
          val permutedIndex = (function._1 * shingleIndex + function._2) % shingleVocab.size

          if (minHash(hashIndex) > permutedIndex)
            minHash(hashIndex) = permutedIndex

          hashIndex += 1;
        }
      }
    minHash
  }
  
  def partitionArray(minHash: Array[Double]): IndexedSeq[Array[Double]] = {

    if (minHash.length < numberBands) {
      println("number of bands exceeds minHash")
      System.exit(0);
    }

    val elementsPerBand = (minHash.length / numberBands);
    (0 to numberBands - 1).map { bandIndex =>
      val start = bandIndex * elementsPerBand
      val end = start + elementsPerBand;
      minHash.slice(start, end);
    }
  }

  def createHash() = {

    val minHashCollection = documentShingles.mapValues(shingleSet => getMinHash(shingleSet))
    val bands =
      (0 to numberBands - 1).map { bandIndex =>
        val elementsPerBand = (1.0 * minHashLength / numberBands).ceil.toInt
        val start = bandIndex * elementsPerBand
        val end = if (bandIndex == numberBands - 1) minHashLength else start + elementsPerBand;
        val subArray = minHashCollection.map(document => (document._1, document._2.slice(start, end)))
        val band = new Band()
        subArray.foreach(array => band.hash(array))
        band
      }
    mBands = bands;
  }
}

class Band() {
  import scala.collection.mutable.ArrayBuffer
  val buckets = scala.collection.mutable.Map[List[Double], ArrayBuffer[Int]]()

  def hash(subArray: (Int, Array[Double])) {
    val tr = buckets.get(subArray._2.toList) match {
      case Some(value) => value += subArray._1;
      case None => buckets(subArray._2.toList) = ArrayBuffer(subArray._1)
    }
  }

  def getCollisionObjects(subArray: Array[Double]): Option[List[Int]] = {
    buckets.get(subArray.toList) match {
      case Some(value) => Some(value.toList);
      case None => buckets(subArray.toList) = ArrayBuffer(-1); None
    }
  }
}

object JaccardSimilarity {
  def apply(set1: Set[String], set2: Set[String]): Double = {
    val intersection = set1.intersect(set2).size
    val union = set2.union(set2).size

    return (intersection * 1.0) / union
  }
}