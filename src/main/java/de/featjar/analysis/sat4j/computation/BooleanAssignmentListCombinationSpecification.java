package de.featjar.analysis.sat4j.computation;

import de.featjar.base.data.IntegerList;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;

import java.util.Collections;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanAssignmentListCombinationSpecification implements ICombinationSpecification {

    public BooleanAssignmentList booleanAssignmentList;

    public BooleanAssignmentListCombinationSpecification(BooleanAssignmentList booleanAssignmentList) {
        this.booleanAssignmentList = new BooleanAssignmentList(booleanAssignmentList);
    }

    @Override
    public long loopCount() {
        return booleanAssignmentList.size();
    }

    @Override
    public void shuffleElements(Random random) {
        Collections.shuffle(booleanAssignmentList.getAll(), random);
    }

    @Override
    public void adapt(VariableMap variableMap) {
        booleanAssignmentList.adapt(variableMap);
    }

    @Override
    public void forEach(Consumer<int[]> consumer) {
        booleanAssignmentList.stream().map(IntegerList::get).forEach(consumer);
    }

    @Override
    public <V> void forEach(BiConsumer<V, int[]> consumer, Supplier<V> environmentCreator) {
        booleanAssignmentList.stream().map(IntegerList::get).forEach(assignment -> consumer.accept(environmentCreator.get(), assignment));
    }

    @Override
    public <V> void forEachParallel(BiConsumer<V, int[]> consumer, Supplier<V> environmentCreator) {
        booleanAssignmentList.stream().parallel().map(IntegerList::get).forEach(assignment -> consumer.accept(environmentCreator.get(), assignment));
    }

    @Override
    public VariableMap variableMap() {
        return booleanAssignmentList.getVariableMap();
    }

    @Override
    public int maxT() {
        return booleanAssignmentList.stream().map(IntegerList::size).max(Integer::compareTo).orElse(0);
    }

    @Override
    public ICombinationSpecification reduceTTo(int newT) {
        return new BooleanAssignmentListCombinationSpecification(new BooleanAssignmentList(booleanAssignmentList.getVariableMap(),
                booleanAssignmentList.stream().filter(assignment -> assignment.size() <= newT)));
    }
}
