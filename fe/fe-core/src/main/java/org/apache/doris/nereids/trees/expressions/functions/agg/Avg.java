// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.trees.expressions.functions.agg;

import org.apache.doris.catalog.FunctionSignature;
import org.apache.doris.nereids.exceptions.AnalysisException;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.functions.CustomSignature;
import org.apache.doris.nereids.trees.expressions.shape.UnaryExpression;
import org.apache.doris.nereids.trees.expressions.visitor.ExpressionVisitor;
import org.apache.doris.nereids.types.BigIntType;
import org.apache.doris.nereids.types.DataType;
import org.apache.doris.nereids.types.DateTimeType;
import org.apache.doris.nereids.types.DateType;
import org.apache.doris.nereids.types.DecimalV2Type;
import org.apache.doris.nereids.types.DoubleType;
import org.apache.doris.nereids.types.coercion.NumericType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

/** avg agg function. */
public class Avg extends NullableAggregateFunction implements UnaryExpression, CustomSignature {

    public Avg(Expression child) {
        this(false, false, child);
    }

    public Avg(boolean isDistinct, Expression arg) {
        this(isDistinct, false, arg);
    }

    private Avg(boolean isDistinct, boolean isAlwaysNullable, Expression arg) {
        super("avg", isAlwaysNullable, isDistinct, arg);
    }

    @Override
    public FunctionSignature customSignature() {
        DataType implicitCastType = implicitCast(getArgument(0).getDataType());
        return FunctionSignature.ret(implicitCastType).args(implicitCastType);
    }

    @Override
    protected List<DataType> intermediateTypes() {
        DataType sumType = getDataType();
        BigIntType countType = BigIntType.INSTANCE;
        return ImmutableList.of(sumType, countType);
    }

    @Override
    public AggregateFunction withDistinctAndChildren(boolean isDistinct, List<Expression> children) {
        Preconditions.checkArgument(children.size() == 1);
        return new Avg(isDistinct, isAlwaysNullable, children.get(0));
    }

    @Override
    public Avg withChildren(List<Expression> children) {
        Preconditions.checkArgument(children.size() == 1);
        return new Avg(isDistinct, isAlwaysNullable, children.get(0));
    }

    @Override
    public NullableAggregateFunction withAlwaysNullable(boolean isAlwaysNullable) {
        return new Avg(isDistinct, isAlwaysNullable, children.get(0));
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitAvg(this, context);
    }

    private DataType implicitCast(DataType dataType) {
        if (dataType instanceof DecimalV2Type) {
            return DecimalV2Type.SYSTEM_DEFAULT;
        } else if (dataType.isDate()) {
            return DateType.INSTANCE;
        } else if (dataType.isDateTime()) {
            return DateTimeType.INSTANCE;
        } else if (dataType instanceof NumericType) {
            return DoubleType.INSTANCE;
        } else {
            throw new AnalysisException("avg requires a numeric parameter: " + dataType);
        }
    }
}
