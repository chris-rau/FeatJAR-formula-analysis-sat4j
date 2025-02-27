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

import de.featjar.analysis.sat4j.computation.TWiseCombinations.TWiseCombinationsList;
import de.featjar.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.analysis.sat4j.solver.ModalImplicationGraph;
import de.featjar.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.analysis.sat4j.solver.SAT4JSolver;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
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

    public static final Dependency<TWiseCombinationsList> COMBINATION_SPECS =
            Dependency.newDependency(TWiseCombinationsList.class);

    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);

    public static final Dependency<Integer> CONFIGURATION_LIMIT = Dependency.newDependency(Integer.class);
    public static final Dependency<BooleanAssignmentList> INITIAL_SAMPLE =
            Dependency.newDependency(BooleanAssignmentList.class);

    public static final Dependency<ModalImplicationGraph> MIG = Dependency.newDependency(ModalImplicationGraph.class);

    public static final Dependency<Boolean> ALLOW_CHANGE_TO_INITIAL_SAMPLE = Dependency.newDependency(Boolean.class);
    public static final Dependency<Boolean> INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT =
            Dependency.newDependency(Boolean.class);

    public static final int DEFAULT_T = 1;

    public ATWiseSampleComputation(IComputation<BooleanAssignmentList> clauseList, Object... computations) {
        super(
                clauseList,
                Computations.of(new TWiseCombinationsList()),
                Computations.of(DEFAULT_T),
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

    protected int maxT, maxSampleSize, variableCount;
    protected boolean allowChangeToInitialSample, initialSampleCountsTowardsConfigurationLimit;
    protected TWiseCombinationsList combinationsList;

    protected SAT4JSolutionSolver solver;
    protected VariableMap variableMap;
    protected Random random;
    protected ModalImplicationGraph mig;

    // TODO change to SampleBitIndex
    protected BooleanAssignmentList initialSample;

    @Override
    public final Result<BooleanAssignmentList> compute(List<Object> dependencyList, Progress progress) {
        combinationsList = COMBINATION_SPECS.get(dependencyList);
        maxT = combinationsList.stream().mapToInt(TWiseCombinations::getT).max().orElse(T.get(dependencyList));
        if (maxT < 1) {
            throw new IllegalArgumentException("Value for t must be grater than 0. Value was " + maxT);
        }

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
        maxT = Math.min(maxT, Math.max(variableCount, 1));

        solver = initializeSolver(dependencyList);
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
