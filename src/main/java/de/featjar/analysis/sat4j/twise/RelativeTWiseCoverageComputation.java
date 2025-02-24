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

import de.featjar.base.computation.Computations;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.data.Combination;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.util.List;
import java.util.Objects;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class RelativeTWiseCoverageComputation extends ATWiseCoverageComputation {
    public static final Dependency<BooleanAssignmentList> REFERENCE_SAMPLE =
            Dependency.newDependency(BooleanAssignmentList.class);

    public RelativeTWiseCoverageComputation(IComputation<BooleanAssignmentList> sample) {
        super(sample, Computations.of(new BooleanAssignmentList(null, 0)));
    }

    public RelativeTWiseCoverageComputation(RelativeTWiseCoverageComputation other) {
        super(other);
    }

    private SampleBitIndex referenceIndex;

    @Override
    protected void init(List<Object> dependencyList) {
        super.init(dependencyList);
        BooleanAssignmentList referenceSample =
                REFERENCE_SAMPLE.get(dependencyList).toSolutionList();
        if (!Objects.equals(referenceSample.getVariableMap(), sample.getVariableMap())) {
            throw new IllegalArgumentException("Variable map of reference sample is different from given sample.");
        }
        referenceIndex = new SampleBitIndex(referenceSample.getAll(), size);
    }

    @Override
    protected void count(Combination<Environment> combo) {
        int[] select = combo.getSelection(literals);
        for (int g : gray) {
            if (referenceIndex.test(select)) {
                if (sampleIndex.test(select)) {
                    combo.environment.statistic.incNumberOfCoveredConditions();
                } else {
                    combo.environment.statistic.incNumberOfUncoveredConditions();
                }
            } else {
                combo.environment.statistic.incNumberOfInvalidConditions();
            }
            select[g] = -select[g];
        }
    }
}
