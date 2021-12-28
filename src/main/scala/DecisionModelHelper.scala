package io.github.RaistlinTao

import com.alibaba.fastjson.{JSON, JSONObject}
import net.liftweb.json.DefaultFormats

import scala.collection.mutable.ListBuffer

/** *
 * DecisionModel Node Class
 *
 * @param featureIndex    featureIndex
 * @param gain            gain
 * @param impurity        impurity
 * @param threshold       threshold
 * @param nodeType        nodeType
 * @param splitType       splitType
 * @param leftCategories  leftCategories
 * @param rightCategories rightCategories
 * @param prediction      prediction
 * @param leftChild       leftChild
 * @param rightChild      rightChild
 * @param path            For Human Processing,
 *                        index|impurity|prediction|threshold|direction (For Leaf Node, direction = "end")
 */
case class DecisionNode(featureIndex: Option[Int],
                        gain: Option[Double],
                        impurity: Double,
                        threshold: Option[Double], // Continuous split
                        nodeType: String, // Internal or leaf
                        splitType: Option[String], // Continuous and categorical
                        leftCategories: Option[Array[Double]], // Categorical Split
                        rightCategories: Option[Array[Double]], // Categorical Split
                        prediction: Double,
                        leftChild: Option[DecisionNode],
                        rightChild: Option[DecisionNode],
                        path: String
                       )

class DecisionModelHelper(tree: org.apache.spark.ml.classification.DecisionTreeClassificationModel) {
  private val _tempPath = new ListBuffer[String]()
  private val _allPath = new ListBuffer[List[String]]()
  private var _jsonObj = new JSONObject()
  private var _lastJSONNode = new JSONObject()
  private var _lastDirectionIsLeft = true
  private var _predictionLabel = 0.0
  private var _impurityRestriction = 0.0
  private var _useCustomFeatureName = false
  private var _customFeatureName = Map[Int, String]()

  def setFeatureName(customFeatureName: Map[Int, String]): Unit = {
    _useCustomFeatureName = true
    _customFeatureName = customFeatureName
  }

  /** *
   * Convert Model Object directly to JSON String
   *
   * @return JSON String
   */
  def toJson: String = {
    implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats
    net.liftweb.json.Serialization.writePretty(get_decision_rules(tree.rootNode))
  }

  /** *
   * Get DecisionNode Object Directly
   *
   * @return DecisionNode
   */
  def getDecisionNode: DecisionNode = {
    get_decision_rules(tree.rootNode)
  }

  /**
   * Get Selected Rules (Including Root to Leaf Path) from Model
   *
   * @param predictionLabel     Set the limitation of Prediction of Selected Leaf Node
   * @param impurityRestriction Set the limitation of Impurity of Selected Leaf Node
   * @return List of Rules, Each Rule has its own list of path.
   */
  def getRulesList(predictionLabel: Int, impurityRestriction: Double): List[List[String]] = {
    implicit val formats: DefaultFormats.type = net.liftweb.json.DefaultFormats
    val jsonNode = JSON.parseObject(net.liftweb.json.Serialization.writePretty(get_decision_rules(tree.rootNode)))
    _predictionLabel = predictionLabel
    _impurityRestriction = impurityRestriction
    _jsonObj = jsonNode
    extractRuleFromJson(_jsonObj)
    _allPath.toList
  }

  private def get_node_type(node: org.apache.spark.ml.tree.Node): String = node match {
    case internal: org.apache.spark.ml.tree.InternalNode => "internal"
    case other => "leaf"
  }

  private def get_split_type(split: org.apache.spark.ml.tree.Split): String = split match {
    case continuous: org.apache.spark.ml.tree.ContinuousSplit => "continuous"
    case other => "categorical"
  }

  private def get_custom_feature_name(index: Int): String = {
    if (index != -1) {
      try {
        _customFeatureName(index)
      }
      catch {
        case _: Throwable => "FEATURE_NAME_NOT_FOUND"
      }
    }
    else {
      "LEAF"
    }
  }

  /** *
   * Extract Rules From Node
   *
   * @param node org.apache.spark.ml.tree.Node
   * @return DecisionNode
   */
  private def get_decision_rules(node: org.apache.spark.ml.tree.Node): DecisionNode = {
    val node_type = get_node_type(node)
    val gain: Option[Double] = node_type match {
      case "internal" => Some(node.asInstanceOf[org.apache.spark.ml.tree.InternalNode].gain)
      case "leaf" => None
    }

    val feature_index: Option[Int] = node_type match {
      case "internal" => Some(node.asInstanceOf[org.apache.spark.ml.tree.InternalNode].split.featureIndex)
      case "leaf" => None
    }

    val split_type: Option[String] = node_type match {
      case "internal" => Some(get_split_type(node.asInstanceOf[org.apache.spark.ml.tree.InternalNode].split))
      case "leaf" => None
    }

    val node_threshold: Option[Double] = split_type match {
      case Some("continuous") => Some(node.asInstanceOf[org.apache.spark.ml.tree.InternalNode].split.asInstanceOf[org.apache.spark.ml.tree.ContinuousSplit].threshold)
      case Some("categorical") => None
      case other => None
    }

    val (left_categories: Option[Array[Double]],
    right_categories: Option[Array[Double]]) = split_type match {
      case Some("categorical") => (Some(node.asInstanceOf[org.apache.spark.ml.tree.InternalNode].split.asInstanceOf[org.apache.spark.ml.tree.CategoricalSplit].leftCategories),
        Some(node.asInstanceOf[org.apache.spark.ml.tree.InternalNode].split.asInstanceOf[org.apache.spark.ml.tree.CategoricalSplit].rightCategories)
      )
      case other => (None, None)
    }

    val left_child: Option[DecisionNode] = node_type match {
      case "internal" => Some(get_decision_rules(node.asInstanceOf[org.apache.spark.ml.tree.InternalNode].leftChild))
      case other => None
    }

    val right_child: Option[DecisionNode] = node_type match {
      case "internal" => Some(get_decision_rules(node.asInstanceOf[org.apache.spark.ml.tree.InternalNode].rightChild))
      case other => None
    }

    DecisionNode(featureIndex = feature_index,
      gain = gain,
      impurity = node.impurity,
      threshold = node_threshold,
      nodeType = node_type,
      splitType = split_type,
      leftCategories = left_categories, // Categorical Split
      rightCategories = right_categories, // Categorical Split
      prediction = node.prediction,
      leftChild = left_child,
      rightChild = right_child,
      path = ((if (_useCustomFeatureName) get_custom_feature_name(feature_index.getOrElse(-1)) else feature_index) + "|" + node.impurity + "|" + node.prediction + "|" +
        node_threshold.toString.replace("Some(", "").replace(")", "") +
        (if (node_type == "internal") "|" else "|E")).replace("Some", "F")
    )
  }

  private def extractRuleFromJson(jsonObj: JSONObject): Unit = {
    if (!_jsonObj.containsKey("leftChild") && !_jsonObj.containsKey("rightChild")) {
      return
    }
    if (jsonObj.getString("nodeType") == "internal") {
      if (jsonObj.containsKey("leftChild") || jsonObj.containsKey("rightChild")) {
        _lastJSONNode = jsonObj
        if (jsonObj.containsKey("leftChild")) {
          _tempPath += jsonObj.getString("path") + "L"
          //println("internal node: Left PATH - " + path)
          _lastDirectionIsLeft = true
          extractRuleFromJson(jsonObj.getJSONObject("leftChild"))
        }
        if (jsonObj.containsKey("rightChild")) {
          _tempPath += jsonObj.getString("path") + "R"
          //println("internal node: Right PATH - " + path)
          _lastDirectionIsLeft = false
          extractRuleFromJson(jsonObj.getJSONObject("rightChild"))
        }
      }
      else {
        if (_lastDirectionIsLeft) {
          _lastJSONNode.remove("leftChild")
        }
        else {
          _lastJSONNode.remove("rightChild")
        }
        _tempPath.dropRight(1)
        extractRuleFromJson(_jsonObj)
      }
    }
    else {
      if (jsonObj.getInteger("prediction") == _predictionLabel && jsonObj.getFloat("impurity") <= _impurityRestriction) {
        _tempPath += jsonObj.getString("path")
        //println("leaf node: PATH - " + _tempPath)
        _allPath += _tempPath.toList
      }
      if (_lastDirectionIsLeft) {
        _lastJSONNode.remove("leftChild")
      }
      else {
        _lastJSONNode.remove("rightChild")
      }
      _tempPath.clear()
      extractRuleFromJson(_jsonObj)
    }
  }
}
