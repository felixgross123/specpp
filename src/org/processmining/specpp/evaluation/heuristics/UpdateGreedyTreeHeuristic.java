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
import org.processmining.specpp.util.JavaTypingUtils;

import java.util.*;

public class UpdateGreedyTreeHeuristic extends UpdatableHeuristicExpansionStrategy<Place, PlaceNode, TreeNodeScore> {

    private final Map<Activity, Set<PlaceNode>> activityToIngoingPlaceNodes = new HashMap<>();
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSource = new DelegatingDataSource<>(HashMap::new);


    public UpdateGreedyTreeHeuristic(HeuristicStrategy heuristicStrategy) {
        super(heuristicStrategy);


        globalComponentSystem().require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping);
        globalComponentSystem().provide(DataRequirements.dataSource("activitiesToEscapingEdges_Map_1", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSource));


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

    private Map<Activity, Integer> activitiesToEscapingEdgesPrevious = new HashMap<>();


    @Override
    public void observe(CandidateCompositionEvent<Place> observation) {
        Place pO = observation.getCandidate();

        switch (observation.getAction()) {
            case Accept:

                    Set<Activity> actvitiesChanged = new HashSet<>();

                    Map<Activity, Integer> activitiesToEscapingEdges = delegatingDataSource.getData();
                    for(Transition t : pO.postset()) {
                        Activity a = actTransMapping.get().getKey(t);
                        if(activitiesToEscapingEdgesPrevious.isEmpty()) {
                            //only at the first accepted place: add all postset activities
                            actvitiesChanged.add(a);
                        } else {
                            //check if EE Score has changed
                            if (!activitiesToEscapingEdgesPrevious.get(a).equals(activitiesToEscapingEdges.get(a))) {
                                actvitiesChanged.add(a);
                            }
                        }
                    }

                    //Collect Places that need to be updated
                    Set<PlaceNode> placesToUpdate = new HashSet<>();

                    for(Activity a : actvitiesChanged) {
                        placesToUpdate.addAll(activityToIngoingPlaceNodes.get(a));
                    }

                    for (PlaceNode p : placesToUpdate) {
                        updateNode(p, getHeuristicStrategy().computeHeuristic(p));
                    }

                //update previous map
                activitiesToEscapingEdgesPrevious = new HashMap<>(delegatingDataSource.getData());

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
