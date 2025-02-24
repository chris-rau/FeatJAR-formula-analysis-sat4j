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

import de.featjar.base.FeatJAR;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.Dependency;
import de.featjar.base.computation.IComputation;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.log.Log.Verbosity;
import de.featjar.formula.assignment.BooleanAssignmentGroups;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.io.BooleanAssignmentGroupsFormats;
import de.featjar.formula.io.csv.BooleanSolutionListCSVFormat;
import java.nio.file.Path;

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
    public static final Option<Integer> T_OPTION = Option.newOption("t", Option.IntegerParser) //
            .setDescription("Value of parameter t.") //
            .setDefaultValue(2);

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

    // TODO handle other combination specs

    protected IComputation<BooleanAssignmentList> setInitialSample(
            OptionList optionParser,
            IComputation<BooleanAssignmentList> analysis,
            Dependency<BooleanAssignmentList> dependency) {
        Result<Path> initialSamplePath = optionParser.getResult(INITIAL_SAMPLE_OPTION);
        if (initialSamplePath.isPresent()) {
            Result<BooleanAssignmentGroups> initialSample =
                    IO.load(initialSamplePath.get(), BooleanAssignmentGroupsFormats.getInstance());
            if (initialSample.isPresent()) {
                return analysis.set(dependency, initialSample.get().getFirstGroup());
            } else {
                FeatJAR.log().problems(initialSample.getProblems(), Verbosity.WARNING);
            }
        }
        return analysis;
    }

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
