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

import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;

import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MultiCombinationSpecification implements ICombinationSpecification {

    private final List<ICombinationSpecification> combinationSets;

    public MultiCombinationSpecification(List<ICombinationSpecification> combinationSets) {
        this.combinationSets = combinationSets;
    }

    @Override
    public long loopCount() {
        long sum = 0;
        for (ICombinationSpecification combinationSet : combinationSets) {
            sum += combinationSet.loopCount();
        }
        return sum;
    }

    @Override
    public void shuffleElements(Random random) {
        for (ICombinationSpecification combinationSet : combinationSets) {
            combinationSet.shuffleElements(random);
        }
    }

    @Override
    public void adapt(VariableMap variableMap) {
        for (ICombinationSpecification combinationSet : combinationSets) {
            combinationSet.adapt(variableMap);

        }
    }

    @Override
    public void forEach(Consumer<int[]> consumer) {
        for (ICombinationSpecification combinationSet : combinationSets) {
            combinationSet.forEach(consumer);
        }
    }

    @Override
    public <V> void forEach(BiConsumer<V, int[]> consumer, Supplier<V> environmentCreator) {
        for (ICombinationSpecification combinationSet : combinationSets) {
            combinationSet.forEach(consumer, environmentCreator);
        }
    }

    @Override
    public <V> void forEachParallel(BiConsumer<V, int[]> consumer, Supplier<V> environmentCreator) {
        for (ICombinationSpecification combinationSet : combinationSets) {
            combinationSet.forEachParallel(consumer, environmentCreator);
        }
    }

    @Override
    public VariableMap variableMap() {
        return new VariableMap(combinationSets.stream()
                .map(ICombinationSpecification::variableMap)
                .collect(Collectors.toList()));
    }

    @Override
    public int maxT() {
        return combinationSets.stream()
                .mapToInt(ICombinationSpecification::maxT)
                .max()
                .orElse(0);
    }

    @Override
    public ICombinationSpecification reduceTTo(int newT) {
        return new MultiCombinationSpecification(
                combinationSets.stream().map(s -> s.reduceTTo(newT)).collect(Collectors.toList()));
    }
}
