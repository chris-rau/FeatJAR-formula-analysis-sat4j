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
package de.featjar.analysis.sat4j.computation;

import de.featjar.base.data.BinomialCalculator;
import de.featjar.base.data.Ints;
import de.featjar.base.data.SingleLexicographicIterator;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class VariableCombinationSpecification extends ACombinationSpecification {

    public VariableCombinationSpecification(int t, VariableMap variableMap) {
        super(variableMap.getVariables().get(), t, variableMap);
    }

    public VariableCombinationSpecification(int t, BooleanAssignment variables, VariableMap variableMap) {
        super(IntStream.of(variables.get()).map(Math::abs).distinct().toArray(), t, variableMap);
    }

    public VariableCombinationSpecification(int t, int[] variables, VariableMap variableMap) {
        super(IntStream.of(variables).map(Math::abs).distinct().toArray(), t, variableMap);
    }

    public VariableCombinationSpecification(int t) {
        super(t);
    }

    public void forEach(Consumer<int[]> consumer) {
        final int[] gray = Ints.grayCode(t);
        SingleLexicographicIterator.stream(elements, t).forEach(combination -> {
            final int[] combinationLiterals = combination.select();
            for (int g : gray) {
                consumer.accept(combinationLiterals);
                combinationLiterals[g] = -combinationLiterals[g];
            }
        });
    }

    public <V> void forEach(BiConsumer<V, int[]> consumer, Supplier<V> environmentCreator) {
        final int[] gray = Ints.grayCode(t);
        SingleLexicographicIterator.stream(elements, t, environmentCreator).forEach(combination -> {
            final int[] combinationLiterals = combination.select();
            final V environment = combination.environment();
            for (int g : gray) {
                consumer.accept(environment, combinationLiterals);
                combinationLiterals[g] = -combinationLiterals[g];
            }
        });
    }

    public <V> void forEachParallel(BiConsumer<V, int[]> consumer, Supplier<V> environmentCreator) {
        final int[] gray = Ints.grayCode(t);
        SingleLexicographicIterator.parallelStream(elements, t, environmentCreator)
                .forEach(combination -> {
                    final int[] combinationLiterals = combination.select();
                    final V environment = combination.environment();
                    for (int g : gray) {
                        consumer.accept(environment, combinationLiterals);
                        combinationLiterals[g] = -combinationLiterals[g];
                    }
                });
    }

    @Override
    public long loopCount() {
        return 1 << t * BinomialCalculator.computeBinomial(elements.length, t);
    }

    @Override
    public ICombinationSpecification reduceTTo(int newT) {
        return new VariableCombinationSpecification(newT, elements, variableMap);
    }
}
