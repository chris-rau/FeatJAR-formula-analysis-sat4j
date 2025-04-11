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
package de.featjar.analysis.sat4j.twise;

/**
 * Holds statistics regarding coverage of a configuration sample.
 *
 * @author Sebastian Krieter
 */
public class CoverageStatistic {
    private long numberOfInvalidConditions;
    private long numberOfCoveredConditions;
    private long numberOfUncoveredConditions;
    private long numberOfIgnoredConditions;

    public void setNumberOfInvalidConditions(long numberOfInvalidConditions) {
        this.numberOfInvalidConditions = numberOfInvalidConditions;
    }

    public void setNumberOfCoveredConditions(long numberOfCoveredConditions) {
        this.numberOfCoveredConditions = numberOfCoveredConditions;
    }

    public void setNumberOfUncoveredConditions(long numberOfUncoveredConditions) {
        this.numberOfUncoveredConditions = numberOfUncoveredConditions;
    }

    public void setNumberOfIgnoredConditions(long numberOfIgnoredConditions) {
        this.numberOfIgnoredConditions = numberOfIgnoredConditions;
    }

    public void incNumberOfInvalidConditions() {
        numberOfInvalidConditions++;
    }

    public void incNumberOfCoveredConditions() {
        numberOfCoveredConditions++;
    }

    public void incNumberOfUncoveredConditions() {
        numberOfUncoveredConditions++;
    }

    public void incNumberOfIgnoredConditions() {
        numberOfIgnoredConditions++;
    }

    public long total() {
        return numberOfInvalidConditions
                + numberOfCoveredConditions
                + numberOfUncoveredConditions
                + numberOfIgnoredConditions;
    }

    public long valid() {
        return numberOfCoveredConditions + numberOfUncoveredConditions;
    }

    public long invalid() {
        return numberOfInvalidConditions;
    }

    public long covered() {
        return numberOfCoveredConditions;
    }

    public long uncovered() {
        return numberOfUncoveredConditions;
    }

    public long ignored() {
        return numberOfIgnoredConditions;
    }

    public double coverage() {
        return (numberOfCoveredConditions + numberOfUncoveredConditions != 0)
                ? (double) numberOfCoveredConditions / (numberOfCoveredConditions + numberOfUncoveredConditions)
                : 1.0;
    }

    CoverageStatistic merge(CoverageStatistic other) {
        numberOfInvalidConditions += other.numberOfInvalidConditions;
        numberOfCoveredConditions += other.numberOfCoveredConditions;
        numberOfUncoveredConditions += other.numberOfUncoveredConditions;
        numberOfIgnoredConditions += other.numberOfIgnoredConditions;
        return this;
    }

    public String print() {
        long total = total();
        int digits = total == 0 ? 1 : (int) (Math.log10(total()) + 1);
        String format = "%" + digits + "d";

        StringBuilder sb = new StringBuilder();
        sb.append("Interaction Coverage Statistics");
        sb.append("\nCoverage:     ");
        sb.append(coverage());
        sb.append("\nInteractions: ");
        sb.append(total);
        sb.append("\n ");
        sb.append(Character.toChars(0x251c));
        sb.append(Character.toChars(0x2500));
        sb.append("Covered:   ");
        sb.append(String.format(format, numberOfCoveredConditions));
        sb.append("\n ");
        sb.append(Character.toChars(0x251c));
        sb.append(Character.toChars(0x2500));
        sb.append("Uncovered: ");
        sb.append(String.format(format, numberOfUncoveredConditions));
        sb.append("\n ");
        sb.append(Character.toChars(0x251c));
        sb.append(Character.toChars(0x2500));
        sb.append("Ignored:   ");
        sb.append(String.format(format, numberOfInvalidConditions));
        sb.append("\n ");
        sb.append(Character.toChars(0x2514));
        sb.append(Character.toChars(0x2500));
        sb.append("Invalid:   ");
        sb.append(String.format(format, numberOfIgnoredConditions));
        return sb.toString();
    }
}
