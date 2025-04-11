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

import de.featjar.analysis.sat4j.computation.ATWiseSampleComputation;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.IComputation;
import de.featjar.base.data.IntegerList;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.log.Log.Verbosity;
import de.featjar.formula.assignment.BooleanAssignmentGroups;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.io.BooleanAssignmentGroupsFormats;
import de.featjar.formula.io.csv.BooleanSolutionListCSVFormat;
import java.nio.file.Path;
import java.util.List;

/**
 * Computes solutions for a given formula using SAT4J.
 *
 * @author Sebastian Krieter
 */
public abstract class ATWiseCommand extends ASAT4JAnalysisCommand<BooleanAssignmentList, BooleanAssignmentList> {

    /**
     * Maximum number of configurations to be generated.
     */
    public static final Option<Integer> LIMIT_OPTION = Option.newOption("n", Option.IntegerParser) //
            .setDescription("Maximum number of configurations to be generated.") //
            .setDefaultValue(Integer.MAX_VALUE);

    /**
     * Value of t.
     */
    public static final Option<List<Integer>> T_OPTION = Option.newListOption("t", Option.IntegerParser) //
            .setDescription("Value(s) of parameter t.") //
            .setDefaultValue(List.of(2));

    /**
     * Path option for combination sets.
     */
    public static final Option<Path> COMBINATION_SETS = Option.newOption("combination-sets", Option.PathParser)
            .setRequired(false)
            .setDefaultValue(null)
            .setDescription("Path to combination specification files.");

    public static final Option<Path> INGNORE_INTERACTIONS = Option.newOption("ignore-interactions", Option.PathParser)
            .setDescription("Path to list of interactions that will be ignored.")
            .setValidator(Option.PathValidator);

    /**
     * Path option for initial sample.
     */
    public static final Option<Path> INITIAL_SAMPLE_OPTION = Option.newOption("initial-sample", Option.PathParser)
            .setRequired(false)
            .setDefaultValue(null)
            .setDescription("Path to initial sample file.")
            .setValidator(Option.PathValidator);

    /**
     * Flag to determine whether the initial sample counts towards the global configuration limit.
     */
    public static final Option<Boolean> INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT = Option.newFlag(
                    "initial-sample-limit")
            .setDescription("If set, the initial sample counts towards the global configuration limit.");

    @Override
    public IComputation<BooleanAssignmentList> newAnalysis(
            OptionList optionParser, IComputation<BooleanAssignmentList> formula) {
        IComputation<BooleanAssignmentList> analysis = newTWiseAnalysis(optionParser, formula)
                .set(ATWiseSampleComputation.T, new IntegerList(optionParser.get(T_OPTION)))
                .set(ATWiseSampleComputation.CONFIGURATION_LIMIT, optionParser.get(LIMIT_OPTION))
                .set(
                        ATWiseSampleComputation.INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT,
                        optionParser.get(INITIAL_SAMPLE_COUNTS_TOWARDS_CONFIGURATION_LIMIT))
                .set(ATWiseSampleComputation.RANDOM_SEED, optionParser.get(RANDOM_SEED_OPTION))
                .set(ATWiseSampleComputation.SAT_TIMEOUT, optionParser.get(SAT_TIMEOUT_OPTION));

        Result<Path> initialSamplePath = optionParser.getResult(INITIAL_SAMPLE_OPTION);
        if (initialSamplePath.isPresent()) {
            BooleanAssignmentGroups initialSample = IO.load(
                            initialSamplePath.get(), BooleanAssignmentGroupsFormats.getInstance())
                    .orElseLog(Verbosity.WARNING);
            if (initialSample != null) {
                analysis.set(ATWiseSampleComputation.INITIAL_SAMPLE, initialSample.getFirstGroup());
            }
        }

        Result<Path> combinationSpecsPath = optionParser.getResult(COMBINATION_SETS);
        if (combinationSpecsPath.isPresent()) {
            BooleanAssignmentGroups tWiseCombinationsList = IO.load(
                            combinationSpecsPath.get(), new BooleanAssignmentGroupsFormats())
                    .orElseLog(Verbosity.WARNING);
            if (tWiseCombinationsList != null) {
                analysis.set(ATWiseSampleComputation.COMBINATION_SETS, tWiseCombinationsList.getMergedGroups());
            }
        }

        Result<Path> ignoreInteractionsPath = optionParser.getResult(INGNORE_INTERACTIONS);
        if (ignoreInteractionsPath.isPresent()) {
            BooleanAssignmentGroups ignoreInteractions = IO.load(
                            ignoreInteractionsPath.get(), new BooleanAssignmentGroupsFormats())
                    .orElseLog(Verbosity.WARNING);
            if (ignoreInteractions != null) {
                analysis.set(ATWiseSampleComputation.EXCLUDE_INTERACTIONS, ignoreInteractions.getMergedGroups());
            }
        }

        return analysis;
    }

    protected abstract IComputation<BooleanAssignmentList> newTWiseAnalysis(
            OptionList optionParser, IComputation<BooleanAssignmentList> formula);

    @Override
    protected Object getOuputObject(BooleanAssignmentList list) {
        return new BooleanAssignmentGroups(list);
    }

    @Override
    protected IFormat<?> getOuputFormat() {
        return new BooleanSolutionListCSVFormat();
    }

    @Override
    public String printResult(BooleanAssignmentList assignments) {
        return assignments.serialize();
    }
}
