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
    protected void removeNode(PlaceNode node) {
        super.removeNode(node);
        removeFromActivityPlacesMapping(node);
    }

    @Override
    protected void clearHeuristic(PlaceNode node) {
        super.clearHeuristic(node);
        // removeNode reicht nicht aus
        // wie in HeuristicTreeExpansion zu sehen ist, können nodes entweder durch
        // dequeue(node) oder dequeueFirst() entfernt werden
        // nur im ersten Fall, der aber bisher von keiner expansion strategy genutzt wird, wird removeNode(node) aufgerufen
        // es ist nicht obvious, dass das der Fall ist
        // in EventingHeuristicTreeExpansion gibt es für das NodeDequeued event daher auch zwei hooks
        // clearHeuristic(node) wird immer am Ende aufgerufen
        // ist nicht sonderlich elegant diese random methode so zu überschreiben, aber ein hackjob ist hackjob
        // ich bevorzuge daher ich extra hook Methoden mit klarer Benennung in Vergangenheitsform und dadurch definierterem method contract (pre-conditions), wie placeRemoved()
        // da wo bisher keine solche Erweiterungen existieren, existieren diese Methoden auch nicht
        // wie auch immer
        removeFromActivityPlacesMapping(node);
    }


    @Override
    public void observe(CandidateCompositionEvent<Place> observation) {
        Place pO = observation.getCandidate();

        switch (observation.getAction()) {
            case Accept:

                //checkMapSanity();

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

    public boolean checkMapSanity() {
        MapIterator<Activity, Transition> mapIterator = actTransMapping.get().mapIterator();
        while(mapIterator.hasNext()) {
            Activity a = mapIterator.next();
            if(activityToIngoingPlaceNodes.containsKey(a)) {
                for (PlaceNode pn : activityToIngoingPlaceNodes.get(a)) {
                    if (!priorityQueue.contains(pn)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
