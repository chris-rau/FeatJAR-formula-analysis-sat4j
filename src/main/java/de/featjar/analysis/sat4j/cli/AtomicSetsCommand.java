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

import de.featjar.analysis.sat4j.computation.ComputeAtomicSetsSAT4J;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.IComputation;
import de.featjar.base.io.format.IFormat;
import de.featjar.formula.assignment.BooleanAssignmentGroups;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.io.csv.BooleanSolutionListCSVFormat;
import java.util.Optional;

/**
 * Computes atomic sets for a given formula using SAT4J.
 *
 * @author Elias Kuiter
 * @author Sebastian Krieter
 * @author Andreas Gerasimow
 */
public class AtomicSetsCommand extends ASAT4JAnalysisCommand<BooleanAssignmentList, BooleanAssignmentList> {

    public static final Option<Boolean> OMIT_SINGLE_SETS = Option.newFlag("omit-singles")
            .setDefaultValue(Boolean.FALSE)
            .setDescription("Omits sets with only one element");
    public static final Option<Boolean> OMIT_CORE =
            Option.newFlag("omit-core").setDefaultValue(Boolean.FALSE).setDescription("Omits set containing core");

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Computes atomic sets for a given formula using SAT4J.");
    }

    @Override
    public IComputation<BooleanAssignmentList> newAnalysis(
            OptionList optionParser, IComputation<BooleanAssignmentList> formula) {
        return formula.map(ComputeAtomicSetsSAT4J::new)
                .set(ComputeAtomicSetsSAT4J.OMIT_CORE, optionParser.get(OMIT_CORE))
                .set(ComputeAtomicSetsSAT4J.OMIT_SINGLE_SETS, optionParser.get(OMIT_SINGLE_SETS));
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
    public String printResult(BooleanAssignmentList list) {
        return list.print();
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("atomic-sets-sat4j");
    }
}
