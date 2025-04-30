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

import de.featjar.base.FeatJAR;
import de.featjar.base.computation.AComputation;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.BinomialCalculator;
import de.featjar.base.data.ICombination;
import de.featjar.base.data.IntegerList;
import de.featjar.base.data.Ints;
import de.featjar.base.data.Result;
import de.featjar.base.data.SingleLexicographicIterator;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public abstract class ATWiseCoverageComputation extends AComputation<CoverageStatistic> {

    public static final Dependency<BooleanAssignmentList> SAMPLE =
            Dependency.newDependency(BooleanAssignmentList.class);
    public static final Dependency<IntegerList> T = Dependency.newDependency(IntegerList.class);
    public static final Dependency<BooleanAssignmentList> EXCLUDE_INTERACTIONS =
            Dependency.newDependency(BooleanAssignmentList.class);
    public static final Dependency<BooleanAssignmentList> COMBINATION_SETS =
            Dependency.newDependency(BooleanAssignmentList.class);

    public ATWiseCoverageComputation(IComputation<BooleanAssignmentList> sample, IComputation<?>... computations) {
        super(
                sample,
                Computations.of(new IntegerList(2)), //
                Computations.of(new BooleanAssignmentList((VariableMap) null)),
                Computations.of(new BooleanAssignmentList((VariableMap) null)),
                computations);
    }

    public ATWiseCoverageComputation(ATWiseCoverageComputation other) {
        super(other);
    }

    protected ArrayList<CoverageStatistic> statisticList = new ArrayList<>();
    protected BooleanAssignmentList combinationSetList;
    protected IntegerList tValues;
    protected SampleListIndex interactionFilter;
    protected BooleanAssignmentList sample;

    protected final void init(List<Object> dependencyList) {
        initWithOriginalVariableMap(dependencyList);

        VariableMap referenceVariableMap = getReferenceVariableMap();
        VariableMap sampleVariableMap = sample.getVariableMap();

        if (combinationSetList.isEmpty()) {
            combinationSetList.adapt(referenceVariableMap);
            combinationSetList.add(referenceVariableMap.getVariables());
        }

        if (!Objects.equals(referenceVariableMap, sampleVariableMap)) {
            FeatJAR.log().warning("Variable maps of given sample and reference are different.");
            VariableMap mergedVariableMap = VariableMap.merge(sampleVariableMap, referenceVariableMap);
            adaptToMergedVariableMap(mergedVariableMap);
        }

        initWithAdaptedVariableMap(dependencyList);
    }

    protected void initWithOriginalVariableMap(List<Object> dependencyList) {
        sample = SAMPLE.get(dependencyList).toSolutionList();
        combinationSetList = COMBINATION_SETS.get(dependencyList);
    }

    protected void initWithAdaptedVariableMap(List<Object> dependencyList) {
        final int numberOfCombinationSets = combinationSetList.size();

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

        interactionFilter = new SampleListIndex(
                EXCLUDE_INTERACTIONS.get(dependencyList).getAll(),
                sample.getVariableMap().getVariableCount());
    }

    protected void adaptToMergedVariableMap(VariableMap mergedVariableMap) {
        combinationSetList.adapt(mergedVariableMap);
        sample.adapt(mergedVariableMap);
    }

    protected VariableMap getReferenceVariableMap() {
        return sample.getVariableMap();
    }

    @Override
    public Result<CoverageStatistic> compute(List<Object> dependencyList, Progress progress) {
        init(dependencyList);

        SampleBitIndex sampleIndex =
                new SampleBitIndex(sample.getAll(), sample.getVariableMap().getVariableCount());
        final int numberOfCombinationSets = combinationSetList.size();
        int count = 0;

        for (int i = 0; i < numberOfCombinationSets; i++) {
            int t = tValues.get(i);
            count += (1 << t)
                    * BinomialCalculator.computeBinomial(
                            combinationSetList.get(i).size(), t);
        }
        progress.setTotalSteps(count);

        for (int i = 0; i < numberOfCombinationSets; i++) {
            int t = tValues.get(i);
            final int[] grayCode = Ints.grayCode(t);
            getCombinationStream(combinationSetList.get(i), t).forEach(c -> {
                final CoverageStatistic statistic = c.environment();
                final int[] interaction = c.select();
                for (int g : grayCode) {
                    checkCancel();
                    progress.incrementCurrentStepSynchronized();
                    if (interactionFilter.test(interaction)) {
                        statistic.incNumberOfIgnoredConditions();
                    } else {
                        if (sampleIndex.test(interaction)) {
                            statistic.incNumberOfCoveredConditions();
                        } else {
                            countUncovered(interaction, statistic);
                        }
                    }
                    interaction[g] = -interaction[g];
                }
            });
        }
        return Result.ofOptional(statisticList.stream() //
                .reduce((s1, s2) -> s1.merge(s2)));
    }

    protected CoverageStatistic createStatistic() {
        CoverageStatistic env = new CoverageStatistic();
        synchronized (statisticList) {
            statisticList.add(env);
        }
        return env;
    }

    protected Stream<ICombination<CoverageStatistic, int[]>> getCombinationStream(BooleanAssignment variables, int t) {
        return SingleLexicographicIterator.parallelStream(variables.get(), t, this::createStatistic);
    }

    protected abstract void countUncovered(int[] uncoveredInteraction, CoverageStatistic statistic);
}
