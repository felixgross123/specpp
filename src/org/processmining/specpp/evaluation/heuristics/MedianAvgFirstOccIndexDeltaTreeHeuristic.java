package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
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
import org.apache.commons.math3.*;

import javax.print.attribute.standard.Media;
import java.util.*;

public class MedianAvgFirstOccIndexDeltaTreeHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    private Map<Activity, Double> activityToAvgFirstOccurrenceIndex;
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping;

    int maxDelta;

    public MedianAvgFirstOccIndexDeltaTreeHeuristic(Map<Activity, Double> activityToAvgFirstOccurrenceIndex, DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping) {
        this.activityToAvgFirstOccurrenceIndex = activityToAvgFirstOccurrenceIndex;
        this.actTransMapping = actTransMapping;


    }

    public static class Builder extends ComponentSystemAwareBuilder<MedianAvgFirstOccIndexDeltaTreeHeuristic> {



        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();


        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping);
        }

        @Override
        protected MedianAvgFirstOccIndexDeltaTreeHeuristic buildIfFullySatisfied() {
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

            return new MedianAvgFirstOccIndexDeltaTreeHeuristic(activityToAvgFirstOccurrenceIndex, actTransMapping);
        }
    }


    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        Place p = node.getPlace();

        if (p.isHalfEmpty()) return new TreeNodeScore(0);

        double[] avgIndicesPreset = new double[p.preset().size()];
        int i = 0;
        for (Transition t : p.preset()) {
            Activity a = actTransMapping.getData().getKey(t);
            avgIndicesPreset[i] = activityToAvgFirstOccurrenceIndex.get(a);
            i++;
        }
        double medianIndexPreset = new Median().evaluate(avgIndicesPreset);

        double[] avgIndicesPostset = new double[p.postset().size()];

        i = 0;
        for (Transition t : p.postset()) {
            Activity a = actTransMapping.getData().getKey(t);
            avgIndicesPostset[i] = activityToAvgFirstOccurrenceIndex.get(a);
            i++;
        }
        double medianIndexPostset= new Median().evaluate(avgIndicesPostset);

        double score = Math.abs(medianIndexPostset - medianIndexPreset);
        return new TreeNodeScore(score);
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.naturalOrder();
    }

}
