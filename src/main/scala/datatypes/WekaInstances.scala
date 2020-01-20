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

package com.scleradb.plugin.analytics.ml.weka.datatypes

import scala.jdk.CollectionConverters._

import weka.core.{Attribute, Instance, DenseInstance, Instances}

import com.scleradb.sql.expr.ColRef
import com.scleradb.sql.result.TableResult

class WekaInstances(
    rs: TableResult,
    numDistinctValuesMap: Map[ColRef, Int]
) {
    val attrs: List[WekaAttr] = rs.columns.map { col =>
        val numDistinctValuesOpt: Option[Int] =
            numDistinctValuesMap.get(ColRef(col.name))
        WekaAttr(col, numDistinctValuesOpt)
    }

    private def instanceIter: Iterator[Instance] = rs.rows.map { t =>
        val instance: Instance = new DenseInstance(attrs.size)
        attrs.zipWithIndex.foreach { case (attr, i) =>
            attr.valueUpdate(t) match {
                case Some(v) => instance.setValue(i, v)
                case None => instance.setMissing(i)
            }
        }

        instance
    }

    def incremental: (Instances, Iterator[Instance]) = {
        val attrInfo: List[Attribute] = attrs.map { attr => attr.attribute }
        val emptyInstances: Instances =
            new Instances(
                "TrainingIncr", new java.util.ArrayList(attrInfo.asJava), 0
            )

        val instances: Iterator[Instance] =
            instanceIter.map { inst =>
                inst.setDataset(emptyInstances)
                inst
            }

        (emptyInstances, instances)
    }

    def batch: Instances = {
        val instances: List[Instance] = instanceIter.toList

        // needs to be computed after processing all the result rows
        val attrInfo: List[Attribute] = attrs.map { attr => attr.attribute }
        val result: Instances =
            new Instances(
                "TrainingBatch", new java.util.ArrayList(attrInfo.asJava), 0
            )

        instances.foreach { inst => result.add(inst) }

        result
    }
}

object WekaInstances {
    def apply(
        rs: TableResult,
        numDistinctValuesMap: Map[ColRef, Int]
    ): WekaInstances = new WekaInstances(rs, numDistinctValuesMap)
}
