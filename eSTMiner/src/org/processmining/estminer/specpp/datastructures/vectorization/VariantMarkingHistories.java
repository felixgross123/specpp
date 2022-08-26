package org.processmining.estminer.specpp.datastructures.vectorization;

import org.processmining.estminer.specpp.base.Evaluation;
import org.processmining.estminer.specpp.datastructures.encoding.BitMask;
import org.processmining.estminer.specpp.datastructures.encoding.IndexSubset;
import org.processmining.estminer.specpp.datastructures.log.NotCoveringRequiredVariantsException;
import org.processmining.estminer.specpp.datastructures.log.NotCoveringSameVariantsException;
import org.processmining.estminer.specpp.datastructures.log.OnlyCoversIndexSubset;
import org.processmining.estminer.specpp.datastructures.util.IndexedItem;
import org.processmining.estminer.specpp.datastructures.vectorization.spliteration.IndexedBitMaskSplitty;
import org.processmining.estminer.specpp.datastructures.vectorization.spliteration.IndexedSpliterable;
import org.processmining.estminer.specpp.traits.Copyable;
import org.processmining.estminer.specpp.traits.IndexAccessible;
import org.processmining.estminer.specpp.traits.PartiallyOrderedOnSubdomain;

import java.nio.IntBuffer;
import java.util.Spliterator;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

public class VariantMarkingHistories implements IndexedSpliterable<IntBuffer>, Mathable<VariantMarkingHistories>, Copyable<VariantMarkingHistories>, PartiallyOrderedOnSubdomain<BitMask, VariantMarkingHistories>, IndexAccessible<IntBuffer>, OnlyCoversIndexSubset, Evaluation {

    private final IndexSubset indexSubset;
    private final IntVectorStorage markingHistories;

    public VariantMarkingHistories(IndexSubset indexSubset, IntVectorStorage markingHistories) {
        this.indexSubset = indexSubset;
        this.markingHistories = markingHistories;
    }

    @Override
    public void add(VariantMarkingHistories other) {
        if (!indexSubset.setEquality(other.indexSubset)) throw new NotCoveringSameVariantsException();
        markingHistories.add(other.markingHistories);
    }

    @Override
    public void subtract(VariantMarkingHistories other) {
        if (!indexSubset.setEquality(other.indexSubset)) throw new NotCoveringSameVariantsException();
        markingHistories.subtract(other.markingHistories);
    }

    @Override
    public void negate() {
        markingHistories.negate();
    }

    @Override
    public VariantMarkingHistories copy() {
        return new VariantMarkingHistories(indexSubset.copy(), markingHistories.copy());
    }

    @Override
    public String toString() {
        return markingHistories.toString();
    }

    @Override
    public boolean gtOn(BitMask mask, VariantMarkingHistories other) {
        return indexSubset.covers(mask) && other.indexSubset.covers(mask) && IVSComputations.gtOn(indexSubset.mapIndices(mask.stream()), markingHistories, other.indexSubset.mapIndices(mask.stream()), other.markingHistories);
    }

    @Override
    public boolean ltOn(BitMask mask, VariantMarkingHistories other) {
        return indexSubset.covers(mask) && other.indexSubset.covers(mask) && IVSComputations.ltOn(indexSubset.mapIndices(mask.stream()), markingHistories, other.indexSubset.mapIndices(mask.stream()), other.markingHistories);
    }

    private IntStream localizeIndicesOf(VariantMarkingHistories other) {
        return indexSubset.mapIndices(other.indexSubset.streamIndices());
    }

    @Override
    public boolean gt(VariantMarkingHistories other) {
        return indexSubset.isSupersetOf(other.indexSubset) && IVSComputations.gtOn(localizeIndicesOf(other), markingHistories, VMHComputations.localIndicesStream(other), other.markingHistories);
    }

    @Override
    public boolean lt(VariantMarkingHistories other) {
        return indexSubset.isSupersetOf(other.indexSubset) && IVSComputations.ltOn(localizeIndicesOf(other), markingHistories, VMHComputations.localIndicesStream(other), other.markingHistories);
    }

    public IntVectorStorage getData() {
        return markingHistories;
    }

    @Override
    public IndexSubset getIndexSubset() {
        return indexSubset;
    }

    public IntStream variantIndices() {
        return indexSubset.streamIndices();
    }

    public BitMask computePerfectlyFitting() {
        return BitMask.of(markingHistories.getIndexedVectors()
                                          .filter(ii -> VMHComputations.markingBasedBooleanReplay(ii.getItem()))
                                          .mapToInt(ii -> indexSubset.unmapIndex(ii.getIndex())));
    }

    @Override
    public Spliterator<IndexedItem<IntBuffer>> indexedSpliterator() {
        return new IndexedBitMaskSplitty(markingHistories, indexSubset.getIndices(), 0, indexSubset.getIndexCount(), IntUnaryOperator.identity());
    }

    @Override
    public Spliterator<IndexedItem<IntBuffer>> indexedSpliterator(BitMask bitMask) {
        return new IndexedBitMaskSplitty(markingHistories, bitMask, 0, bitMask.cardinality(), IntUnaryOperator.identity());
    }

    @Override
    public IntBuffer getAt(int index) {
        if (indexSubset.contains(index)) {
            return markingHistories.getVector(indexSubset.mapIndex(index));
        }
        throw new NotCoveringRequiredVariantsException();
    }

}
