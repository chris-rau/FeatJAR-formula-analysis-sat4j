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
package de.featjar.analysis.sat4j.cli;

import de.featjar.analysis.sat4j.computation.ComputeRandomTWiseSample;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.IComputation;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.util.Optional;

/**
 * Computes solutions for a given formula using SAT4J.
 *
 * @author Sebastian Krieter
 */
public class TWiseCommand extends ATWiseCommand {

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Computes solutions for a given formula using SAT4J. Uses a simple greedy algorithm.");
    }

    @Override
    public IComputation<BooleanAssignmentList> newAnalysis(
            OptionList optionParser, IComputation<BooleanAssignmentList> formula) {
        IComputation<BooleanAssignmentList> analysis = formula.map(ComputeRandomTWiseSample::new)
                .set(ComputeRandomTWiseSample.T, optionParser.get(T_OPTION))
                .set(ComputeRandomTWiseSample.CONFIGURATION_LIMIT, optionParser.get(LIMIT_OPTION))
                .set(
                        ComputeRandomTWiseSample.INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT,
                        optionParser.get(INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT))
                .set(ComputeRandomTWiseSample.RANDOM_SEED, optionParser.get(RANDOM_SEED_OPTION))
                .set(ComputeRandomTWiseSample.SAT_TIMEOUT, optionParser.get(SAT_TIMEOUT_OPTION));
        return setInitialSample(optionParser, analysis, ComputeRandomTWiseSample.INITIAL_SAMPLE);
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("t-wise-sat4j");
    }
}
