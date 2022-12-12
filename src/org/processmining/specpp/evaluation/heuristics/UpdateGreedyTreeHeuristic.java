package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.composition.events.CandidateCompositionEvent;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.heuristic.UpdatableHeuristicExpansionStrategy;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import java.util.*;

public class UpdateGreedyTreeHeuristic extends UpdatableHeuristicExpansionStrategy<Place, PlaceNode, TreeNodeScore> {

    private final Map<Activity, Set<PlaceNode>> activityToIngoingPlaceNodes = new HashMap<>();
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();


    public UpdateGreedyTreeHeuristic(HeuristicStrategy heuristicStrategy) {
        super(heuristicStrategy);


        globalComponentSystem().require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping);

    }

    @Override
    protected void addNode(PlaceNode node, TreeNodeScore score) {
        super.addNode(node, score);
        addToActivityPlacesMapping(node);

    }

    @Override
    protected void clearHeuristic(PlaceNode node) {
        super.clearHeuristic(node);
        removeFromActivityPlacesMapping(node);
    }


    @Override
    public void observe(CandidateCompositionEvent<Place> observation) {
        Place pO = observation.getCandidate();

        switch (observation.getAction()) {
            case Accept:
                Set<PlaceNode> toUpdate = new HashSet<>();

                for(Transition t : pO.postset()) {
                    Activity a = actTransMapping.get().getKey(t);
                    toUpdate.addAll(activityToIngoingPlaceNodes.get(a));
                }

                for (PlaceNode p : toUpdate) {
                    updateNode(p, getHeuristicStrategy().computeHeuristic(p));
                }
                break;

            default:
                break;
        }
    }


    private void addToActivityPlacesMapping(PlaceNode p){
        for (Transition t : p.getPlace().postset()) {
            Activity a = actTransMapping.get().getKey(t);

            if (!activityToIngoingPlaceNodes.containsKey(a)) {
                Set<PlaceNode> s = new HashSet<>();
                s.add(p);
                activityToIngoingPlaceNodes.put(a, s);
            } else {
                Set<PlaceNode> tIn = activityToIngoingPlaceNodes.get(a);
                tIn.add(p);
            }
        }
    }

    private void removeFromActivityPlacesMapping(PlaceNode p){
        for(Transition t : p.getPlace().postset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<PlaceNode> tIn = activityToIngoingPlaceNodes.get(a);
            tIn.remove(p);
        }
    }

}
