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

import de.featjar.analysis.sat4j.computation.ComputeCoreDeadMIG;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.IComputation;
import de.featjar.base.io.format.IFormat;
import de.featjar.formula.assignment.BooleanAssignmentGroups;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.io.dimacs.BooleanAssignmentGroupsDimacsFormat;
import java.util.Optional;

/**
 * Computes core and dead variables for a given formula using SAT4J.
 *
 * @author Sebastian Krieter
 * @author Andreas Gerasimow
 */
public class CoreCommand extends ASAT4JAnalysisCommand<BooleanAssignmentGroups> {

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Computes core and dead variables for a given formula using SAT4J.");
    }

    @Override
    public IComputation<BooleanAssignmentGroups> newAnalysis(
            OptionList optionParser, IComputation<BooleanAssignmentList> formula) {
        return formula.map(ComputeCoreDeadMIG::new)
                .mapResult(CoreCommand.class, "group", a -> new BooleanAssignmentGroups(variableMap, a));
    }

    @Override
    protected IFormat<BooleanAssignmentGroups> getOuputFormat(OptionList optionParser) {
        return new BooleanAssignmentGroupsDimacsFormat();
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("core-sat4j");
    }
}
