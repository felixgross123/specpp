package org.processmining.estminer.specpp.datastructures.log.impls;

import org.processmining.estminer.specpp.datastructures.encoding.BitMask;
import org.processmining.estminer.specpp.datastructures.encoding.IntEncodings;
import org.processmining.estminer.specpp.datastructures.log.Activity;
import org.processmining.estminer.specpp.datastructures.util.IndexedItem;
import org.processmining.estminer.specpp.datastructures.util.Tuple2;
import org.processmining.estminer.specpp.datastructures.vectorization.IntVector;
import org.processmining.estminer.specpp.datastructures.vectorization.spliteration.IndexedSpliterable;
import org.processmining.estminer.specpp.datastructures.vectorization.spliteration.Spliteration;
import org.processmining.estminer.specpp.util.StreamUtils;

import java.nio.IntBuffer;
import java.util.Spliterator;
import java.util.stream.IntStream;

public class MultiEncodedLog implements IndexedSpliterable<Tuple2<IntBuffer, IntBuffer>> {

    private final EncodedLog presetEncodedLog;
    private final EncodedLog postsetEncodedLog;
    private final IntEncodings<Activity> activityEncodings;

    protected MultiEncodedLog(EncodedLog presetEncodedLog, EncodedLog postsetEncodedLog, IntEncodings<Activity> activityEncodings) {
        assert presetEncodedLog.variantCount() == postsetEncodedLog.variantCount();
        assert StreamUtils.streamsEqual(presetEncodedLog.streamIndices(), postsetEncodedLog.streamIndices());
        this.presetEncodedLog = presetEncodedLog;
        this.postsetEncodedLog = postsetEncodedLog;
        this.activityEncodings = activityEncodings;
    }

    public EncodedLog getPresetEncodedLog() {
        return presetEncodedLog;
    }

    public EncodedLog pre() {
        return getPresetEncodedLog();
    }

    public EncodedLog getPostsetEncodedLog() {
        return postsetEncodedLog;
    }

    public EncodedLog post() {
        return getPostsetEncodedLog();
    }

    public IntEncodings<Activity> getEncodings() {
        return activityEncodings;
    }

    public int getVariantCount() {
        return getPresetEncodedLog().variantCount();
    }

    public BitMask variantIndices() {
        return getPresetEncodedLog().variantIndices();
    }

    public IntStream streamIndices() {
        return getPresetEncodedLog().streamIndices();
    }

    public IntVector variantFrequencies() {
        return getPresetEncodedLog().getVariantFrequencies();
    }

    @Override
    public String toString() {
        return "MultiEncodedLog{" + "presetEncodedLog=" + presetEncodedLog + ", postsetEncodedLog=" + postsetEncodedLog + "}";
    }

    @Override
    public Spliterator<IndexedItem<Tuple2<IntBuffer, IntBuffer>>> indexedSpliterator() {
        return Spliteration.joinIndexedSpliterators(presetEncodedLog.indexedSpliterator(), postsetEncodedLog.indexedSpliterator());
    }

    @Override
    public Spliterator<IndexedItem<Tuple2<IntBuffer, IntBuffer>>> indexedSpliterator(BitMask bitMask) {
        return Spliteration.joinIndexedSpliterators(presetEncodedLog.indexedSpliterator(bitMask), postsetEncodedLog.indexedSpliterator(bitMask));
    }

}
