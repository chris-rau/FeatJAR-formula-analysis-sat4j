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

import de.featjar.base.computation.AComputation;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.computation.Progress;
import de.featjar.base.data.Combination;
import de.featjar.base.data.Ints;
import de.featjar.base.data.LexicographicIterator;
import de.featjar.base.data.Result;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public abstract class ATWiseCoverageComputation extends AComputation<CoverageStatistic> {

    public static final Dependency<BooleanAssignmentList> SAMPLE =
            Dependency.newDependency(BooleanAssignmentList.class);
    public static final Dependency<Integer> T = Dependency.newDependency(Integer.class);
    public static final Dependency<BooleanAssignment> FILTER = Dependency.newDependency(BooleanAssignment.class);

    public class Environment {
        final CoverageStatistic statistic = new CoverageStatistic(t);

        public CoverageStatistic getStatistic() {
            return statistic;
        }
    }

    public ATWiseCoverageComputation(IComputation<BooleanAssignmentList> sample, IComputation<?>... computations) {
        super(
                sample,
                Computations.of(2), //
                Computations.of(new BooleanAssignment()),
                computations);
    }

    public ATWiseCoverageComputation(ATWiseCoverageComputation other) {
        super(other);
    }

    protected ArrayList<Environment> statisticList = new ArrayList<>();
    protected BooleanAssignmentList sample;
    protected int t, size;

    protected SampleBitIndex sampleIndex;
    protected int[] literals;
    protected int[] gray;

    @Override
    public Result<CoverageStatistic> compute(List<Object> dependencyList, Progress progress) {
        init(dependencyList);
        LexicographicIterator.parallelStream(t, literals.length, this::createStatistic)
                .forEach(this::count);
        return Result.ofOptional(statisticList.stream() //
                .map(Environment::getStatistic) //
                .reduce((s1, s2) -> s1.merge(s2)));
    }

    protected void init(List<Object> dependencyList) {
        sample = SAMPLE.get(dependencyList).toSolutionList();
        t = T.get(dependencyList);
        size = sample.getVariableMap().getVariableCount();

        sampleIndex = new SampleBitIndex(sample.getAll(), size);

        literals = Ints.filteredList(size, FILTER.get(dependencyList));
        gray = Ints.grayCode(t);
    }

    protected abstract void count(Combination<Environment> combo);

    private Environment createStatistic() {
        Environment env = new Environment();
        synchronized (statisticList) {
            statisticList.add(env);
        }
        return env;
    }
}
