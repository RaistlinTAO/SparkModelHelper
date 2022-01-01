# Spark Model Helper

A helper that extracting useful information from trained Spark Model

![License](https://img.shields.io/github/license/RaistlinTAO/SparkModelHelper)

![Sonatype](https://img.shields.io/nexus/s/io.github.raistlintao/sparkmodelhelper_2.12?server=https%3A%2F%2Fs01.oss.sonatype.org%2F)
![Maven Central](https://img.shields.io/maven-central/v/io.github.raistlintao/sparkmodelhelper_2.12.svg)

![Code Size](https://img.shields.io/github/languages/code-size/raistlintao/SparkModelHelper)
![Repo Size](https://img.shields.io/github/repo-size/RaistlinTAO/SparkModelHelper)

Have you tired staring at the model.toDebugString() for hours and getting no clue at all? Something like this:

```text
DecisionTreeClassificationModel (uid=dtc_e933455b) of depth 5 with 341 nodes
  If (feature 518 <= 1.5)
   If (feature 6 <= 2.5)
    If (feature 30 <= 20)
     If (feature 45 <= 7)
      If (feature 24 <= 2.5)
        ...
      Else (feature 160 > 0.99)
      If (feature 64 <= 3.5)
       Predict: 0.0
      Else (feature 64 > 3.5)
       Predict: 1.0
```
Well now you have this helper designed for HUMAN, which matters.

## Contents

- [Spark Model Helper](#spark-model-helper)
    * [Usage](#usage)
        + [**DecisionTreeClassificationModel Analysis**](#decisiontreeclassificationmodel-analysis)
            - [_1. Get Root Feature Index from trained model_](#1-get-root-feature-index-from-trained-model)
            - [_2. Get the JSON string from
              DecisionTreeClassificationModel_](#2-get-the-json-string-from-decisiontreeclassificationmodel)
            - [_3. Return an Object of Model Node-Tree_](#3-return-an-object-of-model-node-tree)
            - [_4. Return Root to Leaf Path of Rules_](#4-return-root-to-leaf-path-of-rules)
            - [_5. Customise the Feature_Index_](#5-customise-the-feature_index)
        + [**DecisionTreeClassificationModel Rule Helper**](#decisiontreeclassificationmodel-rule-helper)
            - [_Convert Rules into Python or Scala_](#1-convert-rules-into-python-or-scala)
            - [_Evaluate new data against Rules_](#2-evaluate-new-data-against-rules)


##### This helper was built for [Scala 2.12.15](https://www.scala-lang.org/download/2.12.15.html) and [Spark 2.4.8](https://spark.apache.org/docs/2.4.8/). Also tested with the latest Scala and Spark.

# Usage

## **DecisionTreeClassificationModel Analysis**

***

### _1. Get Root Feature Index from trained model_

In automated ML, sometimes you need to retrain model due to the scalar metric as evaluation result (precision and recall
for instance) are not within a desired range. By using rootFeatureIndex we can change the dataframe accordingly.

```scala
  val helper = new DecisionModelHelper(model)
  println("Root Feature Index: " + helper.getRootFeature)
```

### _2. Get the JSON string from DecisionTreeClassificationModel_

```scala
  val helper = new DecisionModelHelper(model)
  println("toJson: " + helper.toJson + "\n")
```

Return beatified JSON string:

```json
    {
      "featureIndex": 367,
      "gain": 0.10617781879627633,
      "impurity": 0.3144732024264778,
      "threshold": 1.5,
      "nodeType": "internal",
      "splitType": "continuous",
      "prediction": 0.0,
      "leftChild": {
        ....
      "path": "F(367)|0.3144732024264778|0.0|1.5|"
    }
```

### _3. Return an Object of Model Node-Tree_

```scala
    val nodeObj = helper.getDecisionNode
    println("getDecisionNode: " + nodeObj)
```

Object version of JSON String

### _4. Return Root to Leaf Path of Rules_

```scala
    val rules = helper.getRulesList(1, 0.2)
    rules.foreach(rule => {
      println("Rule: " + rule.mkString(", "))
    })
```
The above code prints:
```text
    Rule: F(396)|0.3144732024264778|0.0|1.5|L, F(12)|0.49791192623975383|1.0|2.5|R, F(223)|0.2998340735773348|1.0|2500000.0|R, F(20)|0.19586076183802947|1.0|3.523971665E10|L, None|0.1902980108641974|1.0|None|E
```
The function returns List[List[String]], the structure of each String is
```text
    Feature_Index | impurity | prediction | threshold | node_type
```
For example, Feature Index **45** has impurity **3.5**, prediction **1**, threshold **1.5** and the path goes **right** after this node, the string will be:
```text
    F(45)|3.5|1|1.5|R
```
The Leaf nodes will have **"E"** as node_type

### _5. Customise the Feature_Index_
Feature Index is not designed for human reading, especially with large amount of columns.
The helper also supports customisation of Features
```scala
  val helper = new DecisionModelHelper(model)
    helper.setFeatureName(
      Map(0 -> "UserID", 1 -> "UserCity", 2 -> "Salary" ... )
    )
```
The helper automatically change the F(1) into "UserCity" upon called setFeatureName(Map[Int, String])
```text
    F(1)|3.5|1|1.5|R
```
will output as 
```text
    UserCity|3.5|1|1.5|R
```

## **DecisionTreeClassificationModel Rule Helper**

### _1. Convert Rules into Python or Scala_
```scala
    rules.foreach(rule => {
      println("Rule (Scala): " + '\n' + DecisionRuleHelper.getStatementFromPathList(rule, language = Language.Scala))
      println("Rule (Python): " + '\n' + DecisionRuleHelper.getStatementFromPathList(rule, language = Language.Python))
    })
```

You can get code directly from this function and just Copy & Paste
```text
Rule (Scala): 
if (F(58) <= 1.5 && F(56) > 2.5 && F(20) > 2500000.0 && F(20) <= 3.523971665E10) 1.0
Rule (Python): 
if F(58) <= 1.5 and F(56) > 2.5 and F(20) > 2500000.0 and F(20) <= 3.523971665E10: 1.0
```

### _2. Evaluate new data against Rules_
For testing new data against extracted rules, *Remember ONLY support non-customised Feature Name*

```scala
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
    println("Result CSV Saved, or you can use data (Dataframe) directly")
```

After the loop end, the data itself changed into a new dataframe with several additional columns depends on your rules 
extracted from model(1 rule for 1 column). 
Each additional column represents result of evaluation of current row.

For example, if row 1 fit Rule 1: 
```
(F(58) <= 1.5 && F(56) > 2.5 && F(20) > 2500000.0 && F(20) <= 3.523971665E10) 1.0
```
You will get 1 otherwise 0