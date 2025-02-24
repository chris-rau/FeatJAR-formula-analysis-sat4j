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

import de.featjar.base.computation.IComputation;
import de.featjar.base.data.Combination;
import de.featjar.formula.assignment.BooleanAssignmentList;

/**
 * Calculates statistics regarding t-wise feature coverage of a set of
 * solutions.
 *
 * @author Sebastian Krieter
 */
public class AbsoluteTWiseCoverageComputation extends ATWiseCoverageComputation {

    public AbsoluteTWiseCoverageComputation(IComputation<BooleanAssignmentList> sample) {
        super(sample);
    }

    public AbsoluteTWiseCoverageComputation(AbsoluteTWiseCoverageComputation other) {
        super(other);
    }

    @Override
    protected void count(Combination<Environment> combo) {
        int[] select = combo.getSelection(literals);
        for (int g : gray) {
            if (sampleIndex.test(select)) {
                combo.environment.statistic.incNumberOfCoveredConditions();
            } else {
                combo.environment.statistic.incNumberOfUncoveredConditions();
            }
            select[g] = -select[g];
        }
    }
}
