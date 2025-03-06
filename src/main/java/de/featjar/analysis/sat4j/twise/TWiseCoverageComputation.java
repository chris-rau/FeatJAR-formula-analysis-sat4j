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
package de.featjar.analysis.sat4j.twise;

import de.featjar.analysis.RuntimeContradictionException;
import de.featjar.analysis.RuntimeTimeoutException;
import de.featjar.analysis.sat4j.computation.ASAT4JAnalysis;
import de.featjar.analysis.sat4j.computation.MIGBuilder;
import de.featjar.analysis.sat4j.computation.TWiseCombinations;
import de.featjar.analysis.sat4j.computation.TWiseCombinations.TWiseCombinationsList;
import de.featjar.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.analysis.sat4j.solver.MIGVisitorByte;
import de.featjar.analysis.sat4j.solver.ModalImplicationGraph;
import de.featjar.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.analysis.sat4j.solver.SAT4JSolver;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.Ints;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class TWiseCoverageComputation extends ASAT4JAnalysis<CoverageStatistic> {

    public static final Dependency<BooleanAssignmentList> SAMPLE =
            Dependency.newDependency(BooleanAssignmentList.class);
    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);
    public static final Dependency<TWiseCombinationsList> COMBINATION_SPECS =
            Dependency.newDependency(TWiseCombinationsList.class);
    public static final Dependency<ModalImplicationGraph> MIG = Dependency.newDependency(ModalImplicationGraph.class);

    public TWiseCoverageComputation(IComputation<BooleanAssignmentList> sample) {
        super(
                Computations.of(new BooleanAssignmentList(null, 0)),
                sample,
                Computations.of(2), //
                Computations.of(new TWiseCombinationsList()),
                Computations.of(new Object()));
    }

    public TWiseCoverageComputation(TWiseCoverageComputation other) {
        super(other);
    }

    private SampleBitIndex sampleIndex, randomSampleIndex;
    private Random random;
    private ModalImplicationGraph mig;
    private SAT4JSolutionSolver solver;

    @Override
    public Result<CoverageStatistic> compute(List<Object> dependencyList, Progress progress) {
        BooleanAssignmentList sample = SAMPLE.get(dependencyList).toSolutionList();
        random = new Random(RANDOM_SEED.get(dependencyList));

        TWiseCombinationsList combinationsList = COMBINATION_SPECS.get(dependencyList);
        int t = combinationsList.stream()
                .mapToInt(TWiseCombinations::getT)
                .max()
                .orElse(T.get(dependencyList));

        BooleanAssignmentList clauseList = BOOLEAN_CLAUSE_LIST.get(dependencyList);
        BooleanAssignment assumedAssignment = ASSUMED_ASSIGNMENT.get(dependencyList);
        BooleanAssignmentList assumedClauseList = ASSUMED_CLAUSE_LIST.get(dependencyList);
        Duration timeout = SAT_TIMEOUT.get(dependencyList);

        VariableMap cnfVariableMap = clauseList.getVariableMap();
        VariableMap sampleVariableMap = sample.getVariableMap();

        if (Objects.equals(cnfVariableMap, sampleVariableMap)) {
            solver = initializeSolver(clauseList, assumedAssignment, assumedClauseList, timeout);
        } else {
            if (cnfVariableMap.containsAllObjects(sampleVariableMap)
                    && sampleVariableMap.containsAllObjects(cnfVariableMap)) {
                clauseList = clauseList.adapt(sampleVariableMap);
                solver = initializeSolver(
                        clauseList,
                        assumedAssignment.adapt(cnfVariableMap, sampleVariableMap),
                        assumedClauseList.adapt(sampleVariableMap),
                        timeout);
            } else {
                throw new IllegalArgumentException("Variable map of feature model is different from given sample.");
            }
        }
        solver.setSelectionStrategy(ISelectionStrategy.random(random));
        mig = new MIGBuilder(Computations.of(clauseList)).compute();

        int variableCount = sampleVariableMap.getVariableCount();
        sampleIndex = new SampleBitIndex(sample.getAll(), variableCount);
        randomSampleIndex = new SampleBitIndex(variableCount);
        final CoverageStatistic statistic = new CoverageStatistic(t);

        if (combinationsList.isEmpty()) {
            combinationsList.add(new TWiseCombinations(sampleVariableMap.getVariables(), t));
        }

        for (TWiseCombinations combinations : combinationsList) {
            progress.setTotalSteps((1 << combinations.getT()) * combinations.getTotalSteps());
        }

        for (TWiseCombinations combinations : combinationsList) {
            int[] grayCode = Ints.grayCode(combinations.getT());
            combinations.shuffle(random);
            combinations.stream().forEach(combinationLiterals -> {
                for (int g : grayCode) {
                    checkCancel();
                    progress.incrementCurrentStep();
                    if (randomSampleIndex.test(combinationLiterals)) {
                        if (sampleIndex.test(combinationLiterals)) {
                            statistic.incNumberOfCoveredConditions();
                        } else {
                            statistic.incNumberOfUncoveredConditions();
                        }
                    } else if (isCombinationInvalidMIG(combinationLiterals)) {
                        statistic.incNumberOfInvalidConditions();
                    } else {
                        int orgAssignmentSize = solver.getAssignment().size();
                        solver.getAssignment().addAll(combinationLiterals);
                        try {
                            Result<Boolean> hasSolution = solver.hasSolution();
                            if (hasSolution.isPresent()) {
                                if (hasSolution.get()) {
                                    int[] solution = solver.getInternalSolution();
                                    randomSampleIndex.addConfiguration(solution);
                                    if (sampleIndex.test(combinationLiterals)) {
                                        statistic.incNumberOfCoveredConditions();
                                    } else {
                                        statistic.incNumberOfUncoveredConditions();
                                    }
                                    solver.shuffleOrder(random);
                                } else {
                                    statistic.incNumberOfInvalidConditions();
                                }
                            } else {
                                throw new RuntimeTimeoutException();
                            }
                        } finally {
                            solver.getAssignment().clear(orgAssignmentSize);
                        }
                    }
                    combinationLiterals[g] = -combinationLiterals[g];
                }
            });
        }

        return Result.of(statistic);
    }

    private boolean isCombinationInvalidMIG(int[] literals) {
        try {
            MIGVisitorByte visitor = new MIGVisitorByte(mig);
            visitor.propagate(literals);
        } catch (RuntimeContradictionException e) {
            return true;
        }
        return false;
    }

    @Override
    protected SAT4JSolver newSolver(BooleanAssignmentList clauseList) {
        return new SAT4JSolutionSolver(clauseList);
    }
}
