package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.config.parameters.PrecisionThreshold;
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

public class AvgAvgFirstOccIndexDeltaTreeHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    private Map<Activity, Double> activityToAvgFirstOccurrenceIndex;
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping;

    private final double alpha;

    public AvgAvgFirstOccIndexDeltaTreeHeuristic(Map<Activity, Double> activityToAvgFirstOccurrenceIndex, DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping, double alpha) {
        this.activityToAvgFirstOccurrenceIndex = activityToAvgFirstOccurrenceIndex;
        this.actTransMapping = actTransMapping;
        this.alpha = alpha;
    }

    public static class Builder extends ComponentSystemAwareBuilder<AvgAvgFirstOccIndexDeltaTreeHeuristic> {



        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

        private final DelegatingDataSource<TreeHeuristcAlpha> alpha = new DelegatingDataSource<>();


        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                    .require(ParameterRequirements.TREEHEURISTIC_ALPHA, alpha);
        }

        @Override
        protected AvgAvgFirstOccIndexDeltaTreeHeuristic buildIfFullySatisfied() {
            Log log = rawLog.getData();
            Map<Activity, Double> activityToAvgFirstOccurrenceIndex = new HashMap<>();
            Map<Activity, Integer> activityToFreqSum = new HashMap<>();

            for (IndexedVariant indexedVariant : log) {

                Set<Activity> seen = new HashSet<>();
                Variant variant = indexedVariant.getVariant();
                int variantFrequency = log.getVariantFrequency(indexedVariant.getIndex());

                int j = 0;
                for (Activity a : variant) {
                    if (!seen.contains(a)) {
                        if(!activityToAvgFirstOccurrenceIndex.containsKey(a)) {
                            activityToAvgFirstOccurrenceIndex.put(a, Double.valueOf(j));
                            activityToFreqSum.put(a,variantFrequency);
                        } else {
                            int freqSumA = activityToFreqSum.get(a);

                            double newAvg = ((double)freqSumA / (double)(freqSumA + variantFrequency)) * activityToAvgFirstOccurrenceIndex.get(a) + ((double)variantFrequency / (double)(freqSumA + variantFrequency)) * j;
                            activityToAvgFirstOccurrenceIndex.put(a, newAvg);
                            activityToFreqSum.put(a,freqSumA + variantFrequency);
                        }
                    }
                    j++;
                    seen.add(a);
                }
            }

            return new AvgAvgFirstOccIndexDeltaTreeHeuristic(activityToAvgFirstOccurrenceIndex, actTransMapping, alpha.getData().getP());
        }
    }


    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        Place p = node.getPlace();

        if (p.isHalfEmpty()) return new TreeNodeScore(0);

        double avgIndexPreset = 0.0;
        for (Transition t : p.preset()) {
            Activity a = actTransMapping.getData().getKey(t);
            avgIndexPreset += activityToAvgFirstOccurrenceIndex.get(a);
        }
        avgIndexPreset /= p.preset().size();

        double avgIndexPostset = 0.0;
        for (Transition t : p.postset()) {
            Activity a = actTransMapping.getData().getKey(t);
            avgIndexPostset += activityToAvgFirstOccurrenceIndex.get(a);
        }
        avgIndexPostset /= p.postset().size();

        double maxDelta = activityToAvgFirstOccurrenceIndex.get(Factory.ARTIFICIAL_END);
        int maxSize = 2*actTransMapping.get().size();

        double score =  alpha * (Math.abs(avgIndexPostset - avgIndexPreset) / maxDelta) + (1-alpha) * ((double) node.getPlace().size() / maxSize);
        return new TreeNodeScore(score);
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.naturalOrder();
    }

}
