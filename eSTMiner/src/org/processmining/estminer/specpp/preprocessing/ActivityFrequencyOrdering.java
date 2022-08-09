package org.processmining.estminer.specpp.preprocessing;

import org.apache.commons.collections4.BidiMap;
import org.processmining.estminer.specpp.datastructures.log.Activity;
import org.processmining.estminer.specpp.datastructures.log.Log;
import org.processmining.estminer.specpp.datastructures.log.Variant;
import org.processmining.estminer.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.estminer.specpp.datastructures.petri.Transition;
import org.processmining.estminer.specpp.datastructures.util.Counter;
import org.processmining.estminer.specpp.datastructures.util.Pair;

import java.util.Comparator;
import java.util.Map;

public class ActivityFrequencyOrdering extends TransitionEncodingsBuilder {
    public ActivityFrequencyOrdering(Log log, Map<String, Activity> activityMapping, BidiMap<Activity, Transition> transitionMapping) {
        super(log, activityMapping, transitionMapping);
    }

    @Override
    protected Pair<Comparator<Activity>> computeActivityOrderings(Log log, Map<String, Activity> mapping) {

        Counter<Activity> counter = new Counter<>();

        for (IndexedVariant indexedVariant : log) {
            Variant v = indexedVariant.getVariant();
            for (Activity a : v) {
                counter.inc(a, log.getVariantFrequency(indexedVariant.getIndex()));
            }
        }

        Comparator<Activity> comparator = Comparator.<Activity>comparingInt(counter::get).reversed();

        return new Pair<>(comparator, comparator);
    }
}
