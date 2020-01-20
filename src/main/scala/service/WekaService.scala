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

package com.scleradb.plugin.analytics.ml.weka.service

import java.io.ObjectInputStream

import com.scleradb.sql.expr.ColRef
import com.scleradb.sql.result.TableResult

import com.scleradb.analytics.ml.service.MLService

import com.scleradb.plugin.analytics.ml.weka.datatypes._
import com.scleradb.plugin.analytics.ml.weka.classifier.WekaClassifier
import com.scleradb.plugin.analytics.ml.weka.clusterer.WekaClusterer

class WekaService extends MLService {
    override val id: String = WekaService.id

    override def deSerializeClassifier(in: ObjectInputStream): WekaClassifier =
        WekaClassifier.deSerialize(in)

    override def deSerializeClusterer(in: ObjectInputStream): WekaClusterer =
        WekaClusterer.deSerialize(in)

    override def createClassifier(
        name: String,
        specOpt: Option[(String, String)],
        targetColIndex: Int,
        numDistinctValuesMap: Map[ColRef, Int],
        rs: TableResult
    ): WekaClassifier = WekaClassifier(
        name, specOpt, targetColIndex, numDistinctValuesMap, rs
    )

    override def createClusterer(
        name: String,
        specOpt: Option[(String, String)],
        numDistinctValuesMap: Map[ColRef, Int],
        rs: TableResult
    ): WekaClusterer = WekaClusterer(name, specOpt, numDistinctValuesMap, rs)
}

object WekaService {
    val id: String = "WEKA"
}
