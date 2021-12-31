package io.github.raistlintao

import org.apache.spark.ml.classification.DecisionTreeClassificationModel
import org.apache.spark.sql.functions.{col, struct, udf}
import org.apache.spark.sql.types.{DoubleType, IntegerType, LongType}
import org.apache.spark.sql.{Row, SparkSession}

import scala.collection.mutable.ListBuffer

object Usage {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().master("local").getOrCreate()
    //Disable debug INFO, only show ERROR Messages
    spark.sparkContext.setLogLevel("ERROR")
    println("SparkSession Loaded\n")
    //Load the example Model
    val model = DecisionTreeClassificationModel.load("sample/ClassificationModel")
    println("toDebugString:" + model.toDebugString + "\n")
    ////////////////////////////////////////////////////DECISION MODEL HELPER///////////////////////////////////////////
    val helper = new DecisionModelHelper(model)
    //Set Custom Feature Name (Disabled for Rules Helper evaluateRule function)
    /*
    helper.setFeatureName(
      Map(0 -> "F0", 1 -> "F1", 2 -> "F0", 3 -> "F1", 4 -> "F0")
    )
     */
    //Get Root Feature Index
    val root_feature_index = helper.getRootFeature
    println("Root Feature Index: " + root_feature_index)
    //Get JSON format Model Description
    val jsonStr = helper.toJson
    println("toJson: " + jsonStr + "\n")
    //Get Object of Model Description
    val nodeObj = helper.getDecisionNode
    println("getDecisionNode: " + nodeObj + "\n")
    //Extract Rules
    val rules = helper.getRulesList(1, 0.2)
    rules.foreach(rule => {
      println("Rule: " + rule.mkString(","))
    })
    ////////////////////////////////////////////////////DECISION RULE HELPER////////////////////////////////////////////
    //Convert Rules Into Programming Languages
    rules.foreach(rule => {
      println("Rule: " + rule.mkString(","))
    })
    rules.foreach(rule => {
      println("Rule (Scala): " + '\n' + DecisionRuleHelper.getStatementFromPathList(rule, language = Language.Scala))
      println("Rule (Python): " + '\n' + DecisionRuleHelper.getStatementFromPathList(rule, language = Language.Python))
    })

    //Evaluation Rules for new data  (ONLY support non-customised Feature Name)
    //Load the data into Dataframe from CSV
    var data = spark.read.format("csv").option("header", value = false).option("inferSchema", value = true).load("sample/testing_data.csv").na.drop("all")
    println("Data has been loaded into Dataframe from CSV file, or you can use existing Dataframe directly")
    //Convert Dataframe Schema Datatype into a ListBuffer
    val typeList = ListBuffer[String]()
    data.schema.foreach(schemaNode => {
      schemaNode.dataType match {
        case IntegerType => {
          typeList += "INT"
        }
        case DoubleType => {
          typeList += "DOUBLE"
        }
        case LongType => {
          typeList += "LONG"
        }
        case _ => {
          typeList += "DEC"
        }
      }
    })
    //Loop Though Every Rules (Start from 1 for Easy Column Name)
    for (ruleIndex <- 1 to rules.length) {
      val columns = data.columns

      //Define the User Defined Function: Using DecisionRuleHelper.evaluateRule
      def combineUdf = udf((row: Row) => DecisionRuleHelper.evaluateRule(row, rules(ruleIndex - 1), typeList.toList))

      //Create a new column that save the evaluation result of each rows into Column
      //If data in one row fit the rule, the result is 1, otherwise 0
      data = data.withColumn("Rule" + ruleIndex.toString, combineUdf(struct(columns.map(col): _*)))
      println("Rule " + ruleIndex + " proceed")
    }
    data.write.format("csv").save("result/" + System.currentTimeMillis() / 1000)
    println("Result Saved")
  }
}
