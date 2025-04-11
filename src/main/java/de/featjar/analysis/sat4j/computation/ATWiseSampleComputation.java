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

import de.featjar.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.analysis.sat4j.solver.ModalImplicationGraph;
import de.featjar.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.analysis.sat4j.solver.SAT4JSolver;
import de.featjar.analysis.sat4j.twise.SampleListIndex;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.IntegerList;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.util.List;
import java.util.Random;

/**
 * YASA sampling algorithm. Generates configurations for a given propositional
 * formula such that t-wise feature coverage is achieved.
 *
 * @author Sebastian Krieter
 */
public abstract class ATWiseSampleComputation extends ASAT4JAnalysis<BooleanAssignmentList> {

    public static final Dependency<BooleanAssignmentList> COMBINATION_SETS =
            Dependency.newDependency(BooleanAssignmentList.class);

    public static final Dependency<IntegerList> T = Dependency.newDependency(IntegerList.class);

    public static final Dependency<BooleanAssignmentList> EXCLUDE_INTERACTIONS =
            Dependency.newDependency(BooleanAssignmentList.class);

    public static final Dependency<Integer> CONFIGURATION_LIMIT = Dependency.newDependency(Integer.class);
    public static final Dependency<BooleanAssignmentList> INITIAL_SAMPLE =
            Dependency.newDependency(BooleanAssignmentList.class);

    public static final Dependency<ModalImplicationGraph> MIG = Dependency.newDependency(ModalImplicationGraph.class);

    public static final Dependency<Boolean> ALLOW_CHANGE_TO_INITIAL_SAMPLE = Dependency.newDependency(Boolean.class);
    public static final Dependency<Boolean> INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT =
            Dependency.newDependency(Boolean.class);

    public ATWiseSampleComputation(IComputation<BooleanAssignmentList> clauseList, Object... computations) {
        super(
                clauseList,
                Computations.of(new BooleanAssignmentList((VariableMap) null)),
                Computations.of(new IntegerList(1)),
                Computations.of(new BooleanAssignmentList((VariableMap) null)),
                Computations.of(Integer.MAX_VALUE),
                Computations.of(new BooleanAssignmentList((VariableMap) null)),
                new MIGBuilder(clauseList),
                Computations.of(Boolean.TRUE),
                Computations.of(Boolean.TRUE),
                computations);
    }

    protected ATWiseSampleComputation(ATWiseSampleComputation other) {
        super(other);
    }

    protected int maxSampleSize, variableCount;
    protected boolean allowChangeToInitialSample, initialSampleCountsTowardsConfigurationLimit;
    protected BooleanAssignmentList combinationSetList;
    protected IntegerList tValues;
    protected SampleListIndex interactionFilter;

    protected SAT4JSolutionSolver solver;
    protected VariableMap variableMap;
    protected Random random;
    protected ModalImplicationGraph mig;

    // TODO change to SampleBitIndex
    protected BooleanAssignmentList initialSample;

    @Override
    public final Result<BooleanAssignmentList> compute(List<Object> dependencyList, Progress progress) {
        maxSampleSize = CONFIGURATION_LIMIT.get(dependencyList);
        if (maxSampleSize < 0) {
            throw new IllegalArgumentException(
                    "Configuration limit must be greater than 0. Value was " + maxSampleSize);
        }

        initialSample = INITIAL_SAMPLE.get(dependencyList);

        random = new Random(RANDOM_SEED.get(dependencyList));

        allowChangeToInitialSample = ALLOW_CHANGE_TO_INITIAL_SAMPLE.get(dependencyList);
        initialSampleCountsTowardsConfigurationLimit =
                INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT.get(dependencyList);

        variableMap = BOOLEAN_CLAUSE_LIST.get(dependencyList).getVariableMap();
        variableCount = variableMap.getVariableCount();

        combinationSetList = COMBINATION_SETS.get(dependencyList);
        if (combinationSetList.isEmpty()) {
            combinationSetList.adapt(variableMap);
            combinationSetList.add(variableMap.getVariables());
        }
        int numberOfCombinationSets = combinationSetList.size();

        interactionFilter =
                new SampleListIndex(EXCLUDE_INTERACTIONS.get(dependencyList).getAll(), variableCount);

        tValues = T.get(dependencyList);
        if (tValues.size() < 1) {
            throw new IllegalArgumentException("List of t values must contain at least one value. Was empty.");
        }
        if (tValues.size() > 1) {
            if (tValues.size() != numberOfCombinationSets) {
                throw new IllegalArgumentException(String.format(
                        "Number of t values (%d) must be one or equal to number of combinations sets (%d).",
                        tValues.size(), combinationSetList.size()));
            }
        } else {
            int t = tValues.get(0);
            for (int i = 1; i < numberOfCombinationSets; i++) {
                tValues.addAll(t);
            }
        }

        for (int i = 0; i < numberOfCombinationSets; i++) {
            int t = tValues.get(i);
            if (t < 1) {
                throw new IllegalArgumentException(
                        String.format("Value for t must be greater than 0. Value was %d.", +t));
            }
            BooleanAssignment variables = combinationSetList.get(i);
            if (variables.size() < t) {
                throw new IllegalArgumentException(String.format(
                        "Value for t (%d) must be greater than number of variables (%d).", t, variables.size()));
            }
            for (int v : variables.get()) {
                if (v <= 0) {
                    throw new IllegalArgumentException(
                            String.format("Variable ID must not be negative or zero. Was %d", v));
                }
            }
        }

        solver = createSolver(dependencyList);
        solver.setSelectionStrategy(ISelectionStrategy.random(random));

        if (initialSampleCountsTowardsConfigurationLimit) {
            maxSampleSize = Math.max(maxSampleSize, maxSampleSize + initialSample.size());
        }

        return computeSample(dependencyList, progress);
    }

    public abstract Result<BooleanAssignmentList> computeSample(List<Object> dependencyList, Progress progress);

    @Override
    protected SAT4JSolver newSolver(BooleanAssignmentList clauseList) {
        return new SAT4JSolutionSolver(clauseList);
    }
}
