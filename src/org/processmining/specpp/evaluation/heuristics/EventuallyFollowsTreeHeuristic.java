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
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.heuristic.SubtreeMonotonicity;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import org.processmining.specpp.datastructures.vectorization.IntVector;
import org.processmining.specpp.traits.ZeroOneBounded;

import java.util.Comparator;
import java.util.PrimitiveIterator;

public class EventuallyFollowsTreeHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    public EventuallyFollowsTreeHeuristic(int[][] eventuallyFollows, double alpha, double maxEF, int maxSize) {
        this.eventuallyFollows = eventuallyFollows;
        this.alpha = alpha;
        this.maxEF = maxEF;
        this.maxSize = maxSize;
    }

    public static class Builder extends ComponentSystemAwareBuilder<EventuallyFollowsTreeHeuristic> {


        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<IntEncodings<Activity>> encAct = new DelegatingDataSource<>();
        private final DelegatingDataSource<TreeHeuristcAlpha> alpha = new DelegatingDataSource<>();

        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ENC_ACT, encAct).require(ParameterRequirements.TREEHEURISTIC_ALPHA, alpha);
        }

        @Override
        protected EventuallyFollowsTreeHeuristic buildIfFullySatisfied() {
            Log log = rawLog.getData();
            IntEncodings<Activity> activityIntEncodings = encAct.getData();
            IntEncoding<Activity> presetEncoding = activityIntEncodings.getPresetEncoding();
            IntEncoding<Activity> postsetEncoding = activityIntEncodings.getPostsetEncoding();
            IntVector frequencies = log.getVariantFrequencies();
            int preSize = presetEncoding.size();
            int postSize = postsetEncoding.size();

            int[][] ef = new int[preSize][postSize];
            for (int i = 0; i < ef.length; i++) {
                ef[i] = new int[postSize];
            }

            for (IndexedVariant indexedVariant : log) {
                Variant variant = indexedVariant.getVariant();
                double f = frequencies.get(indexedVariant.getIndex());
                for (int i = 0; i < variant.getLength(); i++) {
                    Activity a = variant.getAt(i);
                    if (presetEncoding.isInDomain(a)) {
                        Integer m = presetEncoding.encode(a);
                        for (int j = i + 1; j < variant.getLength(); j++) {
                            Activity b = variant.getAt(j);
                            if (postsetEncoding.isInDomain(b)) {
                                Integer n = postsetEncoding.encode(b);
                                ef[m][n] += f;
                            }
                        }
                    }
                }
            }


            //search for max EF value
            double maxEF = 0;

            for (int[] rows : ef) {
                for (int entry : rows) {
                    if (entry > maxEF) {
                        maxEF = entry;
                    }
                }
            }

            int maxSize = encAct.getData().getPresetEncoding().size() + encAct.getData().getPostsetEncoding().size();

            return new EventuallyFollowsTreeHeuristic(ef, alpha.getData().getAlpha(), maxEF, maxSize);
        }
    }

    protected int[][] eventuallyFollows;
    private final double alpha;
    private final double maxEF;
    private final int maxSize;

    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        double sum = node.getPlace().preset()
                .streamIndices()
                .flatMap(i -> node.getPlace().postset().streamIndices().map(j -> eventuallyFollows[i][j]))
                .sum();

        double score = alpha * ((double) sum / (node.getPlace().preset().size() * node.getPlace().postset().size()) / maxEF) + (1-alpha) * (1 - ((double) node.getPlace().size() / maxSize));
        return new TreeNodeScore(score);
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.reverseOrder();
    }

}
