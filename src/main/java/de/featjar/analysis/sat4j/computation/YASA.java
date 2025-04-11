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

import de.featjar.analysis.RuntimeContradictionException;
import de.featjar.analysis.RuntimeTimeoutException;
import de.featjar.analysis.sat4j.solver.IMIGVisitor;
import de.featjar.analysis.sat4j.solver.MIGVisitorBitSet;
import de.featjar.analysis.sat4j.solver.MIGVisitorByte;
import de.featjar.analysis.sat4j.solver.ModalImplicationGraph;
import de.featjar.analysis.sat4j.solver.SAT4JAssignment;
import de.featjar.analysis.sat4j.twise.SampleBitIndex;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.BinomialCalculator;
import de.featjar.base.data.Ints;
import de.featjar.base.data.Result;
import de.featjar.base.data.SingleLexicographicIterator;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanSolution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * YASA sampling algorithm. Generates configurations for a given propositional
 * formula such that t-wise feature coverage is achieved.
 *
 * @author Sebastian Krieter
 */
public class YASA extends ATWiseSampleComputation {

    private static class PartialConfiguration {
        private final int id;
        private final boolean allowChange;
        private final IMIGVisitor visitor;

        private int randomCount;

        public PartialConfiguration(int id, boolean allowChange, ModalImplicationGraph mig, int... newliterals) {
            this.id = id;
            this.allowChange = allowChange;
            visitor = new MIGVisitorBitSet(mig);
            if (allowChange) {
                visitor.propagate(newliterals);
            } else {
                visitor.setLiterals(newliterals);
            }
        }

        public int setLiteral(int... literals) {
            final int oldModelCount = visitor.getAddedLiteralCount();
            visitor.propagate(literals);
            return oldModelCount;
        }

        public int countLiterals() {
            return visitor.getAddedLiteralCount();
        }
    }

    public static final Dependency<Integer> ITERATIONS = Dependency.newDependency(Integer.class);
    public static final Dependency<Integer> INTERNAL_SOLUTION_LIMIT = Dependency.newDependency(Integer.class);
    public static final Dependency<Boolean> INCREMENTAL_T = Dependency.newDependency(Boolean.class);

    public YASA(IComputation<BooleanAssignmentList> clauseList) {
        super(clauseList, Computations.of(1), Computations.of(65_536), Computations.of(Boolean.TRUE));
    }

    private int iterations, randomConfigurationLimit, curSolutionId, randomSampleIdsIndex;
    private boolean incrementalT;
    private List<PartialConfiguration> currentSample, selectionCandidates;
    private SampleBitIndex bestSampleIndex, currentSampleIndex, randomSampleIndex;

    @Override
    public Result<BooleanAssignmentList> computeSample(List<Object> dependencyList, Progress progress) {
        iterations = ITERATIONS.get(dependencyList);
        if (iterations < 0) {
            iterations = Integer.MAX_VALUE;
        }

        randomConfigurationLimit = INTERNAL_SOLUTION_LIMIT.get(dependencyList);
        if (randomConfigurationLimit < 0) {
            throw new IllegalArgumentException(
                    "Internal solution limit must be greater than 0. Value was " + randomConfigurationLimit);
        }

        incrementalT = INCREMENTAL_T.get(dependencyList);

        mig = MIG.get(dependencyList);
        randomSampleIndex = new SampleBitIndex(variableCount);

        selectionCandidates = new ArrayList<>();

        final int numberOfCombinationSets = combinationSetList.size();
        int count = 0;

        for (int i = 0; i < numberOfCombinationSets; i++) {
            int maxT = tValues.get(i);
            BooleanAssignment combinationSet = combinationSetList.get(i);
            count += (1 << maxT) * BinomialCalculator.computeBinomial(combinationSet.size(), maxT);
            int minT = incrementalT ? 1 : maxT;
            for (int t = minT; t <= maxT; t++) {
                count += iterations * (1 << t) * BinomialCalculator.computeBinomial(combinationSet.size(), t);
            }
        }
        progress.setTotalSteps(count);

        buildCombinations(progress);
        rebuildCombinations(progress);

        return finalizeResult();
    }

    @Override
    public Result<BooleanAssignmentList> getIntermediateResult() {
        return finalizeResult();
    }

    private Result<BooleanAssignmentList> finalizeResult() {
        currentSample = null;
        currentSampleIndex = null;
        if (bestSampleIndex != null) {
            BooleanAssignmentList result = new BooleanAssignmentList(variableMap, bestSampleIndex.size());
            int initialSize = initialSample.size();
            for (int j = 0; j < initialSize; j++) {
                result.add(new BooleanSolution(bestSampleIndex.getConfiguration(j), false));
            }
            for (int j = bestSampleIndex.size() - 1; j >= initialSize; j--) {
                result.add(autoComplete(Arrays.stream(bestSampleIndex.getConfiguration(j))
                        .filter(l -> l != 0)
                        .toArray()));
            }
            return Result.of(result);
        } else {
            return Result.empty();
        }
    }

    private void buildCombinations(Progress monitor) {
        curSolutionId = 0;
        currentSample = new ArrayList<>();
        currentSampleIndex = new SampleBitIndex(variableCount);
        for (BooleanAssignment config : initialSample) {
            currentSampleIndex.addConfiguration(config);
        }

        for (int i = 0; i < combinationSetList.size(); i++) {
            int t = tValues.get(i);
            final int[] gray = Ints.grayCode(t);
            SingleLexicographicIterator.stream(
                            combinationSetList.get(i).shuffle(random).get(), t)
                    .forEach(combination -> {
                        int[] combinationLiterals = combination.select();
                        for (int g : gray) {
                            checkCancel();
                            monitor.incrementCurrentStep();

                            if (!currentSampleIndex.test(combinationLiterals)
                                    && !isCombinationInvalidMIG(combinationLiterals)
                                    && !interactionFilter.test(combinationLiterals)) {
                                newRandomConfiguration(combinationLiterals);
                            }
                            combinationLiterals[g] = -combinationLiterals[g];
                        }
                    });
        }
        setBestSolutionList();
    }

    private void newRandomConfiguration(final int[] fixedLiterals) {
        int orgAssignmentSize = setUpSolver(fixedLiterals);
        try {
            Result<Boolean> hasSolution = solver.hasSolution();
            if (hasSolution.isPresent()) {
                if (hasSolution.get()) {
                    int[] solution = solver.getInternalSolution();
                    currentSampleIndex.addConfiguration(solution);
                    if (randomSampleIndex.size() < randomConfigurationLimit) {
                        randomSampleIndex.addConfiguration(solution);
                    }
                    solver.shuffleOrder(random);
                }
            } else {
                throw new RuntimeTimeoutException();
            }
        } finally {
            solver.getAssignment().clear(orgAssignmentSize);
        }
    }

    private void rebuildCombinations(Progress monitor) {
        for (int j = 0; j < iterations; j++) {
            curSolutionId = 0;
            currentSample = new ArrayList<>();
            currentSampleIndex = new SampleBitIndex(variableCount);
            for (BooleanAssignment config : initialSample) {
                newConfiguration(config.get(), allowChangeToInitialSample);
            }

            for (int i = 0; i < combinationSetList.size(); i++) {
                int maxT = tValues.get(i);
                int minT = incrementalT ? 1 : maxT;
                for (int t = minT; t <= maxT; t++) {
                    final int[] gray = Ints.grayCode(t);
                    SingleLexicographicIterator.stream(
                                    combinationSetList.get(i).shuffle(random).get(), t)
                            .forEach(combination -> {
                                int[] combinationLiterals = combination.select();
                                for (int g : gray) {
                                    checkCancel();
                                    monitor.incrementCurrentStep();
                                    if (!currentSampleIndex.test(combinationLiterals)
                                            && bestSampleIndex.test(combinationLiterals)
                                            && !interactionFilter.test(combinationLiterals)) {
                                        getSelectionCandidates(combinationLiterals);
                                        if (selectionCandidates.isEmpty()
                                                || (!tryCoverWithRandomSolutions(combinationLiterals)
                                                        && !tryCoverWithSat(combinationLiterals))) {
                                            newConfiguration(combinationLiterals, true);
                                        }
                                        selectionCandidates.clear();
                                    }
                                    combinationLiterals[g] = -combinationLiterals[g];
                                }
                            });
                }
            }
            setBestSolutionList();
        }
    }

    private void setBestSolutionList() {
        if (bestSampleIndex == null || bestSampleIndex.size() > currentSampleIndex.size()) {
            bestSampleIndex = currentSampleIndex;
        }
    }

    private void updateIndex(PartialConfiguration solution, int firstLiteralToConsider) {
        int addedLiteralCount = solution.visitor.getAddedLiteralCount();
        int[] addedLiterals = solution.visitor.getAddedLiterals();
        for (int i = firstLiteralToConsider; i < addedLiteralCount; i++) {
            currentSampleIndex.set(solution.id, addedLiterals[i]);
        }
    }

    private boolean getSelectionCandidates(int[] literals) {
        BitSet negatedBitSet = currentSampleIndex.getNegatedBitSet(literals);
        int nextBit = negatedBitSet.nextClearBit(0);
        while (nextBit < currentSampleIndex.size()) {
            PartialConfiguration configuration = currentSample.get(nextBit);
            if (canBeModified(configuration)) {
                selectionCandidates.add(configuration);
            }
            nextBit = negatedBitSet.nextClearBit(nextBit + 1);
        }
        return false;
    }

    private boolean tryCoverWithRandomSolutions(int[] literals) {
        // TODO use this set for AND
        BitSet literalBitSet = randomSampleIndex.getBitSet(literals);
        if (!literalBitSet.isEmpty()) {
            Collections.sort(
                    selectionCandidates,
                    Comparator.<PartialConfiguration>comparingInt(c -> c.visitor.countUndefined(literals))
                            .thenComparingInt(c -> c.countLiterals()));
            for (PartialConfiguration configuration : selectionCandidates) {
                BitSet configurationBitSet = randomSampleIndex.getBitSet(
                        configuration.visitor.getAddedLiterals(), configuration.visitor.getAddedLiteralCount());
                configuration.randomCount = configurationBitSet.cardinality();
                configurationBitSet.and(literalBitSet);
                if (!configurationBitSet.isEmpty()) {
                    updateIndex(configuration, configuration.setLiteral(literals));
                    return true;
                }
            }
        }
        return false;
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

    private boolean tryCoverWithSat(int[] literals) {
        Collections.sort(selectionCandidates, Comparator.comparingInt(c -> c.randomCount));
        for (PartialConfiguration configuration : selectionCandidates) {
            if (trySelectSat(configuration, literals)) {
                return true;
            }
        }
        return false;
    }

    private void newConfiguration(int[] literals, boolean allowChange) {
        if (currentSample.size() < maxSampleSize) {
            PartialConfiguration newConfiguration =
                    new PartialConfiguration(curSolutionId++, allowChange, mig, literals);
            currentSample.add(newConfiguration);
            currentSampleIndex.addEmptyConfiguration();
            updateIndex(newConfiguration, 0);
        }
    }

    private BooleanSolution autoComplete(int[] configuration) {
        int nextSetBit =
                randomSampleIndex.getBitSet(configuration, configuration.length).nextSetBit(0);
        if (nextSetBit > -1) {
            return new BooleanSolution(randomSampleIndex.getConfiguration(nextSetBit), false);
        } else {
            final int orgAssignmentSize = setUpSolver(configuration);
            try {
                Result<BooleanSolution> hasSolution = solver.findSolution();
                if (hasSolution.isPresent()) {
                    return new BooleanSolution(hasSolution.get());
                } else {
                    throw new RuntimeTimeoutException();
                }
            } finally {
                solver.getAssignment().clear(orgAssignmentSize);
            }
        }
    }

    private boolean canBeModified(PartialConfiguration configuration) {
        return configuration.allowChange && configuration.visitor.getAddedLiteralCount() != variableCount;
    }

    private boolean trySelectSat(PartialConfiguration configuration, final int[] literals) {
        int addedLiteralCount = configuration.visitor.getAddedLiteralCount();
        final int oldModelCount = addedLiteralCount;
        try {
            configuration.visitor.propagate(literals);
        } catch (RuntimeException e) {
            configuration.visitor.reset(oldModelCount);
            return false;
        }

        final int orgAssignmentSize = setUpSolver(configuration);
        try {
            Result<Boolean> hasSolution = solver.hasSolution();
            if (hasSolution.isPresent()) {
                if (hasSolution.get()) {
                    updateIndex(configuration, oldModelCount);
                    randomSampleIdsIndex = (randomSampleIdsIndex + 1) % randomConfigurationLimit;
                    final int[] solution = solver.getInternalSolution();
                    randomSampleIndex.set(randomSampleIdsIndex, solution);
                    solver.shuffleOrder(random);
                    return true;
                } else {
                    configuration.visitor.reset(oldModelCount);
                }
            } else {
                throw new RuntimeTimeoutException();
            }
        } finally {
            solver.getAssignment().clear(orgAssignmentSize);
        }
        return false;
    }

    private int setUpSolver(PartialConfiguration configuration) {
        final int orgAssignmentSize = solver.getAssignment().size();
        int addedLiteralCount = configuration.visitor.getAddedLiteralCount();
        int[] addedLiterals = configuration.visitor.getAddedLiterals();
        for (int i = 0; i < addedLiteralCount; i++) {
            solver.getAssignment().add(addedLiterals[i]);
        }
        return orgAssignmentSize;
    }

    private int setUpSolver(int[] configuration) {
        SAT4JAssignment assignment = solver.getAssignment();
        final int orgAssignmentSize = assignment.size();
        for (int i = 0; i < configuration.length; i++) {
            assignment.add(configuration[i]);
        }
        return orgAssignmentSize;
    }
}
