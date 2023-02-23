package org.processmining.specpp.evaluation.heuristics;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.config.parameters.TreeHeuristcAlpha;
import org.processmining.specpp.datastructures.encoding.IntEncoding;
import org.processmining.specpp.datastructures.encoding.IntEncodings;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.log.Variant;
import org.processmining.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.heuristic.SubtreeMonotonicity;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import org.processmining.specpp.datastructures.vectorization.IntVector;
import org.processmining.specpp.traits.ZeroOneBounded;

import java.util.Comparator;

public class DirectlyFollowsTreeHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {
    private final int[][] dfCounts;

    private final double alpha;
    private final int maxDF;
    private final int maxSize;

    public DirectlyFollowsTreeHeuristic(int[][] dfCounts, double alpha, int maxDF, int maxSize) {
        this.dfCounts = dfCounts;
        this.alpha = alpha;
        this.maxDF = maxDF;
        this.maxSize = maxSize;
    }

    public static class Builder extends ComponentSystemAwareBuilder<DirectlyFollowsTreeHeuristic> {


        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<IntEncodings<Activity>> encAct = new DelegatingDataSource<>();
        private final DelegatingDataSource<TreeHeuristcAlpha> alpha = new DelegatingDataSource<>();


        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ENC_ACT, encAct).require(ParameterRequirements.TREEHEURISTIC_ALPHA, alpha);
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

            //search for max DF value
            int maxDF = 0;

            for (int[] rows : counts) {
                for (int entry : rows) {
                    if (entry > maxDF) {
                        maxDF = entry;
                    }
                }
            }


            int maxSize = encAct.getData().getPresetEncoding().size() + encAct.getData().getPostsetEncoding().size();

            return new DirectlyFollowsTreeHeuristic(counts, alpha.getData().getAlpha(), maxDF, maxSize);
        }
    }

    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        int sum = node.getPlace().preset()
                .streamIndices()
                .flatMap(i -> node.getPlace().postset().streamIndices().map(j -> dfCounts[i][j]))
                .sum();

        double score = alpha * (((double) sum / (node.getPlace().preset().size() * node.getPlace().postset().size())) / maxDF) + (1-alpha) * (1-((double) node.getPlace().size() / maxSize));

        return new TreeNodeScore(score);
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.reverseOrder();
    }

}
