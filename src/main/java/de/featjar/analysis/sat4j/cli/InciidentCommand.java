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

import de.featjar.analysis.ExternalConfigurationTester;
import de.featjar.analysis.sat4j.computation.ATWiseSampleComputation;
import de.featjar.analysis.sat4j.computation.Inciident;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.IComputation;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.log.Log.Verbosity;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentGroups;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.io.BooleanAssignmentGroupsFormats;
import de.featjar.formula.io.csv.BooleanSolutionListCSVFormat;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Detects partial configurations causing a given property of a configuration.
 *
 * @author Sebastian Krieter
 */
public abstract class InciidentCommand extends ASAT4JAnalysisCommand<BooleanAssignment, BooleanAssignmentList> {

    /**
     * Maximum number of tests to be performed.
     */
    public static final Option<Integer> LIMIT_OPTION = Option.newOption("n", Option.IntegerParser) //
            .setDescription("Maximum number of tests to be performed.") //
            .setRequired(false)
            .setDefaultValue(Integer.MAX_VALUE);

    /**
     * Value of t.
     */
    public static final Option<Integer> T_OPTION = Option.newOption("t", Option.IntegerParser) //
            .setDescription("Value(s) of parameter t.") //
            .setRequired(true)
            .setDefaultValue(1);

    /**
     * Path option for initial sample.
     */
    public static final Option<Path> INITIAL_SAMPLE_OPTION = Option.newOption("initial-sample", Option.PathParser)
            .setDescription("Path to initial sample file.")
            .setRequired(false)
            .setDefaultValue(null)
            .setValidator(Option.PathValidator);

    /**
     * Path option for initial sample.
     */
    public static final Option<Path> TESTER_EXECUTABLE = Option.newOption("verifier", Option.PathParser)
            .setDescription("Path to initial sample file.")
            .setRequired(true)
            .setValidator(Option.PathValidator);

    @Override
    public IComputation<BooleanAssignment> newAnalysis(
            OptionList optionParser, IComputation<BooleanAssignmentList> formula) {
        IComputation<BooleanAssignment> analysis = formula.map(Inciident::new)
                .set(Inciident.T, optionParser.get(T_OPTION))
                .set(Inciident.TESTING_LIMIT, optionParser.get(LIMIT_OPTION))
                .set(Inciident.RANDOM_SEED, optionParser.get(RANDOM_SEED_OPTION))
                .set(Inciident.SAT_TIMEOUT, optionParser.get(SAT_TIMEOUT_OPTION))
                .set(Inciident.TESTER, new ExternalConfigurationTester(optionParser.get(TESTER_EXECUTABLE)));

        Result<Path> initialSamplePath = optionParser.getResult(INITIAL_SAMPLE_OPTION);
        if (initialSamplePath.isPresent()) {
            BooleanAssignmentGroups initialSample = IO.load(
                            initialSamplePath.get(), BooleanAssignmentGroupsFormats.getInstance())
                    .orElseLog(Verbosity.WARNING);
            if (initialSample != null) {
                analysis.set(ATWiseSampleComputation.INITIAL_SAMPLE, initialSample.getFirstGroup());
            }
        }

        return analysis;
    }

    protected abstract IComputation<BooleanAssignmentList> newTWiseAnalysis(
            OptionList optionParser, IComputation<BooleanAssignmentList> formula);

    @Override
    protected IFormat<?> getOuputFormat() {
        return new BooleanSolutionListCSVFormat();
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Detects partial configurations causing a given property of a configuration.");
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("inciident");
    }
}
