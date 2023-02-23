package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.composition.events.CandidateCompositionEvent;
import org.processmining.specpp.config.parameters.PrecisionTresholdGamma;
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

    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSourceE = new DelegatingDataSource<>(HashMap::new);
    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSourceA = new DelegatingDataSource<>(HashMap::new);

    private final DelegatingDataSource<PrecisionTresholdGamma> gamma = new DelegatingDataSource<>();
    public UpdateGreedyTreeHeuristic(HeuristicStrategy heuristicStrategy) {
        super(heuristicStrategy);


        globalComponentSystem().require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                                .require(ParameterRequirements.PRECISION_TRHESHOLD_GAMMA, gamma);
        globalComponentSystem().provide(DataRequirements.dataSource("activitiesToEscapingEdges_UpdatingGreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSourceE))
                                .provide(DataRequirements.dataSource("activitiesToAllowed_UpdatingGreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSourceA));


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
    private Map<Activity, Integer> activitiesToAllowedPrevious= new HashMap<>();


    @Override
    public void observe(CandidateCompositionEvent<Place> observation) {
        Place pO = observation.getCandidate();

        switch (observation.getAction()) {
            case Accept:
                updatePlaces(pO);
                break;

            case RevokeAcceptance:
                if(gamma.getData().getG() > 0) {
                    updatePlaces(pO);
                }
                break;

            default:
                break;
        }
    }

    private void updatePlaces(Place pO) {
        Set<Activity> actvitiesChanged = new HashSet<>();

        Map<Activity, Integer> activitiesToEscapingEdges = delegatingDataSourceE.getData();
        Map<Activity, Integer> activitiesToAllowed = delegatingDataSourceA.getData();

        for(Transition t : pO.postset()) {
            Activity a = actTransMapping.get().getKey(t);
            if(activitiesToEscapingEdgesPrevious.isEmpty() && activitiesToAllowedPrevious.isEmpty()) {
                //only at the first accepted place: add all postset activities
                actvitiesChanged.add(a);
            } else {
                //check if EE Score has changed
                if (!activitiesToEscapingEdgesPrevious.get(a).equals(activitiesToEscapingEdges.get(a)) || !activitiesToAllowedPrevious.get(a).equals(activitiesToAllowed.get(a))) {
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
        activitiesToEscapingEdgesPrevious = new HashMap<>(delegatingDataSourceE.getData());
        activitiesToAllowedPrevious = new HashMap<>(delegatingDataSourceA.getData());
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
