package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.componenting.system.link.AbstractBaseClass;
import org.processmining.specpp.composition.composers.FelixNewPlaceComposer;
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
import org.processmining.specpp.util.JavaTypingUtils;

import java.util.*;

public class GreedyETCPrecisionTreeHeuristic extends AbstractBaseClass implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    private final BidiMap<Activity, Transition> actTransMapping;
    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSourceE = new DelegatingDataSource<>(HashMap::new);
    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSourceA = new DelegatingDataSource<>(HashMap::new);

    private final double alpha;

    private final int maxSize;


    public GreedyETCPrecisionTreeHeuristic(BidiMap<Activity, Transition> actTransMapping, int maxSize, double alpha) {
        this.actTransMapping = actTransMapping;
        this.alpha = alpha;

        this.maxSize = maxSize;



        globalComponentSystem()
                .provide(DataRequirements.dataSource("activitiesToEscapingEdges_GreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSourceE))
                .provide(DataRequirements.dataSource("activitiesToAllowed_GreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSourceA));
    }

    @Override
    protected void initSelf() {

    }

    public static class Builder extends ComponentSystemAwareBuilder<GreedyETCPrecisionTreeHeuristic> {



        final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

        private final DelegatingDataSource<TreeHeuristcAlpha> delAlpha = new DelegatingDataSource<>();

        private final DelegatingDataSource<IntEncodings<Activity>> encAct = new DelegatingDataSource<>();

        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping).require(DataRequirements.ENC_ACT, encAct).require(ParameterRequirements.TREEHEURISTIC_ALPHA, delAlpha);
        }

        @Override
        protected GreedyETCPrecisionTreeHeuristic buildIfFullySatisfied() {
            int maxSize = encAct.getData().getPresetEncoding().size() + encAct.getData().getPostsetEncoding().size();
            return new GreedyETCPrecisionTreeHeuristic(actTransMapping.getData(), maxSize, delAlpha.getData().getAlpha());
        }
    }


    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        Place p = node.getPlace();
        Map<Activity, Integer> activityToEscapingEdges = delegatingDataSourceE.getData();
        Map<Activity, Integer> activityToAllowed = delegatingDataSourceA.getData();

        if(p.isHalfEmpty()) {
            return new TreeNodeScore(Double.MAX_VALUE);
        } else {
            if(activityToEscapingEdges.isEmpty()) {
                return  new TreeNodeScore(Double.MAX_VALUE);
            } else {

                int sumEPostSet = 0;
                for(Transition t : node.getPlace().postset()) {
                    Activity a = actTransMapping.getKey(t);
                    sumEPostSet += activityToEscapingEdges.get(a);
                }

                int sumAPostSet = 0;
                for(Transition t : node.getPlace().postset()) {
                    Activity a = actTransMapping.getKey(t);
                    sumAPostSet += activityToAllowed.get(a);
                }

                double score = (alpha) * ((double) sumEPostSet / sumAPostSet) + (1-alpha) * (1 - (double) p.size() / maxSize);
                return new TreeNodeScore(score);

            }
        }
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.reverseOrder();
    }

}
