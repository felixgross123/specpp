package org.processmining.specpp.composition.composers;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.processmining.specpp.base.AdvancedComposition;
import org.processmining.specpp.base.impls.AbstractComposer;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.delegators.DelegatingEvaluator;
import org.processmining.specpp.componenting.evaluation.EvaluationRequirements;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.datastructures.encoding.BitEncodedSet;
import org.processmining.specpp.datastructures.log.impls.Factory;
import org.processmining.specpp.datastructures.transitionSystems.PAState;
import org.processmining.specpp.datastructures.transitionSystems.PrefixAutomaton;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.specpp.datastructures.petri.CollectionOfPlaces;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.vectorization.VariantMarkingHistories;
import org.processmining.specpp.evaluation.implicitness.ImplicitnessRating;
import org.processmining.specpp.supervision.EventSupervision;
import org.processmining.specpp.supervision.observations.DebugEvent;
import org.processmining.specpp.supervision.piping.PipeWorks;

import java.nio.IntBuffer;
import java.util.*;

public class FelixNewPlaceComposer<I extends AdvancedComposition<Place>> extends AbstractComposer<Place, I, CollectionOfPlaces> {

    private final DelegatingDataSource<Log> logSource = new DelegatingDataSource<>();


    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();
    private final DelegatingEvaluator<Place, ImplicitnessRating> implicitnessEvaluator = new DelegatingEvaluator<>();
    private final DelegatingEvaluator<Place, VariantMarkingHistories> markingHistoriesEvaluator = new DelegatingEvaluator<>();
    private final EventSupervision<DebugEvent> eventSupervisor = PipeWorks.eventSupervision();

    private final PrefixAutomaton prefixAutomaton = new PrefixAutomaton(new PAState());

    private final Map<Activity, Set<Place>> activityToIngoingPlaces = new HashMap<>();
    private final Map<Activity, Set<Place>> activityToOutgoingPlaces = new HashMap<>();;

    private final Map<Place, Integer> placeToEscapingEdges = new HashMap<>();

    private final Set<Place> selfLoopingPlaces = new HashSet<>();



    public FelixNewPlaceComposer(I composition) {
        super(composition, c -> new CollectionOfPlaces(c.toList()));
        globalComponentSystem().require(DataRequirements.RAW_LOG, logSource)
                               .require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                               .require(EvaluationRequirements.PLACE_MARKING_HISTORY, markingHistoriesEvaluator)
                               .provide(SupervisionRequirements.observable("felix.debug", DebugEvent.class, eventSupervisor));
        localComponentSystem().require(EvaluationRequirements.PLACE_IMPLICITNESS, implicitnessEvaluator);
    }


    boolean selfloopconfig = false;
    @Override
    protected boolean deliberateAcceptance(Place candidate) {

        //eventSupervisor.observe(new DebugEvent("read me"));


        //check for self-loops
        if(candidate.preset().intersects(candidate.postset()) && selfloopconfig) {

            //System.out.println("Evaluating Place " + candidate);

            BitEncodedSet <Transition> presetCopy = candidate.preset().copy();
            BitEncodedSet <Transition> postsetCopy = candidate.postset().copy();

            BitEncodedSet <Transition> selfLoopingTransitions = candidate.preset().copy();
            selfLoopingTransitions.intersection(candidate.postset());

            presetCopy.setminus(selfLoopingTransitions);
            postsetCopy.setminus(selfLoopingTransitions);

            Place pWithoutSelf = new Place(presetCopy, postsetCopy);

            boolean containsPWithoutSelf = false;
            for(Place p: composition) {
                if(p.equals(pWithoutSelf)) {
                    //System.out.println("added " + candidate + " (self-loop)");
                    //System.out.println("----------------");
                    placeToEscapingEdges.put(candidate, evaluateEscapingEdges(candidate));
                    return true;
                }
            }

            //System.out.println("rejected " + candidate + " (self-loop)");
            //System.out.println("----------------");
            return false;

        }else {
            //System.out.println("Evaluating Place " + candidate);

            Log log = logSource.getData();

            BitEncodedSet<Transition> candidateOut = candidate.postset();
            Set<Place> placesToReevaluate = new HashSet<>();

            for (Transition t : candidate.postset()) {
                placesToReevaluate.addAll(activityToIngoingPlaces.get(actTransMapping.get().getKey(t)));
            }


            if (!placesToReevaluate.isEmpty()) {

                Map<Place, Integer> tmpPlaceToEscapingEdges = new HashMap<>();

                boolean candidateIsMorePrecise = false;

                addToActivityPlacesMapping(candidate);

                for (Place p : placesToReevaluate) {

                    int oldScore = placeToEscapingEdges.get(p);
                    int newScore = evaluateEscapingEdges(p);

                    tmpPlaceToEscapingEdges.put(p, newScore);

                    if (oldScore > newScore) {
                        candidateIsMorePrecise = true;
                    }
                }

                removeFromActivityPlacesMapping(candidate);

                if (!candidateIsMorePrecise) {
                    // no decrease in EE(p) for any place p that was reevaluated
                    //System.out.println("Place " + candidate + " not accepted (no increase in precision)");
                    //System.out.println("----------------");
                    return false;

                } else {

                    // candidate place makes the result more precise -> check for potentially implicit places

                    int candidateScore = evaluateEscapingEdges(candidate);

                    LinkedList<Place> potImpl = new LinkedList<>();

                    //Note: tmpPlaceEscapingEdgesPairs contains all placesToReevaluate as keys
                    Set<Map.Entry<Place, Integer>> tmpPlaceEscapingEdgesPairs = tmpPlaceToEscapingEdges.entrySet();

                    //collect potentiallyImplicitPlaces
                    for (Map.Entry<Place, Integer> e : tmpPlaceEscapingEdgesPairs) {
                        if (e.getValue() <= candidateScore) {
                            potImpl.add(e.getKey());
                        }
                    }

                    //check implicitness and remove
                    addToActivityPlacesMapping(candidate);
                    List<Place> implicitRemoved = new LinkedList<>();

                    for (Place pPotImpl : potImpl) {

                        //System.out.println("ImplicitnessCheck: " + pPotImpl);

                        removeFromActivityPlacesMapping(pPotImpl);

                        BitEncodedSet<Transition> pPotImplOut = pPotImpl.postset();
                        Set<Place> placesToReevaluatePPotImpl = new HashSet<>();

                        //Collect Places to reevaluate for implicitness check of pPotImpl
                        for (Transition t : pPotImpl.postset()) {
                            Set<Place> places = activityToIngoingPlaces.get(actTransMapping.get().getKey(t));
                            for (Place p : places) {
                                if (!p.equals(pPotImpl)) {
                                    placesToReevaluatePPotImpl.add(p);
                                }
                            }

                        }
                        boolean remove = true;

                        for (Place p : placesToReevaluatePPotImpl) {

                            int oldScore;
                            if (p.equals(candidate)) {
                                oldScore = candidateScore;
                            } else if (tmpPlaceToEscapingEdges.containsKey(p)) {
                                oldScore = tmpPlaceToEscapingEdges.get(p);
                            } else {
                                oldScore = placeToEscapingEdges.get(p);
                            }

                            //System.out.print("Reeval ");
                            int newScore = evaluateEscapingEdges(p);

                            if (oldScore < newScore) {
                                remove = false;
                                break;
                            }
                        }
                        addToActivityPlacesMapping(pPotImpl);

                        if (remove) {
                            revokeAcceptance(pPotImpl);
                            tmpPlaceToEscapingEdges.remove(pPotImpl);
                            //System.out.println(pPotImpl + " implicit --> remove");
                        }


                    }

                    removeFromActivityPlacesMapping(candidate);

                    tmpPlaceEscapingEdgesPairs = tmpPlaceToEscapingEdges.entrySet();
                    //update scores
                    for (Map.Entry<Place, Integer> e : tmpPlaceEscapingEdgesPairs) {
                        placeToEscapingEdges.put(e.getKey(), e.getValue());
                    }

                    placeToEscapingEdges.put(candidate, candidateScore);
                    //System.out.println(candidate + " accepted");
                    //System.out.println("----------------");
                    return true;
                }

            } else {
                // no places of composition need to be reevaluated -> insert p, calculate EE(p)
                placeToEscapingEdges.put(candidate, evaluateEscapingEdges(candidate));
                //System.out.println(candidate + " accepted (postset empty)");
                //System.out.println("----------------");
                return true;
            }
        }
    }

    @Override
    protected void acceptanceRevoked(Place candidate) {

        //update ActivityPlaceMapping
        removeFromActivityPlacesMapping(candidate);
        //update PlaceToEE
        placeToEscapingEdges.remove(candidate);


    }

    @Override
    protected void candidateAccepted(Place candidate) {
        //update ActivityPlaceMapping
        addToActivityPlacesMapping(candidate);
    }

    @Override
    protected void candidateRejected(Place candidate) {

    }


    @Override
    public void candidatesAreExhausted() {

    }



    @Override
    protected void initSelf() {

        // Build Prefix-Automaton
        Log log = logSource.getData();
        for (IndexedVariant indexedVariant : log) {
            prefixAutomaton.addVariant(indexedVariant.getVariant());
        }

        // Init ActivityPlaceMapping
        MapIterator<Activity, Transition> mapIterator = actTransMapping.get().mapIterator();
        while(mapIterator.hasNext()) {
            Activity a = mapIterator.next();
            activityToIngoingPlaces.put(a, new HashSet<>());
            activityToOutgoingPlaces.put(a, new HashSet<>());
        }



    }

    private void addToActivityPlacesMapping(Place p){
        for(Transition t : p.preset()) {
            Set<Place> tOut = activityToOutgoingPlaces.get(actTransMapping.get().getKey(t));
            tOut.add(p);
        }
        for(Transition t : p.postset()) {
            Set<Place> tIn = activityToIngoingPlaces.get(actTransMapping.get().getKey(t));
            tIn.add(p);
        }
    }

    private void removeFromActivityPlacesMapping(Place p){
        for(Transition t : p.preset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<Place> tOut = activityToOutgoingPlaces.get(a);
            tOut.remove(p);
        }
        for(Transition t : p.postset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<Place> tIn = activityToIngoingPlaces.get(a);
            tIn.remove(p);
        }
    }


    @Override
    public boolean isFinished() {
        for(Place p : composition) {
            if(placeToEscapingEdges.get(p) != 0) {
                return false;
            }
        }
        Set<Map.Entry<Activity, Set<Place>>> s  = activityToIngoingPlaces.entrySet();
        for(Map.Entry<Activity, Set<Place>> se  : s) {
            if (!se.getKey().equals(Factory.ARTIFICIAL_START) && se.getValue().isEmpty()) {
                return false;
            }
        };
        //System.out.println("PREMETURE ABORT");
        return true;
    }

    public int evaluateEscapingEdges(Place candidate) {

        int escapingEdges = 0;

        for(Transition t : candidate.postset()) {

            Activity o = actTransMapping.get().getKey(t);

            Set<Place> prerequisites = activityToIngoingPlaces.get(o);

            // Collect MarkingHistories
            LinkedList<VariantMarkingHistories> markingHistories = new LinkedList<>();
            for(Place p : prerequisites) {
                markingHistories.add(markingHistoriesEvaluator.eval(p));
            }
            markingHistories.add(markingHistoriesEvaluator.eval(candidate));

            //check if o is escaping
            Log log = logSource.getData();

            for(IndexedVariant variant : log) {

                int vIndex = variant.getIndex();
                int length = variant.getVariant().getLength();

                PAState logState = prefixAutomaton.getInitial();


                for(int j = 1; j < length*2; j+=2) {
                    int aIndex = ((j+1)/2)-1;

                    // update log state
                    logState = logState.getTrans(log.getVariant(vIndex).getAt(aIndex)).getPointer();

                    boolean isAllowedO = true;
                    for(VariantMarkingHistories h : markingHistories) {
                        //check if column = 1 f.a. p in prerequites
                        // TODO: multiple tokens in all prereq -> multiply with |token| for EE(p)?

                        IntBuffer buffer = h.getAt(vIndex);
                        int p = buffer.position();

                        if(buffer.get(p+j) == 0) {
                            isAllowedO = false;
                            break;
                        }
                    }

                    if(isAllowedO) {
                        if(!logState.checkForOutgoingAct(o)) {
                            //o is not reflected, hence escaping
                            escapingEdges += log.getVariantFrequency(vIndex);
                        }
                    }
                }
            }
        }
        //System.out.println("EE(" + candidate + ")= " + escapingEdges);
        return escapingEdges;
    }



}
