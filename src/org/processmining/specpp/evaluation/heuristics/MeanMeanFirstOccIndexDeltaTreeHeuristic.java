package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.config.parameters.TreeHeuristcAlpha;
import org.processmining.specpp.datastructures.encoding.IntEncodings;
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

import javax.xml.crypto.Data;
import java.util.*;

public class MeanMeanFirstOccIndexDeltaTreeHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    private final Map<Activity, Double> activityToMeanFirstOccurrenceIndex;
    private final BidiMap<Activity, Transition> actTransMapping;
    private final double alpha;

    private final double maxDelta;

    private final int maxSize;

    public MeanMeanFirstOccIndexDeltaTreeHeuristic(Map<Activity, Double> activityToMeanFirstOccurrenceIndex, BidiMap<Activity, Transition> actTransMapping, double alpha, double maxDelta, int maxSize) {
        this.activityToMeanFirstOccurrenceIndex = activityToMeanFirstOccurrenceIndex;
        this.actTransMapping = actTransMapping;
        this.alpha = alpha;
        this.maxDelta = maxDelta;
        this.maxSize = maxSize;
    }

    public static class Builder extends ComponentSystemAwareBuilder<MeanMeanFirstOccIndexDeltaTreeHeuristic> {



        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();
        private final DelegatingDataSource<IntEncodings<Activity>> encAct = new DelegatingDataSource<>();
        private final DelegatingDataSource<TreeHeuristcAlpha> alpha = new DelegatingDataSource<>();


        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping).require(DataRequirements.ENC_ACT, encAct)
                    .require(ParameterRequirements.TREEHEURISTIC_ALPHA, alpha);
        }

        @Override
        protected MeanMeanFirstOccIndexDeltaTreeHeuristic buildIfFullySatisfied() {
            Log log = rawLog.getData();
            Map<Activity, Double> activityToMeanFirstOccurrenceIndex = new HashMap<>();
            Map<Activity, Integer> activityToFreqSum = new HashMap<>();

            for (IndexedVariant indexedVariant : log) {

                Set<Activity> seen = new HashSet<>();
                Variant variant = indexedVariant.getVariant();
                int variantFrequency = log.getVariantFrequency(indexedVariant.getIndex());

                int j = 0;
                for (Activity a : variant) {
                    if (!seen.contains(a)) {
                        if(!activityToMeanFirstOccurrenceIndex.containsKey(a)) {
                            activityToMeanFirstOccurrenceIndex.put(a, (double) j);
                            activityToFreqSum.put(a,variantFrequency);
                        } else {
                            int freqSumA = activityToFreqSum.get(a);

                            double newAvg = ((double)freqSumA / (double)(freqSumA + variantFrequency)) * activityToMeanFirstOccurrenceIndex.get(a) + ((double)variantFrequency / (double)(freqSumA + variantFrequency)) * j;
                            activityToMeanFirstOccurrenceIndex.put(a, newAvg);
                            activityToFreqSum.put(a,freqSumA + variantFrequency);
                        }
                    }
                    j++;
                    seen.add(a);
                }
            }

            double maxDelta = activityToMeanFirstOccurrenceIndex.get(Factory.ARTIFICIAL_END);
            int maxSize = encAct.getData().getPresetEncoding().size() + encAct.getData().getPostsetEncoding().size();

            return new MeanMeanFirstOccIndexDeltaTreeHeuristic(activityToMeanFirstOccurrenceIndex, actTransMapping.getData(), alpha.getData().getAlpha(), maxDelta, maxSize);
        }
    }


    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        Place p = node.getPlace();

        if (p.isHalfEmpty()) return new TreeNodeScore(0);

        double avgIndexPreset = 0.0;
        for (Transition t : p.preset()) {
            Activity a = actTransMapping.getKey(t);
            avgIndexPreset += activityToMeanFirstOccurrenceIndex.get(a);
        }
        avgIndexPreset /= p.preset().size();

        double avgIndexPostset = 0.0;
        for (Transition t : p.postset()) {
            Activity a = actTransMapping.getKey(t);
            avgIndexPostset += activityToMeanFirstOccurrenceIndex.get(a);
        }
        avgIndexPostset /= p.postset().size();

        double score =  alpha * (Math.abs(avgIndexPostset - avgIndexPreset) / maxDelta) + (1-alpha) * ((double) node.getPlace().size() / maxSize);
        return new TreeNodeScore(score);
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.naturalOrder();
    }

}
