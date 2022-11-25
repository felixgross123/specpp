package org.processmining.specpp.evaluation.heuristics;

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

public class HarMeanMedFirstOccIndexDeltaTreeHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    private final Map<Activity, Double> activityToMedFirstOccurrenceIndex;
    private final BidiMap<Activity, Transition> actTransMapping;

    private final double alpha;
    private final double maxDelta;
    private final double maxSize;

    public HarMeanMedFirstOccIndexDeltaTreeHeuristic(Map<Activity, Double> activityToMedFirstOccurrenceIndex, BidiMap<Activity, Transition> actTransMapping, double alpha, double maxDelta, int maxSize) {
        this.activityToMedFirstOccurrenceIndex = activityToMedFirstOccurrenceIndex;
        this.actTransMapping = actTransMapping;
        this.alpha = alpha;
        this.maxDelta = maxDelta;
        this.maxSize = maxSize;
    }

    public static class Builder extends ComponentSystemAwareBuilder<HarMeanMedFirstOccIndexDeltaTreeHeuristic> {



        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

        private final DelegatingDataSource<TreeHeuristcAlpha> alpha = new DelegatingDataSource<>();

        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping).require(ParameterRequirements.TREEHEURISTIC_ALPHA, alpha);
        }

        @Override
        protected HarMeanMedFirstOccIndexDeltaTreeHeuristic buildIfFullySatisfied() {
            Log log = rawLog.getData();
            Map<Activity, Double> activityToMedFirstOccurrenceIndex = new HashMap<>();

            Map<Activity, List<Integer>> activityToFirstOccurrenceIndices = new HashMap<>();

            for (Activity a : actTransMapping.getData().keySet()) {
                activityToFirstOccurrenceIndices.put(a, new LinkedList<Integer>());
            }


            for (IndexedVariant indexedVariant : log) {
                int firstOcc = 0;
                Set<Activity> seen = new HashSet<>();
                Variant variant = indexedVariant.getVariant();

                for (Activity a : variant) {
                    if (!seen.contains(a)) {
                        List<Integer> indices = activityToFirstOccurrenceIndices.get(a);
                        indices.add(firstOcc);
                    }
                    firstOcc++;
                    seen.add(a);
                }
            }

            for (Map.Entry<Activity, List<Integer>> activityToFirstOccurrenceIndicesEntry : activityToFirstOccurrenceIndices.entrySet()) {
                double[] indicesArray = new double[activityToFirstOccurrenceIndicesEntry.getValue().size()];

                int i = 0;
                for (int index : activityToFirstOccurrenceIndicesEntry.getValue()) {
                    indicesArray[i] = index;
                    i++;
                }
                double median = new Median().evaluate(indicesArray);
                activityToMedFirstOccurrenceIndex.put(activityToFirstOccurrenceIndicesEntry.getKey(), median);
            }

            double maxDelta = activityToMedFirstOccurrenceIndex.get(Factory.ARTIFICIAL_END);
            int maxSize = 2*actTransMapping.getData().size();

            return new HarMeanMedFirstOccIndexDeltaTreeHeuristic(activityToMedFirstOccurrenceIndex, actTransMapping.getData(), alpha.getData().getP(), maxDelta, maxSize);
        }
    }


    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        Place p = node.getPlace();

        if (p.isHalfEmpty()) return new TreeNodeScore(0);

        double harMeanIndexPreset = 0.0;
        for (Transition t : p.preset()) {
            Activity a = actTransMapping.getKey(t);
            harMeanIndexPreset += 1.0/activityToMedFirstOccurrenceIndex.get(a);
        }
        harMeanIndexPreset = p.preset().size() / harMeanIndexPreset;

        double harMeanIndexPostset = 0.0;
        for (Transition t : p.postset()) {
            Activity a = actTransMapping.getKey(t);
            harMeanIndexPostset += 1.0/activityToMedFirstOccurrenceIndex.get(a);
        }
        harMeanIndexPostset = p.postset().size() / harMeanIndexPostset;

        double score = alpha * (Math.abs(harMeanIndexPostset - harMeanIndexPreset) / maxDelta) + (1-alpha) * (node.getPlace().size() / maxSize);
        return new TreeNodeScore(score);
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.naturalOrder();
    }

}
