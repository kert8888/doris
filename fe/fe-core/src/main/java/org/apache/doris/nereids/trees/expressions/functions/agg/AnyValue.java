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
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.functions.CustomSignature;
import org.apache.doris.nereids.trees.expressions.functions.PropagateNullable;
import org.apache.doris.nereids.trees.expressions.shape.UnaryExpression;
import org.apache.doris.nereids.trees.expressions.visitor.ExpressionVisitor;
import org.apache.doris.nereids.types.DataType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * any_value agg function.
 */
public class AnyValue extends AggregateFunction implements UnaryExpression, PropagateNullable, CustomSignature {

    public AnyValue(Expression child) {
        super("any_value", child);
    }

    public AnyValue(boolean isDistinct, Expression arg) {
        super("any_value", false, arg);
    }

    @Override
    public FunctionSignature customSignature() {
        DataType dataType = getArgument(0).getDataType();
        return FunctionSignature.ret(dataType).args(dataType);
    }

    @Override
    protected List<DataType> intermediateTypes() {
        return ImmutableList.of(getDataType());
    }

    @Override
    public AnyValue withDistinctAndChildren(boolean isDistinct, List<Expression> children) {
        Preconditions.checkArgument(children.size() == 1);
        return new AnyValue(isDistinct, children.get(0));
    }

    @Override
    public AnyValue withChildren(List<Expression> children) {
        Preconditions.checkArgument(children.size() == 1);
        return new AnyValue(children.get(0));
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitAnyValue(this, context);
    }
}
