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
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.log.impls.Factory;
import org.processmining.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.specpp.datastructures.petri.CollectionOfPlaces;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.transitionSystems.PAState;
import org.processmining.specpp.datastructures.transitionSystems.PrefixAutomaton;
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

    private final Map<Activity, Integer> activityToEscapingEdges = new HashMap<>();

    private boolean foundOnCurrentLevel = true;

    private boolean levelChange = false;
    private int currentLevel;


    public FelixNewPlaceComposer(I composition) {
        super(composition, c -> new CollectionOfPlaces(c.toList()));
        globalComponentSystem().require(DataRequirements.RAW_LOG, logSource)
                               .require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                               .require(EvaluationRequirements.PLACE_MARKING_HISTORY, markingHistoriesEvaluator)
                               .provide(SupervisionRequirements.observable("felix.debug", DebugEvent.class, eventSupervisor));
        localComponentSystem().require(EvaluationRequirements.PLACE_IMPLICITNESS, implicitnessEvaluator);
    }


    @Override
    protected boolean deliberateAcceptance(Place candidate) {

        //eventSupervisor.observe(new DebugEvent("read me"));

        //System.out.println("Evaluating Place " + candidate);

        Log log = logSource.getData();

        if (!checkPrecisionGain(candidate)) {
            // no decrease in EE(a) for any activity a that was reevaluated

            //begin swap multiple simpler places for complex

            LinkedList<Place> potImpl = new LinkedList<>();

            BitEncodedSet<Transition> candidateOut = candidate.postset();
            Set<Activity> activitiesToRevealuate = new HashSet<>();
            for (Transition t: candidateOut) {
                activitiesToRevealuate.add(actTransMapping.getData().getKey(t));
            }
            for(Activity a : activitiesToRevealuate){
                potImpl.addAll(activityToIngoingPlaces.get(a));
            }
            Set<Place> impl = new HashSet<>();

            addToActivityPlacesMapping(candidate);

            for (Place p : potImpl) {
                if (checkImplicitness(p)) {
                    impl.add(p);
                }
            }

            removeFromActivityPlacesMapping(candidate);

            if(impl.size() > 1) {
                for (Place p : impl) {
                    revokeAcceptance(p);
                }
                return true;
            }

            //end swap multiple simpler places for complex


            //System.out.println("Place " + candidate + " not accepted (no increase in precision)");
            //System.out.println("----------------");
            return false;

        } else {

            // candidate place makes the result more precise -> check for potentially implicit places
            //System.out.println("Place " + candidate + " accepted");

            //collect potentially implcit places
            LinkedList<Place> potImpl = new LinkedList<>();

            BitEncodedSet<Transition> candidateOut = candidate.postset();
            Set<Activity> activitiesToRevealuate = new HashSet<>();
            for (Transition t: candidateOut) {
                activitiesToRevealuate.add(actTransMapping.getData().getKey(t));
            }
            for(Activity a : activitiesToRevealuate){
                potImpl.addAll(activityToIngoingPlaces.get(a));
            }

            //check implicitness and remove
            addToActivityPlacesMapping(candidate);
            List<Place> implicitRemoved = new LinkedList<>();

            for (Place pPotImpl : potImpl) {

                //System.out.println("ImplicitnessCheck: " + pPotImpl);
                if (checkImplicitness(pPotImpl)) {

                    revokeAcceptance(pPotImpl);
                    //System.out.println(pPotImpl + " implicit --> remove");
                }
            }

            removeFromActivityPlacesMapping(candidate);
            //System.out.println("----------------");

            return true;
        }
    }

    public boolean checkPrecisionGain(Place p) {
        BitEncodedSet<Transition> candidateOut = p.postset();
        Set<Activity> activitiesToRevealuate = new HashSet<>();
        for (Transition t: candidateOut) {
            activitiesToRevealuate.add(actTransMapping.getData().getKey(t));
        }

        boolean isMorePrecise = false;

        addToActivityPlacesMapping(p);

        for (Activity a : activitiesToRevealuate) {
            int oldScore = activityToEscapingEdges.get(a);
            int newScore = evaluateEscapingEdges(a);

            if (oldScore > newScore) {
                isMorePrecise = true;
                //note: if p brings gain in precision we assume that it will be accepted (hence we update the score)
                activityToEscapingEdges.put(a, newScore);
            }
        }
        removeFromActivityPlacesMapping(p);

        return isMorePrecise;
    }

    public boolean checkImplicitness(Place p) {

        BitEncodedSet<Transition> pPotImplOut = p.postset();
        Set<Activity> activitiesToReevaluatePPotImpl = new HashSet<>();
        for (Transition t: pPotImplOut) {
            activitiesToReevaluatePPotImpl.add(actTransMapping.getData().getKey(t));
        }

        removeFromActivityPlacesMapping(p);

        boolean implicit = true;

        for (Activity a : activitiesToReevaluatePPotImpl) {

            int oldScore = activityToEscapingEdges.get(a);
            //System.out.print("Reeval ");

            int newScore = evaluateEscapingEdges(a);

            if (oldScore < newScore) {
                implicit = false;
                break;
            }
        }

        addToActivityPlacesMapping(p);

        return implicit;
    }

    @Override
    protected void acceptanceRevoked(Place candidate) {
        //update ActivityPlaceMapping
        removeFromActivityPlacesMapping(candidate);
    }

    @Override
    protected void candidateAccepted(Place candidate) {
        //update ActivityPlaceMapping
        addToActivityPlacesMapping(candidate);

        //update currentTreeLevel
        foundOnCurrentLevel = true;
        if(candidate.size() > currentLevel){
            currentLevel = candidate.size();
            levelChange = true;
        }
    }

    @Override
    protected void candidateRejected(Place candidate) {
        //update currentTreeLevel
        if(candidate.size() > currentLevel){
            currentLevel = candidate.size();
            levelChange = true;
        }
    }


    @Override
    public void candidatesAreExhausted() {

    }



    @Override
    protected void initSelf() {

        currentLevel = 2;
        System.out.println("Level: " + currentLevel);

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
        }

        // Init EscapingEdges
        Set<Activity> activities = actTransMapping.getData().keySet();
        for(Activity a : activities) {
            if(!a.equals(Factory.ARTIFICIAL_START)) {
                activityToEscapingEdges.put(a, evaluateEscapingEdges(a));
            }
        }



    }

    private void addToActivityPlacesMapping(Place p){
        for(Transition t : p.postset()) {
            Set<Place> tIn = activityToIngoingPlaces.get(actTransMapping.get().getKey(t));
            tIn.add(p);
        }
    }

    private void removeFromActivityPlacesMapping(Place p){
        for(Transition t : p.postset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<Place> tIn = activityToIngoingPlaces.get(a);
            tIn.remove(p);
        }
    }


    @Override
    public boolean isFinished() {

        if(levelChange) {
            System.out.println("Level: " + currentLevel);
            //check if place was added on previous level
            if(!foundOnCurrentLevel) {
                System.out.println("PREMATURE ABORT (no place found on level) " + (currentLevel-1));
                return true;
            }
            foundOnCurrentLevel = false;
            levelChange = false;

            //check if optimal precision is reached
            for(int i : activityToEscapingEdges.values()) {
                if(i != 0){
                    return false;
                }
            }

            System.out.println("PREMATURE ABORT (optimal precision reached)");
            return true;
        }
        return false;
    }

    public int evaluateEscapingEdges(Activity a) {

        int escapingEdges = 0;
        Log log = logSource.getData();

        if(activityToIngoingPlaces.get(a).isEmpty()) {
            // calculate initial score

            for(IndexedVariant variant : log) {
                int vIndex = variant.getIndex();

                for(int i=0; i<variant.getVariant().getLength(); i++) {
                    if(!variant.getVariant().getAt(i).equals(a)) {
                        escapingEdges += log.getVariantFrequency(vIndex);
                    }
                }
            }

        }else{
            Set<Place> prerequisites = activityToIngoingPlaces.get(a);

            // collect markingHistories
            LinkedList<VariantMarkingHistories> markingHistories = new LinkedList<>();
            for(Place p : prerequisites) {
                markingHistories.add(markingHistoriesEvaluator.eval(p));
            }

            //iterate log: variant by variant, activity by activity

            for(IndexedVariant variant : log) {

                int vIndex = variant.getIndex();
                int length = variant.getVariant().getLength();

                PAState logState = prefixAutomaton.getInitial();


                for (int j = 1; j < length * 2; j += 2) {
                    int aIndex = ((j + 1) / 2) - 1;

                    // update log state
                    logState = logState.getTrans(log.getVariant(vIndex).getAt(aIndex)).getPointer();

                    boolean isAllowedO = true;

                    for (VariantMarkingHistories h : markingHistories) {
                        //check if column = 1 f.a. p in prerequites --> activity a is allowed
                        IntBuffer buffer = h.getAt(vIndex);
                        int p = buffer.position();

                        if (buffer.get(p + j) == 0) {
                            isAllowedO = false;
                            break;
                        }
                    }

                    if (isAllowedO) {
                        //check if a is reflected

                        if (!logState.checkForOutgoingAct(a)) {
                            //a is not reflected, hence escaping
                            escapingEdges += log.getVariantFrequency(vIndex);
                        }
                    }
                }
            }
        }
        //System.out.println("EE(" + a +") = " + escapingEdges);
        return escapingEdges;
    }



}
