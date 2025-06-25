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

import de.featjar.analysis.sat4j.computation.ICombinationSpecification;
import de.featjar.analysis.sat4j.computation.VariableCombinationSpecifictionComputation;
import de.featjar.base.FeatJAR;
import de.featjar.base.computation.AComputation;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.IntegerList;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public abstract class ATWiseCoverageComputation extends AComputation<CoverageStatistic> {

    public static final Dependency<BooleanAssignmentList> SAMPLE =
            Dependency.newDependency(BooleanAssignmentList.class);

    public static final Dependency<ICombinationSpecification> COMBINATION_SET =
            Dependency.newDependency(ICombinationSpecification.class);
    public static final Dependency<IInteractionFilter> EXCLUDE_INTERACTIONS =
            Dependency.newDependency(IInteractionFilter.class);
    public static final Dependency<IInteractionFilter> INCLUDE_INTERACTIONS =
            Dependency.newDependency(IInteractionFilter.class);

    public ATWiseCoverageComputation(IComputation<BooleanAssignmentList> sample, IComputation<?>... computations) {
        super(
                sample,
                new VariableCombinationSpecifictionComputation(sample, Computations.of(1)),
                Computations.of(IInteractionFilter.of(false)),
                Computations.of(IInteractionFilter.of(true)),
                computations);
    }

    public ATWiseCoverageComputation(ATWiseCoverageComputation other) {
        super(other);
    }

    protected ArrayList<CoverageStatistic> statisticList = new ArrayList<>();
    protected ICombinationSpecification combinationSet;
    protected IntegerList tValues;
    protected IInteractionFilter excludeFilter;
    protected IInteractionFilter includeFilter;
    protected BooleanAssignmentList sample;

    protected final void init(List<Object> dependencyList) {
        initWithOriginalVariableMap(dependencyList);

        VariableMap referenceVariableMap = getReferenceVariableMap();
        VariableMap sampleVariableMap = sample.getVariableMap();

        if (!Objects.equals(referenceVariableMap, sampleVariableMap)) {
            FeatJAR.log().warning("Variable maps of given sample and reference are different.");
            VariableMap mergedVariableMap = VariableMap.merge(sampleVariableMap, referenceVariableMap);
            adaptToMergedVariableMap(mergedVariableMap);
        }
        combinationSet.adapt(sample.getVariableMap());

        adaptVariableMap(dependencyList);
    }

    protected void initWithOriginalVariableMap(List<Object> dependencyList) {
        sample = SAMPLE.get(dependencyList).toSolutionList();
        combinationSet = COMBINATION_SET.get(dependencyList);
    }

    protected void adaptVariableMap(List<Object> dependencyList) {
        excludeFilter = EXCLUDE_INTERACTIONS.get(dependencyList).adapt(sample.getVariableMap());
        includeFilter = INCLUDE_INTERACTIONS.get(dependencyList).adapt(sample.getVariableMap());
    }

    protected void adaptToMergedVariableMap(VariableMap mergedVariableMap) {
        sample.adapt(mergedVariableMap);
    }

    protected VariableMap getReferenceVariableMap() {
        return combinationSet.variableMap();
    }

    @Override
    public Result<CoverageStatistic> compute(List<Object> dependencyList, Progress progress) {
        init(dependencyList);

        SampleBitIndex sampleIndex = new SampleBitIndex(sample.getAll(), sample.getVariableMap());

        progress.setTotalSteps(combinationSet.loopCount());

        process(
                combinationSet,
                (statistic, interaction) -> {
                    checkCancel();
                    progress.incrementCurrentStep();
                    if (excludeFilter.test(interaction) || !includeFilter.test(interaction)) {
                        statistic.incNumberOfIgnoredConditions();
                    } else {
                        if (sampleIndex.test(interaction)) {
                            statistic.incNumberOfCoveredConditions();
                        } else {
                            countUncovered(interaction, statistic);
                        }
                    }
                },
                this::createStatistic);
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

    protected void process(
            ICombinationSpecification combinationSet,
            BiConsumer<CoverageStatistic, int[]> consumer,
            Supplier<CoverageStatistic> environmentCreator) {
        combinationSet.forEachParallel(consumer, environmentCreator);
    }

    protected abstract void countUncovered(int[] uncoveredInteraction, CoverageStatistic statistic);
}
