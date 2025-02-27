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
import de.featjar.base.data.ICombination;
import de.featjar.base.data.SingleLexicographicIterator;
import de.featjar.formula.assignment.BooleanAssignment;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TWiseCombinations {

    public static class TWiseCombinationsList extends ArrayList<TWiseCombinations> {
        private static final long serialVersionUID = 6684204745606984679L;

        public TWiseCombinationsList(BooleanAssignment variables, int t) {
            super(1);
            add(new TWiseCombinations(variables, t));
        }

        public TWiseCombinationsList(TWiseCombinations... specs) {
            super(List.of(specs));
        }
    }

    private final int t;

    private final int[] literals;

    public TWiseCombinations(BooleanAssignment variables, int t) {
        this(variables.get(), t);
    }

    public TWiseCombinations(int[] literals, int t) {
        if (t < 1) {
            throw new IllegalArgumentException("Value for t must be greater than 0. Value was " + t);
        }

        this.literals = literals;
        if (literals.length < t) {
            throw new IllegalArgumentException(
                    String.format("Value for t must be greater than number of variables", t, literals.length));
        }
        this.t = t;
    }

    public TWiseCombinations forOtherT(int otherT) {
        return new TWiseCombinations(literals, otherT);
    }

    public Stream<int[]> stream() {
        return SingleLexicographicIterator.stream(literals, t).map(combo -> combo.select());
    }

    public <V> Stream<ICombination<V, int[]>> parallelStream(Supplier<V> environment) {
        return SingleLexicographicIterator.parallelStream(literals, t, environment);
    }

    public int getT() {
        return t;
    }

    public long getTotalSteps() {
        return BinomialCalculator.computeBinomial(literals.length, t);
    }

    public void shuffle(Random random) {
        final long seed = random.nextLong();
        Random curRandom = new Random(seed);
        for (int i = literals.length - 1; i > 0; --i) {
            int swapIndex = curRandom.nextInt(literals.length);
            int temp = literals[i];
            literals[i] = literals[swapIndex];
            literals[swapIndex] = temp;
        }
    }
}
