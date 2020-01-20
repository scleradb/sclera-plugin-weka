/**
* Sclera - Weka Connector
* Copyright 2012 - 2020 Sclera, Inc.
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.scleradb.plugin.analytics.ml.weka.classifier

import scala.jdk.CollectionConverters._

import java.io.{ObjectInputStream, ObjectOutputStream}

import weka.core.{Attribute, Instance, DenseInstance, Instances}

import com.scleradb.sql.expr.{ColRef, ScalValueBase}
import com.scleradb.sql.result.{TableRow, TableResult}

import com.scleradb.analytics.ml.classifier.objects.Classifier

import com.scleradb.plugin.analytics.ml.weka.datatypes._
import com.scleradb.plugin.analytics.ml.weka.service.WekaService

import com.scleradb.config.ScleraConfig

class WekaClassifier(
    override val name: String,
    attrs: List[WekaAttr],
    targetAttrIndex: Int,
    classifier: weka.classifiers.Classifier
) extends Classifier {
    override def serviceId: String = WekaService.id
    override def serialize(out: ObjectOutputStream): Unit = {
        super.serialize(out)
        out.writeObject(this)
    }

    override val targetAttr: WekaAttr = attrs(targetAttrIndex)
    override val featureAttrs: List[WekaAttr] = attrs diff List(targetAttr)

    private lazy val refDataSet: Instances = {
        val attrInfo: List[Attribute] = attrs.map { attr => attr.attribute }
        val dataSet: Instances =
            new Instances("Input", new java.util.ArrayList(attrInfo.asJava), 0)
        dataSet.setClassIndex(targetAttrIndex)

        dataSet
    }

    override def classifyOpt(t: TableRow): Option[ScalValueBase] = {
        try {
            val instance: Instance = new DenseInstance(attrs.size)
            attrs.zipWithIndex.foreach { case (attr, i) =>
                if( i == targetAttrIndex ) instance.setMissing(i) else {
                    attr.value(t) match {
                        case Some(v) => instance.setValue(i, v)
                        case None => instance.setMissing(i)
                    }
                }
            }

            instance.setDataset(refDataSet)

            val targetVal: Double = classifier.classifyInstance(instance)
            Option(targetVal).map { v => targetAttr.decode(v) }
        } catch {
            case (e: Throwable) =>
                logger.warn(
                    "[Classifier " + name + "] Alert: " + e.getMessage()
                )

                logger.info(
                    "[Classifier " + name + "]" +
                    " Setting the classified value = NULL"
                )

                None
        }
    }

    override def description: String = classifier.toString

    override def toString: String = {
        "CLASSIFIER[" + name + ": " +
        featureAttrs.mkString(", ") + " -> " + targetAttr.name + "]"
    }
}

object WekaClassifier {
    def deSerialize(in: ObjectInputStream): WekaClassifier =
        in.readObject().asInstanceOf[WekaClassifier]

    private def create(
        typeName: String
    ): weka.classifiers.Classifier with weka.core.OptionHandler =
        typeName.toUpperCase match {
            case "J48" => new weka.classifiers.trees.J48()
            case "HOEFFDINGTREE" => new weka.classifiers.trees.HoeffdingTree()
            case "LMT" => new weka.classifiers.trees.LMT()
            case "M5P" => new weka.classifiers.trees.M5P()
            case "RANDOMFOREST" => new weka.classifiers.trees.RandomForest()
            case "REPTREE" => new weka.classifiers.trees.REPTree()
            case "CLASSIFICATIONVIAREGRESSION" =>
                new weka.classifiers.meta.ClassificationViaRegression()
            case "DECISIONTABLE" => new weka.classifiers.rules.DecisionTable()
            case "M5RULES" => new weka.classifiers.rules.M5Rules()
            case "ONER" => new weka.classifiers.rules.OneR()
            case "SIMPLELOGISTIC" =>
                new weka.classifiers.functions.SimpleLogistic()
            case "LOGISTIC" => new weka.classifiers.functions.Logistic()
            case "NAIVEBAYES" => new weka.classifiers.bayes.NaiveBayes()
            case "SIMPLELINEAR" =>
                new weka.classifiers.functions.SimpleLinearRegression()
            case "LINEAR" => new weka.classifiers.functions.LinearRegression()
            case "SGD" => new weka.classifiers.functions.SGD()
            case "SMO" => new weka.classifiers.functions.SMO()
            case _ => throw new IllegalArgumentException(
                "Classifier type \"" + typeName + "\" not supported"
            )
        }

    def apply(
        name: String,
        specOpt: Option[(String, String)],
        targetAttrIndex: Int,
        numDistinctValuesMap: Map[ColRef, Int],
        rs: TableResult
    ): WekaClassifier = {
        val (typeName, options) = specOpt getOrElse ("J48", "")
        val wekaClassifier
            : weka.classifiers.Classifier with weka.core.OptionHandler =
            create(typeName)
        wekaClassifier.setOptions(options.split("""\s+"""))

        val data: WekaInstances = WekaInstances(rs, numDistinctValuesMap)
        val numTrainingInstances: Long = wekaClassifier match {
            case (incrClassifier: weka.classifiers.UpdateableClassifier) =>
                val (initInstances, incrInstances) = data.incremental
                initInstances.setClassIndex(targetAttrIndex)

                try wekaClassifier.buildClassifier(initInstances)
                catch { case (e: Throwable) =>
                    throw new IllegalArgumentException(
                        typeName + ": " +
                        Option(e.getMessage()).getOrElse(
                            "Unable to create incremental classifier"
                        ),
                        e
                    )
                }

                var numInstances: Long = initInstances.numInstances()
                incrInstances.foreach { instance =>
                    try incrClassifier.updateClassifier(instance)
                    catch { case (e: Throwable) =>
                        throw new IllegalArgumentException(
                            typeName + ": " +
                            Option(e.getMessage()).getOrElse(
                                "Unable to update incremental classifier"
                            ),
                            e
                        )
                    }

                    numInstances += 1L
                }

                numInstances

            case batchClassifier =>
                val instances: Instances = data.batch
                instances.setClassIndex(targetAttrIndex)

                try wekaClassifier.buildClassifier(instances)
                catch { case (e: Throwable) =>
                    throw new IllegalArgumentException(
                        typeName + ": " +
                        Option(e.getMessage()).getOrElse(
                            "Unable to create batch classifier"
                        ),
                        e
                    )
                }

                instances.numInstances()
        }

        if( ScleraConfig.isExplain )
            println("Learned classifier " + name +
                    " on " + numTrainingInstances + " training instances")

        new WekaClassifier(name, data.attrs, targetAttrIndex, wekaClassifier)
    }
}
