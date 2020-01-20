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

package com.scleradb.plugin.analytics.ml.weka.clusterer

import scala.jdk.CollectionConverters._

import java.io.{ObjectInputStream, ObjectOutputStream}

import weka.core.{Attribute, Instance, DenseInstance, Instances}

import com.scleradb.sql.expr.ColRef
import com.scleradb.sql.result.{TableResult, TableRow}

import com.scleradb.analytics.ml.clusterer.objects.Clusterer

import com.scleradb.plugin.analytics.ml.weka.datatypes._
import com.scleradb.plugin.analytics.ml.weka.service.WekaService

import com.scleradb.config.ScleraConfig

class WekaClusterer(
    override val name: String,
    override val attrs: List[WekaAttr],
    clusterer: weka.clusterers.Clusterer
) extends Clusterer {
    override def serviceId: String = WekaService.id
    override def serialize(out: ObjectOutputStream): Unit = {
        super.serialize(out)
        out.writeObject(this)
    }

    override def toString: String = {
        "CLUSTERER[" + name + ": " + attrs.mkString(", ") + "]"
    }

    private lazy val refDataSet: Instances = {
        val attrInfo: List[Attribute] = attrs.map { attr => attr.attribute }
        new Instances("Input", new java.util.ArrayList(attrInfo.asJava), 0)
    }

    override def cluster(t: TableRow): Int = {
        val instance: Instance = new DenseInstance(attrs.size)
        attrs.zipWithIndex.foreach { case (attr, i) =>
            attr.value(t) match {
                case Some(v) => instance.setValue(i, v)
                case None => instance.setMissing(i)
            }
        }

        instance.setDataset(refDataSet)

        clusterer.clusterInstance(instance)
    }

    override def description: String = clusterer.toString
}

object WekaClusterer {
    def deSerialize(in: ObjectInputStream): WekaClusterer =
        in.readObject().asInstanceOf[WekaClusterer]

    private def create(
        typeName: String
    ): weka.clusterers.Clusterer with weka.core.OptionHandler =
        typeName.toUpperCase match {
            case "SIMPLEKMEANS" => new weka.clusterers.SimpleKMeans()
            case "COBWEB" => new weka.clusterers.Cobweb()
            case "EM" => new weka.clusterers.EM()
            case "FARTHESTFIRST" => new weka.clusterers.FarthestFirst()
            case "HIERARCHICAL" => new weka.clusterers.HierarchicalClusterer()
            case _ => throw new IllegalArgumentException(
                "Clusterer type \"" + typeName + "\" not supported"
            )
        }

    def apply(
        name: String,
        specOpt: Option[(String, String)],
        numDistinctValuesMap: Map[ColRef, Int],
        rs: TableResult
    ): WekaClusterer = {
        val (typeName, options) = specOpt getOrElse ("SIMPLEKMEANS", "")
        val wekaClusterer
            : weka.clusterers.Clusterer with weka.core.OptionHandler =
            create(typeName)
        wekaClusterer.setOptions(options.split("""\s+"""))
        
        val data: WekaInstances = WekaInstances(rs, numDistinctValuesMap)
        val instances: Instances = data.batch

        try wekaClusterer.buildClusterer(instances)
        catch { case (e: Throwable) =>
            throw new IllegalArgumentException(
                typeName + ":" +
                Option(e.getMessage()).getOrElse(
                    "Unable to create clusterer"
                ),
                e
            )
        }

        if( ScleraConfig.isExplain )
            println("Trained clusterer " + name +
                    " on " + instances.numInstances() + " training instances")

        new WekaClusterer(name, data.attrs, wekaClusterer)
    }
}
