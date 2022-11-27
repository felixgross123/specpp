package org.processmining.specpp.evaluation.heuristics;

import jnr.ffi.annotations.In;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.config.parameters.TreeHeuristcAlpha;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.log.Variant;
import org.processmining.specpp.datastructures.log.impls.Factory;
import org.processmining.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.heuristic.SubtreeMonotonicity;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import org.processmining.specpp.traits.ZeroOneBounded;

import java.util.*;

public class MedModFirstOccIndexDeltaTreeHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    private final Map<Activity, Integer> activityToModFirstOccurrenceIndex;
    private final BidiMap<Activity, Transition> actTransMapping;
    private final double alpha;
    private final double maxDelta;
    private final int maxSize;

    public MedModFirstOccIndexDeltaTreeHeuristic(Map<Activity, Integer> activityToModFirstOccurrenceIndex, BidiMap<Activity, Transition>actTransMapping, double alpha, double maxDelta, int maxSize) {
        this.activityToModFirstOccurrenceIndex = activityToModFirstOccurrenceIndex;
        this.actTransMapping = actTransMapping;
        this.alpha = alpha;
        this.maxDelta = maxDelta;
        this.maxSize = maxSize;
    }

    public static class Builder extends ComponentSystemAwareBuilder<MedModFirstOccIndexDeltaTreeHeuristic> {



        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

        private final DelegatingDataSource<TreeHeuristcAlpha> alpha = new DelegatingDataSource<>();

        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping).require(ParameterRequirements.TREEHEURISTIC_ALPHA, alpha);
        }

        @Override
        protected MedModFirstOccIndexDeltaTreeHeuristic buildIfFullySatisfied() {
            Log log = rawLog.getData();
            Map<Activity, Integer> activityToModFirstOccurrenceIndex = new HashMap<>();

            Map<Activity, int[]> activityToNumberOfFirstOccurrenceIndices = new HashMap<>();

            int maxLength = 0;
            for (IndexedVariant indexedVariant : log) {
                if(indexedVariant.getVariant().getLength() > maxLength) {
                    maxLength = indexedVariant.getVariant().getLength();
                }
            }
            for (Activity a : actTransMapping.getData().keySet()) {
                activityToNumberOfFirstOccurrenceIndices.put(a, new int[maxLength]);
            }

            for (IndexedVariant indexedVariant : log) {
                int firstOcc = 0;
                Set<Activity> seen = new HashSet<>();
                Variant variant = indexedVariant.getVariant();

                for (Activity a : variant) {
                    if (!seen.contains(a)) {
                        int[] numberOfIndices = activityToNumberOfFirstOccurrenceIndices.get(a);
                        numberOfIndices[firstOcc]++;
                    }
                    firstOcc++;
                    seen.add(a);
                }
            }

            for (Activity a : actTransMapping.getData().keySet()) {
                int maxIndex = -1;
                int max = Integer.MIN_VALUE;
                int[] numberOfIndices = activityToNumberOfFirstOccurrenceIndices.get(a);

                for(int i = 0; i<numberOfIndices.length; i++) {
                    if(numberOfIndices[i] > max) {
                        max = numberOfIndices[i];
                        maxIndex = i;
                    }
                }
                activityToModFirstOccurrenceIndex.put(a, maxIndex);
            }

            double maxDelta = activityToModFirstOccurrenceIndex.get(Factory.ARTIFICIAL_END);
            int maxSize = 2*actTransMapping.getData().size();

            return new MedModFirstOccIndexDeltaTreeHeuristic(activityToModFirstOccurrenceIndex, actTransMapping.getData(), alpha.getData().getP(), maxDelta, maxSize);
        }
    }


    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        Place p = node.getPlace();

        if (p.isHalfEmpty()) return new TreeNodeScore(0);

        double[] avgIndicesPreset = new double[p.preset().size()];
        int i = 0;
        for (Transition t : p.preset()) {
            Activity a = actTransMapping.getKey(t);
            avgIndicesPreset[i] = activityToModFirstOccurrenceIndex.get(a);
            i++;
        }
        double medianIndexPreset = new Median().evaluate(avgIndicesPreset);

        double[] avgIndicesPostset = new double[p.postset().size()];

        i = 0;
        for (Transition t : p.postset()) {
            Activity a = actTransMapping.getKey(t);
            avgIndicesPostset[i] = activityToModFirstOccurrenceIndex.get(a);
            i++;
        }
        double medianIndexPostset= new Median().evaluate(avgIndicesPostset);

        double score = alpha * (Math.abs(medianIndexPostset - medianIndexPreset) / maxDelta) + (1-alpha) * ((double) node.getPlace().size() / maxSize);
        return new TreeNodeScore(score);
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.naturalOrder();
    }

}
