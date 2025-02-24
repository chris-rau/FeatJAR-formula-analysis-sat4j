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
import de.featjar.analysis.sat4j.computation.ICombinationSpecification;
import de.featjar.analysis.sat4j.computation.MIGBuilder;
import de.featjar.analysis.sat4j.computation.NoneCombinationSpecification;
import de.featjar.analysis.sat4j.computation.SingleCombinationSpecification;
import de.featjar.analysis.sat4j.solver.ISelectionStrategy;
import de.featjar.analysis.sat4j.solver.MIGVisitorByte;
import de.featjar.analysis.sat4j.solver.ModalImplicationGraph;
import de.featjar.analysis.sat4j.solver.SAT4JSolutionSolver;
import de.featjar.analysis.sat4j.solver.SAT4JSolver;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.Result;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

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
    public static final Dependency<ICombinationSpecification> LITERALS =
            Dependency.newDependency(ICombinationSpecification.class);
    public static final Dependency<ModalImplicationGraph> MIG = Dependency.newDependency(ModalImplicationGraph.class);

    public TWiseCoverageComputation(IComputation<BooleanAssignmentList> sample) {
        super(
                Computations.of(new BooleanAssignmentList(null, 0)),
                sample,
                Computations.of(2), //
                Computations.of(new NoneCombinationSpecification()),
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
        int t = T.get(dependencyList);
        ICombinationSpecification variables = LITERALS.get(dependencyList);

        mig = new MIGBuilder(Computations.of(BOOLEAN_CLAUSE_LIST.get(dependencyList))).compute();

        solver = initializeSolver(dependencyList);
        solver.setSelectionStrategy(ISelectionStrategy.random(random));

        int variableCount = sample.getVariableMap().getVariableCount();
        sampleIndex = new SampleBitIndex(sample.getAll(), variableCount);
        randomSampleIndex = new SampleBitIndex(variableCount);
        final CoverageStatistic statistic = new CoverageStatistic(t);

        if (variables instanceof NoneCombinationSpecification) {
            variables = new SingleCombinationSpecification(
                    new BooleanAssignment(new BooleanAssignment(IntStream.range(-variableCount, variableCount + 1)
                                    .filter(i -> i != 0)
                                    .toArray())
                            .removeAllVariables(
                                    Arrays.stream(mig.getCore()).map(Math::abs).toArray())),
                    t);
        }

        progress.setTotalSteps(variables.getTotalSteps());

        variables.shuffle(random);
        variables.stream().forEach(combinationLiterals -> {
            checkCancel();
            progress.incrementCurrentStep();

            if (randomSampleIndex.test(combinationLiterals)) {
                if (sampleIndex.test(combinationLiterals)) {
                    statistic.incNumberOfCoveredConditions();
                } else {
                    statistic.incNumberOfUncoveredConditions();
                }
                return;
            }
            if (isCombinationInvalidMIG(combinationLiterals)) {
                statistic.incNumberOfInvalidConditions();
                return;
            }
            int orgAssignmentSize = solver.getAssignment().size();
            try {
                solver.getAssignment().addAll(combinationLiterals);
                Result<Boolean> hasSolution = solver.hasSolution();
                if (hasSolution.isPresent()) {
                    if (hasSolution.get()) {
                        int[] solution = solver.getInternalSolution();
                        randomSampleIndex.addConfiguration(solution);
                        solver.shuffleOrder(random);
                        if (sampleIndex.test(combinationLiterals)) {
                            statistic.incNumberOfCoveredConditions();
                        } else {
                            statistic.incNumberOfUncoveredConditions();
                        }
                    } else {
                        statistic.incNumberOfInvalidConditions();
                    }
                } else {
                    throw new RuntimeTimeoutException();
                }
            } finally {
                solver.getAssignment().clear(orgAssignmentSize);
            }
        });

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
