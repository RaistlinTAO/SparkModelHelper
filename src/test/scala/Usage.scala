package io.github.RaistlinTao

import org.apache.spark.ml.classification.DecisionTreeClassificationModel
import org.apache.spark.sql.SparkSession

object Usage {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local").getOrCreate()
    //Disable debug INFO, only show ERROR Messages
    spark.sparkContext.setLogLevel("ERROR")
    println("SparkSession Loaded\n")
    val model = DecisionTreeClassificationModel.load("sample/ClassificationModel")
    println("toDebugString:" + model.toDebugString + "\n")
    val helper = new DecisionModelHelper(model)
    helper.setFeatureName(
      Map(0 -> "F0", 1 -> "F1", 2 -> "F0", 3 -> "F1", 4 -> "F0", 5 -> "F1", 6 -> "F0", 7 -> "F1", 8 -> "F0", 9 -> "F1",
        10 -> "DSF", 11 -> "ZCX", 12 -> "CSD", 13 -> "GF", 14 -> "F0", 15 -> "F1", 16 -> "F0", 17 -> "F1", 18 -> "F0", 19 -> "F1",
        20 -> "SC", 21 -> "CZX", 22 -> "KJL", 23 -> "GF", 24 -> "FG", 25 -> "F1", 26 -> "FFG0", 27 -> "GF1", 28 -> "FM0", 29 -> "FV1",
        30 -> "CZX", 31 -> "XCZ", 32 -> "HGJ", 33 -> "BB", 34 -> "F0", 35 -> "F1", 36 -> "F0", 37 -> "FA1", 38 -> "F0V", 39 -> "FV1",
        40 -> "XZ", 41 -> "XC", 42 -> "HGJ", 43 -> "BB", 44 -> "F0", 45 -> "F1", 46 -> "F0D", 47 -> "F1A", 48 -> "FV0", 49 -> "FV1",
        50 -> "CZ", 51 -> "CX", 52 -> "HN", 53 -> "GG", 54 -> "F0", 55 -> "F1", 56 -> "F0", 57 -> "F1"
      )
    )
    val jsonStr = helper.toJson
    println("toJson: " + jsonStr + "\n")
    val nodeObj = helper.getDecisionNode
    println("getDecisionNode: " + nodeObj + "\n")
    val rules = helper.getRulesList(1, 0.2)
    rules.foreach(rule => {
      println("Rule: " + rule.mkString(","))
    })
  }
}
