package org.processmining.specpp.evaluation.heuristics;

import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.datastructures.encoding.IntEncoding;
import org.processmining.specpp.datastructures.encoding.IntEncodings;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.log.Variant;
import org.processmining.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.heuristic.SubtreeMonotonicity;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import org.processmining.specpp.datastructures.vectorization.IntVector;
import org.processmining.specpp.traits.ZeroOneBounded;

import java.util.Comparator;
import java.util.PrimitiveIterator;

public class DirectlyFollowsTreeHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {
    private final int[][] dfCounts;


    public DirectlyFollowsTreeHeuristic(int[][] dfCounts) {
        this.dfCounts = dfCounts;
    }

    public static class Builder extends ComponentSystemAwareBuilder<DirectlyFollowsTreeHeuristic> {


        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<IntEncodings<Activity>> encAct = new DelegatingDataSource<>();

        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ENC_ACT, encAct);
        }

        @Override
        protected DirectlyFollowsTreeHeuristic buildIfFullySatisfied() {
            Log log = rawLog.getData();
            IntEncodings<Activity> activityIntEncodings = encAct.getData();
            IntEncoding<Activity> presetEncoding = activityIntEncodings.getPresetEncoding();
            IntEncoding<Activity> postsetEncoding = activityIntEncodings.getPostsetEncoding();
            int preSize = presetEncoding.size();
            int postSize = postsetEncoding.size();

            int[][] counts = new int[preSize][postSize];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = new int[postSize];
            }

            IntVector frequencies = log.getVariantFrequencies();
            for (IndexedVariant indexedVariant : log) {
                Variant variant = indexedVariant.getVariant();
                int f = frequencies.get(indexedVariant.getIndex());
                Activity last = null;
                for (Activity activity : variant) {
                    if (last != null) {
                        if (presetEncoding.isInDomain(last) && postsetEncoding.isInDomain(activity)) {
                            Integer i = presetEncoding.encode(last);
                            Integer j = postsetEncoding.encode(activity);
                            counts[i][j] += f;
                        }
                    }
                    last = activity;
                }
            }

            return new DirectlyFollowsTreeHeuristic(counts);
        }
    }

    protected double[][] eventuallyFollows;

    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        int sum = node.getPlace().preset()
                .streamIndices()
                .flatMap(i -> node.getPlace().postset().streamIndices().map(j -> dfCounts[i][j]))
                .sum();

        return new TreeNodeScore((double) sum / node.getPlace().size());
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.reverseOrder();
    }

}
