/*
 * Copyright (C) 2025 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-formula-analysis-sat4j.
 *
 * formula-analysis-sat4j is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * formula-analysis-sat4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with formula-analysis-sat4j. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.analysis.sat4j.computation;

import de.featjar.base.computation.AComputation;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.util.List;

/**
 *
 * @author Sebastian Krieter
 */
public class VariableCombinationSpecifictionComputation extends AComputation<ICombinationSpecification> {

    public static final Dependency<BooleanAssignmentList> BOOLEAN_CLAUSE_LIST =
            Dependency.newDependency(BooleanAssignmentList.class);
    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);

    public VariableCombinationSpecifictionComputation(IComputation<BooleanAssignmentList> clauseList) {
        super(clauseList, Computations.of(1));
    }

    public VariableCombinationSpecifictionComputation(
            IComputation<BooleanAssignmentList> clauseList, IComputation<Integer> t) {
        super(clauseList, t);
    }

    public VariableCombinationSpecifictionComputation(IComputation<BooleanAssignmentList> clauseList, int t) {
        super(clauseList, Computations.of(t));
    }

    @Override
    public Result<ICombinationSpecification> compute(List<Object> dependencyList, Progress progress) {
        VariableMap variableMap = BOOLEAN_CLAUSE_LIST.get(dependencyList).getVariableMap();
        return Result.of(new VariableCombinationSet(T.get(dependencyList), variableMap.getVariables(), variableMap));
    }
}
