package org.processmining.estminer.specpp.datastructures.petri;

import org.processmining.estminer.specpp.base.Candidate;
import org.processmining.estminer.specpp.datastructures.encoding.BitEncodedSet;
import org.processmining.estminer.specpp.datastructures.encoding.MutatingSetOperations;
import org.processmining.estminer.specpp.datastructures.encoding.NonMutatingSetOperations;
import org.processmining.estminer.specpp.datastructures.encoding.SetQueries;
import org.processmining.estminer.specpp.datastructures.tree.base.NodeProperties;
import org.processmining.estminer.specpp.datastructures.util.Pair;
import org.processmining.estminer.specpp.traits.Copyable;
import org.processmining.estminer.specpp.traits.ProperlyHashable;
import org.processmining.estminer.specpp.traits.ProperlyPrintable;

/**
 * A class representing a Petri net place as a combination of its preset and postset.
 * The sets are represented as ordered subsets of all possible transitions that may occur in the preset and postset respectively.
 * A place lifts {@code NonMutatingSetOperations} and {@code SetQueries} to these pairs by component wise application.
 * Places are marked as proposal candidates {@code Candidate} and node properties {@code NodeProperties} of trees.
 *
 * @see NonMutatingSetOperations
 * @see Candidate
 * @see NodeProperties
 */
public class Place implements Candidate, NodeProperties, ProperlyHashable, ProperlyPrintable, Copyable<Place>, NonMutatingSetOperations<Place>, SetQueries<Place> {

    private final BitEncodedSet<Transition> ingoingTransitions, outgoingTransitions;

    public Place(Pair<BitEncodedSet<Transition>> encodedSetPair) {
        this(encodedSetPair.first(), encodedSetPair.second());
    }

    public Place(BitEncodedSet<Transition> ingoingTransitions, BitEncodedSet<Transition> outgoingTransitions) {
        this.ingoingTransitions = ingoingTransitions;
        this.outgoingTransitions = outgoingTransitions;
    }

    public static Place of(BitEncodedSet<Transition> ingoingTransitions, BitEncodedSet<Transition> outgoingTransitions) {
        return new Place(ingoingTransitions, outgoingTransitions);
    }

    public int size() {
        return preset().cardinality() + postset().cardinality();
    }

    public boolean isEmpty() {
        return ingoingTransitions.isEmpty() && outgoingTransitions.isEmpty();
    }

    public BitEncodedSet<Transition> preset() {
        return ingoingTransitions;
    }

    public BitEncodedSet<Transition> postset() {
        return outgoingTransitions;
    }


    public Place nonSelfLoops() {
        return new Place(MutatingSetOperations.dualSetminus(preset(), postset()));
    }

    public Place selfLoops() {
        return new Place(MutatingSetOperations.dualIntersection(preset(), postset()));
    }

    @Override
    public Place union(Place other) {
        Place result = copy();
        result.preset().union(other.preset());
        result.postset().union(other.postset());
        return result;
    }

    @Override
    public Place setminus(Place other) {
        Place result = copy();
        result.preset().setminus(other.preset());
        result.postset().setminus(other.postset());
        return result;
    }

    @Override
    public Place intersection(Place other) {
        Place result = copy();
        result.preset().intersection(other.preset());
        result.postset().intersection(other.postset());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Place place = (Place) o;

        if (!ingoingTransitions.setEquality(place.ingoingTransitions)) return false;
        return outgoingTransitions.setEquality(place.outgoingTransitions);
    }

    @Override
    public int hashCode() {
        int result = ingoingTransitions.hashCode();
        result = 31 * result + outgoingTransitions.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ingoingTransitions.toString() + "|" + outgoingTransitions.toString();
    }

    @Override
    public Place copy() {
        return new Place(ingoingTransitions.copy(), outgoingTransitions.copy());
    }

    @Override
    public boolean intersects(Place other) {
        return preset().intersects(other.preset()) || postset().intersects(other.postset());
    }

    @Override
    public boolean setEquality(Place other) {
        return preset().setEquality(other.preset()) && postset().setEquality(other.postset());
    }

    @Override
    public boolean isSubsetOf(Place other) {
        return preset().isSubsetOf(other.preset()) && postset().isSubsetOf(other.postset());
    }

    @Override
    public boolean isSupersetOf(Place other) {
        return preset().isSupersetOf(other.preset()) && postset().isSupersetOf(other.postset());
    }
}
