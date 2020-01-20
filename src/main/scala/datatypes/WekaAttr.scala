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

import java.sql.{Time, Timestamp, Date}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

import weka.core.Attribute

import com.scleradb.sql.expr._
import com.scleradb.sql.types._
import com.scleradb.sql.datatypes.Column
import com.scleradb.sql.result.TableRow

import com.scleradb.analytics.ml.datatypes.DataAttribute

sealed abstract class WekaAttr extends DataAttribute {
    override val name: String
    override val sqlType: SqlType

    def valueUpdate(t: TableRow): Option[Double]
    def value(t: TableRow): Option[Double]

    def attribute: Attribute

    def decode(v: Double): ScalValueBase

    override def toString: String = name + "(" + sqlType.repr + ")"
}

case class NumericAttr(
    override val name: String,
    override val sqlType: SqlType
) extends WekaAttr {
    override def valueUpdate(t: TableRow): Option[Double] = value(t)
    override def value(t: TableRow): Option[Double] =
        t.getScalExpr(name, sqlType) match {
            case (_: SqlNull) => None
            case IntConst(v) => Some(v.toDouble)
            case ShortConst(v) => Some(v.toDouble)
            case LongConst(v) => Some(v.toDouble)
            case FloatConst(v) => Some(v.toDouble)
            case DoubleConst(v) => Some(v)
            case other =>
                throw new RuntimeException(
                    "Cannot translate to numeric: " + other.repr
                )
        }

    override def attribute: Attribute = new Attribute(name)

    override def decode(v: Double): ScalValueBase = sqlType match {
        case SqlInteger => IntConst(v.toInt)
        case SqlSmallInt => ShortConst(v.toShort)
        case SqlBigInt => LongConst(v.toLong)
        case SqlReal => FloatConst(v.toFloat)
        case SqlDecimal(_, _) | SqlFloat(_) => DoubleConst(v)
        case _ =>
            throw new RuntimeException(
                "Cannot decode to numeric: " + sqlType
            )
    }
}

case class NominalAttr(
    override val name: String,
    override val sqlType: SqlType,
    numDistinctValuesOpt: Option[Int]
) extends WekaAttr {
    private val nominalValues: mutable.ArrayBuffer[ScalValueBase] =
        new mutable.ArrayBuffer()

    private val nominalIndex: mutable.Map[ScalValueBase, Int] = mutable.Map() ++
        nominalValues.zipWithIndex

    override def value(t: TableRow): Option[Double] =
        t.getScalValueOpt(name, sqlType).map { v =>
            val i: Int = nominalIndex.get(v).getOrElse {
                throw new IllegalArgumentException(
                    "Unknown value \"" + v.repr + "\" for col \"" + name + "\""
                )
            }

            i.toDouble
        }

    override def valueUpdate(t: TableRow): Option[Double] = 
        t.getScalValueOpt(name, sqlType).map { v => index(v).toDouble }

    private def index(v: ScalValueBase): Int = nominalIndex.getOrElseUpdate(v, {
        val vIndex: Int = nominalValues.size
        nominalValues += v
        vIndex
    })

    override def attribute: Attribute = {
        val numDistinctVals: Int =
            numDistinctValuesOpt getOrElse nominalValues.size
        val dummyVals: Seq[String] =
            (1 to numDistinctVals).map { n => n.toString }

        new Attribute(name, dummyVals.asJava)
    }

    override def decode(v: Double): ScalValueBase = nominalValues(v.toInt)
}

case class TimeAttr(
    override val name: String,
    override val sqlType: SqlType
) extends WekaAttr {
    override def valueUpdate(t: TableRow): Option[Double] = value(t)
    override def value(t: TableRow): Option[Double] =
        t.getScalExpr(name, sqlType) match {
            case (_: SqlNull) => None
            case TimestampConst(v) => Some(v.getTime().toDouble)
            case TimeConst(v) => Some(v.getTime().toDouble)
            case DateConst(v) => Some(v.getTime().toDouble)
            case other =>
                throw new RuntimeException(
                    "Cannot translate to time: " + other.repr
                )
        }

    private val dateFormat: String = null
    override def attribute: Attribute = new Attribute(name, dateFormat)

    override def decode(v: Double): ScalValueBase = sqlType match {
        case SqlTimestamp => TimestampConst(new Timestamp(v.toLong))
        case SqlTime => TimeConst(new Time(v.toLong))
        case SqlDate => DateConst(new Date(v.toLong))
        case _ =>
            throw new RuntimeException(
                "Cannot decode to time: " + sqlType
            )
    }
}

object WekaAttr {
    def apply(
        col: Column,
        numDistinctValuesOpt: Option[Int]
    ): WekaAttr = apply(col.name, col.sqlType, numDistinctValuesOpt)

    def apply(
        name: String,
        sqlType: SqlType,
        numDistinctValuesOpt: Option[Int]
    ): WekaAttr = (sqlType, numDistinctValuesOpt) match {
        case (SqlOption(baseType), _) =>
            apply(name, baseType, numDistinctValuesOpt)

        case (_, nOpt@Some(_)) => // finite domain => nominal
            NominalAttr(name, sqlType, nOpt)

        case (SqlBool, None) =>
            NominalAttr(name, SqlBool, Some(2))

        case (SqlCharFixed(_) | SqlCharVarying(_) | SqlText, None) =>
            NominalAttr(name, sqlType, None)

        case (SqlInteger | SqlSmallInt | SqlBigInt |
              SqlDecimal(_, _) | SqlFloat(_) | SqlReal, None) =>
            NumericAttr(name, sqlType)
                
        case (SqlTimestamp | SqlTime | SqlDate, None) =>
            TimeAttr(name, sqlType)

        case _ =>
            throw new RuntimeException(
                "Cannot instantiate attribute of type " + sqlType
            )
    }
}
