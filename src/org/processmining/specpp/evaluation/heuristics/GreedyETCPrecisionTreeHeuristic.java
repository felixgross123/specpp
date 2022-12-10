package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.componenting.system.link.AbstractBaseClass;
import org.processmining.specpp.composition.composers.FelixNewPlaceComposer;
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

    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping;

    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSource = new DelegatingDataSource<>(HashMap::new);


    public GreedyETCPrecisionTreeHeuristic( DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping) {
        this.actTransMapping = actTransMapping;
        globalComponentSystem().provide(DataRequirements.dataSource("activitiesToEscapingEdges_Map", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSource));
    }

    @Override
    protected void initSelf() {

    }

    public static class Builder extends ComponentSystemAwareBuilder<GreedyETCPrecisionTreeHeuristic> {



        final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();
        private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();


        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping);
        }

        @Override
        protected GreedyETCPrecisionTreeHeuristic buildIfFullySatisfied() {
            return new GreedyETCPrecisionTreeHeuristic(actTransMapping);
        }
    }


    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        Place p = node.getPlace();
        Map<Activity, Integer> activityToEscapingEdges = delegatingDataSource.getData();

        if(p.isHalfEmpty()) {
            return new TreeNodeScore(Double.MAX_VALUE);
        } else {
            if(activityToEscapingEdges.isEmpty()) {
                return  new TreeNodeScore(Double.MAX_VALUE);
            } else {

                int EEPostSet = 0;
                for(Transition t : node.getPlace().postset()) {
                    Activity a = actTransMapping.getData().getKey(t);
                    EEPostSet += activityToEscapingEdges.get(a);
                }

                double score = (double) EEPostSet / node.getPlace().postset().size();
                return new TreeNodeScore(score);

            }
        }
    }

    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.reverseOrder();
    }

}
