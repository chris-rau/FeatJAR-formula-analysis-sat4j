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

import de.featjar.analysis.AAnalysisCommand;
import de.featjar.analysis.sat4j.twise.ATWiseCoverageComputation;
import de.featjar.analysis.sat4j.twise.AbsoluteTWiseCoverageComputation;
import de.featjar.analysis.sat4j.twise.ConstraintedCoverageComputation;
import de.featjar.analysis.sat4j.twise.CoverageStatistic;
import de.featjar.analysis.sat4j.twise.RelativeTWiseCoverageComputation;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.IComputation;
import de.featjar.base.data.IntegerList;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.text.StringTextFormat;
import de.featjar.base.log.Log.Verbosity;
import de.featjar.formula.assignment.BooleanAssignmentGroups;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.ComputeBooleanClauseList;
import de.featjar.formula.computation.ComputeCNFFormula;
import de.featjar.formula.computation.ComputeNNFFormula;
import de.featjar.formula.io.BooleanAssignmentGroupsFormats;
import de.featjar.formula.io.FormulaFormats;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Computes atomic sets for a given formula using SAT4J.
 *
 * @author Elias Kuiter
 * @author Sebastian Krieter
 * @author Andreas Gerasimow
 */
public class TWiseCoverageCommand extends AAnalysisCommand<CoverageStatistic> {

    /**
     * Input option for feature model path.
     */
    public static final Option<Path> FM_OPTION = Option.newOption("fm", Option.PathParser)
            .setDescription("Path to feature model. Cannot be chosen together with --ref")
            .setValidator(Option.PathValidator);

    /**
     * Input option for feature model path.
     */
    public static final Option<Path> REFERENCE_SAMPLE_OPTION = Option.newOption("ref", Option.PathParser)
            .setDescription("Path to reference sample. Cannot be chosen together with --fm")
            .setValidator(Option.PathValidator);

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

    public static final Option<Boolean> COVERAGE_ONLY_OPTION = Option.newFlag("coverage-only") //
            .setDescription("Shows only coverage value.");

    public static final Option<Boolean> COUNT_ONLY_OPTION = Option.newFlag("count-only") //
            .setDescription("Shows only the interaction count: covered, uncovered, invalid, ignored (line separated)");

    private boolean coverageOnly, countOnly;

    @Override
    public Optional<String> getDescription() {
        return Optional.of(
                "Computes the t-wise coverage of a given sample. To calculate the number of invalid interactions either a feature model or a reference sample must be provided.");
    }

    @Override
    protected IComputation<CoverageStatistic> newComputation(OptionList optionParser) {
        coverageOnly = optionParser.getResult(COVERAGE_ONLY_OPTION).orElseThrow();
        countOnly = optionParser.getResult(COUNT_ONLY_OPTION).orElseThrow();
        Path samplePath = optionParser.getResult(INPUT_OPTION).orElseThrow();

        if (optionParser.has(FM_OPTION) && optionParser.has(REFERENCE_SAMPLE_OPTION)) {
            throw new IllegalArgumentException("Cannot set " + FM_OPTION.getArgumentName() + " and "
                    + REFERENCE_SAMPLE_OPTION.getArgumentName() + " at the same time!");
        }

        Path fmPath = optionParser.getResult(FM_OPTION).orElse(null);
        Path referencePath = optionParser.getResult(REFERENCE_SAMPLE_OPTION).orElse(null);
        IntegerList ts = new IntegerList(optionParser.get(T_OPTION));

        if (fmPath != null && referencePath != null) {
            throw new IllegalArgumentException("Cannot set " + FM_OPTION.getArgumentName() + " and "
                    + REFERENCE_SAMPLE_OPTION.getArgumentName() + " at the same time!");
        }

        IComputation<BooleanAssignmentList> sample = IO.load(samplePath, BooleanAssignmentGroupsFormats.getInstance())
                .map(BooleanAssignmentGroups::getFirstGroup)
                .toComputation();

        IComputation<CoverageStatistic> coverageComputation;
        if (fmPath != null) {
            coverageComputation = computeFMCoverage(sample, fmPath);
        } else if (referencePath != null) {
            coverageComputation = computeRelativeCoverage(sample, referencePath);
        } else {
            coverageComputation = computeAbsoluteCoverage(sample);
        }
        coverageComputation.set(ATWiseCoverageComputation.T, ts);

        Result<Path> combinationSpecsPath = optionParser.getResult(COMBINATION_SETS);
        if (combinationSpecsPath.isPresent()) {
            BooleanAssignmentGroups tWiseCombinationsList = IO.load(
                            combinationSpecsPath.get(), new BooleanAssignmentGroupsFormats())
                    .orElseLog(Verbosity.WARNING);
            if (tWiseCombinationsList != null) {
                coverageComputation.set(
                        ATWiseCoverageComputation.COMBINATION_SETS, tWiseCombinationsList.getMergedGroups());
            }
        }

        Result<Path> ignoreInteractionsPath = optionParser.getResult(INGNORE_INTERACTIONS);
        if (ignoreInteractionsPath.isPresent()) {
            BooleanAssignmentGroups ignoreInteractions = IO.load(
                            ignoreInteractionsPath.get(), new BooleanAssignmentGroupsFormats())
                    .orElseLog(Verbosity.WARNING);
            if (ignoreInteractions != null) {
                coverageComputation.set(
                        ATWiseCoverageComputation.EXCLUDE_INTERACTIONS, ignoreInteractions.getMergedGroups());
            }
        }

        return coverageComputation;
    }

    private IComputation<CoverageStatistic> computeAbsoluteCoverage(IComputation<BooleanAssignmentList> sample) {
        return sample.map(AbsoluteTWiseCoverageComputation::new);
    }

    private IComputation<CoverageStatistic> computeRelativeCoverage(
            IComputation<BooleanAssignmentList> sample, Path referencePath) {
        BooleanAssignmentList referenceSample = IO.load(referencePath, BooleanAssignmentGroupsFormats.getInstance())
                .map(BooleanAssignmentGroups::getFirstGroup)
                .orElseThrow();

        return sample.map(RelativeTWiseCoverageComputation::new)
                .set(RelativeTWiseCoverageComputation.REFERENCE_SAMPLE, referenceSample);
    }

    private IComputation<CoverageStatistic> computeFMCoverage(IComputation<BooleanAssignmentList> sample, Path fmPath) {
        BooleanAssignmentList formula = IO.load(fmPath, BooleanAssignmentGroupsFormats.getInstance())
                .map(cnf -> (IComputation<BooleanAssignmentList>)
                        Computations.of(cnf.getFirstGroup().toClauseList()))
                .orElseGet(() -> IO.load(fmPath, FormulaFormats.getInstance())
                        .toComputation()
                        .map(ComputeNNFFormula::new)
                        .map(ComputeCNFFormula::new)
                        .map(ComputeBooleanClauseList::new))
                .computeResult()
                .orElseThrow();
        return sample.map(ConstraintedCoverageComputation::new)
                .set(ConstraintedCoverageComputation.BOOLEAN_CLAUSE_LIST, formula);
    }

    @Override
    protected IFormat<?> getOuputFormat() {
        return new StringTextFormat();
    }

    @Override
    protected String printResult(CoverageStatistic result) {
        return coverageOnly
                ? countOnly
                        ? result.coverage() + "\n" + result.covered() + "\n" + result.uncovered() + "\n"
                                + result.invalid() + "\n" + result.ignored()
                        : String.valueOf(result.coverage())
                : countOnly
                        ? result.covered() + "\n" + result.uncovered() + "\n" + result.invalid() + "\n"
                                + result.ignored()
                        : result.print();
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("t-wise-coverage");
    }
}
