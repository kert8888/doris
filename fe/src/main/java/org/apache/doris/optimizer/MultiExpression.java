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

package org.apache.doris.optimizer;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.doris.optimizer.operator.OptOperator;
import org.apache.doris.optimizer.rule.OptRuleType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// MultiExpression is another way to represent OptExpression, which
// contains an operator and inputs.
// Because MultiExpression's inputs are Groups, so one MultiExpression
// equal with several logical equivalent Expression. As a result, this
// can reduce search space dramatically.
public class MultiExpression {
    private static final Logger LOG = LogManager.getLogger(OptMemo.class);
    public static final int INVALID_ID = -1;
    private int id;
    private OptOperator op;
    private List<OptGroup> inputs;
    private MEState status;

    // OptGroup which this MultiExpression belongs to. Firstly it's null when object is created,
    // it will be assigned after it is inserted into OptMemo
    private OptGroup group;

    // next MultiExpression in same OptGroup, set with group
    private MultiExpression next;
    private OptRuleType ruleTypeDerivedFrom;
    private int sourceMExprId;

    public MultiExpression(OptOperator op, List<OptGroup> inputs, OptRuleType ruleTypeDerivedFrom, int sourceMExprId) {
        this.op = op;
        this.inputs = inputs;
        this.status = MEState.UnImplemented;
        this.ruleTypeDerivedFrom = OptRuleType.RULE_NONE;
        this.ruleTypeDerivedFrom = ruleTypeDerivedFrom;
        this.sourceMExprId = sourceMExprId;
    }

    public MultiExpression(OptOperator op, List<OptGroup> inputs) {
        this(op, inputs, OptRuleType.RULE_NONE, -1);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public OptOperator getOp() { return op; }
    public int arity() { return inputs.size(); }
    public List<OptGroup> getInputs() { return inputs; }
    public OptGroup getInput(int idx) { return inputs.get(idx); }
    public void setGroup(OptGroup group) { this.group = group; }
    public OptGroup getGroup() { return group; }
    public OptRuleType getRuleTypeDerivedFrom() { return ruleTypeDerivedFrom; }
    public void setStatus(MEState status) { this.status = status; }
    public MEState getStatus() { return status; }
    public boolean isImplemented() { return status == MEState.Implemented; }
    public void setNext(MultiExpression next) { this.next = next; }
    public void setInvalid() { this.group = null; }
    public boolean isValid() { return this.group != null; }
    // get next MultiExpression in same group
    public MultiExpression next() { return next; }

    public String debugString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(":").append(op.debugString()).append('[');
        Joiner joiner = Joiner.on(',');
        joiner.join(inputs.stream().map(group -> "" + group.getId()).collect(Collectors.toList()));
        sb.append(']');
        return sb.toString();
    }

    public final String getExplainString() {
        return getExplainString("", "");
    }

    public String getExplainString(String headlinePrefix, String detailPrefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(headlinePrefix).append("MultiExpression ").append(id);
        if (ruleTypeDerivedFrom != OptRuleType.RULE_NONE) {
            sb.append(" (from MultiExpression ")
                    .append(sourceMExprId)
                    .append(" rule:")
                    .append(ruleTypeDerivedFrom)
                    .append(")");
        }
        sb.append(' ')
                .append(op.getExplainString(detailPrefix)).append('\n');
        String childHeadlinePrefix = detailPrefix + OptUtils.HEADLINE_PREFIX;
        String childDetailPrefix = detailPrefix + OptUtils.DETAIL_PREFIX;
        for (OptGroup input : inputs) {
            sb.append(input.getExplain(childHeadlinePrefix, childDetailPrefix));
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = op.hashCode();
        for (OptGroup group : inputs) {
            hash = OptUtils.combineHash(hash, group.hashCode());
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MultiExpression)) {
            return false;
        }
        MultiExpression rhs = (MultiExpression) obj;
        if (this == rhs) {
            return true;
        }
        if (arity() != rhs.arity() || !op.equals(rhs.getOp())) {
            return false;
        }
        for (int i = 0; i < arity(); ++i) {
            if (!inputs.get(i).duplicateWith(rhs.getInput(i))) {
                return false;
            }
        }
        return true;
    }

    public enum MEState {
        UnImplemented,
        Implementing,
        Implemented
    }
}
