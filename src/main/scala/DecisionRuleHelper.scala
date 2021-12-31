package io.github.raistlintao

import org.apache.spark.sql.Row

import scala.math.BigDecimal.javaBigDecimal2bigDecimal

object Language extends Enumeration {
  val Python, Scala = Value
}

object DecisionRuleHelper {
  // A typical rule string contains:
  // 0: Feature Index or Feature Name
  // 1: Node Impurity
  // 2: Node Prediction
  // 3: Node Threshold
  // 4: Node Type: L left, R right, E end. AKA Leaf Node

  def evaluateRule(row: Row, pathList: List[String], typeList: List[String]): Int = {
    //val labels = ListBuffer[Boolean]()

    pathList.foreach(node => {
      if (pathList.indexOf(node) != pathList.length - 1) {
        //Node is not leaf
        //Evaluate the rule, Only return 1 if all nodes are fit, otherwise 0
        val tempArray = node.split("\\|", 0)
        if (tempArray(4) == "L") {
          //Node is LeftChild  <= Threshold

          typeList(tempArray(0).replace("F(", "").replace(")", "").toInt) match {
            case "INT" => {
              if (row.getInt(tempArray(0).replace("F(", "").replace(")", "").toInt) > tempArray(3).toDouble) {
                return 0
              }
            }
            case "DOUBLE" => {
              if (row.getDouble(tempArray(0).replace("F(", "").replace(")", "").toInt) > tempArray(3).toDouble) {
                return 0
              }
            }
            case "LONG" => {
              if (row.getLong(tempArray(0).replace("F(", "").replace(")", "").toInt) > tempArray(3).toDouble) {
                return 0
              }
            }
            case _ => {
              if (row.getDecimal(tempArray(0).replace("F(", "").replace(")", "").toInt) > tempArray(3).toDouble) {
                return 0
              }
            }
          }
        }
        if (tempArray(4) == "R") {
          //Node is RightChild > Threshold
          typeList(tempArray(0).replace("F(", "").replace(")", "").toInt) match {
            case "INT" => {
              if (row.getInt(tempArray(0).replace("F(", "").replace(")", "").toInt) <= tempArray(3).toDouble) {
                return 0
              }
            }
            case "DOUBLE" => {
              if (row.getDouble(tempArray(0).replace("F(", "").replace(")", "").toInt) <= tempArray(3).toDouble) {
                return 0
              }
            }
            case "LONG" => {
              if (row.getLong(tempArray(0).replace("F(", "").replace(")", "").toInt) <= tempArray(3).toDouble) {
                return 0
              }
            }
            case _ => {
              if (row.getDecimal(tempArray(0).replace("F(", "").replace(")", "").toInt) <= tempArray(3).toDouble) {
                return 0
              }
            }
          }
        }
      }
    })
    //if (labels.contains(false)) 0 else 1
    1
  }

  /**
   * Get Scala Statement from pathList
   *
   * @param pathList List[String]: Result from DecisionModelHelper.getRulesList
   * @return String: Scala Statement
   */
  def getStatementFromPathList(pathList: List[String], language: Language.Value): String = {
    var statement = if (language == Language.Scala) "if (" else "if "
    pathList.foreach(node => {
      val tempArray = node.split("\\|", 0)
      if (tempArray(4) != "E") {
        var tempString = tempArray(0)
        if (tempArray(4) == "L") {
          tempString += " <= "
        }
        if (tempArray(4) == "R") {
          tempString += " > "
        }
        if (pathList.indexOf(node) == pathList.length - 2) {
          tempString += tempArray(3)
        }
        else {
          tempString += tempArray(3) + (if (language == Language.Scala) " && " else " and ")
        }
        statement += tempString
      }
      else {
        statement += (if (language == Language.Scala) ") " else ": ") + tempArray(2)
      }
    })
    statement
  }
}
