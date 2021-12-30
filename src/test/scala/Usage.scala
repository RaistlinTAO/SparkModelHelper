package io.github.raistlintao

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
      Map(0 -> "F0", 1 -> "F1", 2 -> "F0", 3 -> "F1", 4 -> "F0")
    )
    val root_feature_index = helper.getRootFeature
    println("Root Feature Index: " + root_feature_index)
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
