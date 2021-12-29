# Spark Model Helper

A helper that extracting useful information from trained Spark Model

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

## Usage:

### **DecisionTreeClassificationModel Analysis**
***

#### _1. Get Root Feature Index from trained model_

In automated ML, sometimes you need to retrain model due to the scalar metric as evaluation result (precision and recall
for instance) are not within a desired range. By using rootFeatureIndex we can change the dataframe accordingly.

```scala
  val helper = new DecisionModelHelper(model)
  println("Root Feature Index: " + helper.getRootFeature)
```

#### _2. Get the JSON string from DecisionTreeClassificationModel_

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