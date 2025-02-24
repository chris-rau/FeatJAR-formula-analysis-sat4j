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

import de.featjar.analysis.sat4j.computation.YASALegacy;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.IComputation;
import de.featjar.formula.assignment.BooleanAssignmentList;
import java.util.Optional;

/**
 * Computes solutions for a given formula using SAT4J.
 *
 * @author Sebastian Krieter
 * @author Andreas Gerasimow
 */
public class LegacyYASACommand extends ATWiseCommand {

    /**
     * Number of iterations.
     */
    public static final Option<Integer> ITERATIONS_OPTION = Option.newOption("i", Option.IntegerParser) //
            .setDescription("Number of iterations.") //
            .setDefaultValue(1);

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Computes solutions for a given formula using SAT4J. Uses YASA.");
    }

    @Override
    public IComputation<BooleanAssignmentList> newAnalysis(
            OptionList optionParser, IComputation<BooleanAssignmentList> formula) {
        IComputation<BooleanAssignmentList> analysis = formula.map(YASALegacy::new)
                .set(YASALegacy.T, optionParser.get(T_OPTION))
                .set(YASALegacy.CONFIGURATION_LIMIT, optionParser.get(LIMIT_OPTION))
                .set(YASALegacy.ITERATIONS, optionParser.get(ITERATIONS_OPTION))
                .set(
                        YASALegacy.INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT,
                        optionParser.get(INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT))
                .set(YASALegacy.RANDOM_SEED, optionParser.get(RANDOM_SEED_OPTION))
                .set(YASALegacy.SAT_TIMEOUT, optionParser.get(SAT_TIMEOUT_OPTION));
        return setInitialSample(optionParser, analysis, YASALegacy.INITIAL_SAMPLE);
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("yasa-legacy");
    }
}
