package org.processmining.estminer.specpp.datastructures.vectorization.spliterators;

import org.processmining.estminer.specpp.datastructures.util.IndexedItem;
import org.processmining.estminer.specpp.datastructures.vectorization.IntVectorStorage;

import java.nio.IntBuffer;
import java.util.Spliterator;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

public class IndexedSplitty extends AbstractSplitty<IndexedItem<IntBuffer>> {
    private final IntUnaryOperator outsideMapper;

    public IndexedSplitty(IntVectorStorage ivs, int startVectorIndex, int fenceVectorIndex, IntUnaryOperator outsideMapper) {
        super(ivs, startVectorIndex, fenceVectorIndex);
        this.outsideMapper = outsideMapper;
    }

    @Override
    protected IndexedItem<IntBuffer> make(int index) {
        return new IndexedItem<>(outsideMapper.applyAsInt(index), ivs.vectorBuffer(index));
    }

    @Override
    protected AbstractSplitty<IndexedItem<IntBuffer>> makePrefix(int low, int mid) {
        return new IndexedSplitty(ivs, low, mid, outsideMapper);
    }
}
