package org.processmining.specpp.composition.composers;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.processmining.specpp.base.AdvancedComposition;
import org.processmining.specpp.base.impls.AbstractComposer;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.DataSource;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.data.StaticDataSource;
import org.processmining.specpp.componenting.delegators.ConsumingContainer;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.delegators.DelegatingEvaluator;
import org.processmining.specpp.componenting.evaluation.EvaluationRequirements;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.composition.events.CandidateAcceptanceRevoked;
import org.processmining.specpp.composition.events.CandidateAccepted;
import org.processmining.specpp.composition.events.CandidateCompositionEvent;
import org.processmining.specpp.composition.events.CandidateRejected;
import org.processmining.specpp.config.parameters.PrecisionThreshold;
import org.processmining.specpp.config.parameters.TauFitnessThresholds;
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
import org.processmining.specpp.util.JavaTypingUtils;

import javax.xml.crypto.Data;
import java.nio.IntBuffer;
import java.util.*;

public class FelixNewPlaceComposer<I extends AdvancedComposition<Place>> extends AbstractComposer<Place, I, CollectionOfPlaces> {

    private final DelegatingDataSource<Log> logSource = new DelegatingDataSource<>();
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();
    private final DelegatingEvaluator<Place, VariantMarkingHistories> markingHistoriesEvaluator = new DelegatingEvaluator<>();
    private final EventSupervision<DebugEvent> eventSupervisor = PipeWorks.eventSupervision();
    private final DelegatingDataSource<PrecisionThreshold> precisionThreshold = new DelegatingDataSource<>();
    private final PrefixAutomaton prefixAutomaton = new PrefixAutomaton(new PAState());
    private final Map<Activity, Set<Place>> activityToIngoingPlaces = new HashMap<>();
    private final Map<Activity, Integer> activityToEscapingEdges = new HashMap<>();
    private final Map<Activity, Integer> activityToAllowed = new HashMap<>();
    private final EventSupervision<CandidateCompositionEvent<Place>> compositionEventSupervision = PipeWorks.eventSupervision();

    public FelixNewPlaceComposer(I composition) {
        super(composition, c -> new CollectionOfPlaces(c.toList()));

        ConsumingContainer<DataSource<DelegatingDataSource<Map<Activity, Integer>>>> consActivitiesToEscapingEdges = new ConsumingContainer<>(del -> del.getData().setDelegate(StaticDataSource.of(activityToEscapingEdges)));
        globalComponentSystem().require(DataRequirements.dataSource("activitiesToEscapingEdges_Map", JavaTypingUtils.castClass(DelegatingDataSource.class)), consActivitiesToEscapingEdges);

        ConsumingContainer<DataSource<DelegatingDataSource<Map<Activity, Integer>>>> consActivitiesToEscapingEdges1 = new ConsumingContainer<>(del -> del.getData().setDelegate(StaticDataSource.of(activityToEscapingEdges)));
        globalComponentSystem().require(DataRequirements.dataSource("activitiesToEscapingEdges_Map_1", JavaTypingUtils.castClass(DelegatingDataSource.class)), consActivitiesToEscapingEdges1);

        globalComponentSystem().require(DataRequirements.RAW_LOG, logSource)
                               .require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                               .require(EvaluationRequirements.PLACE_MARKING_HISTORY, markingHistoriesEvaluator)
                                .require(ParameterRequirements.PRECISION_TRHESHOLD, precisionThreshold)
                               .provide(SupervisionRequirements.observable("felix.debug", DebugEvent.class, eventSupervisor));

        localComponentSystem().provide(SupervisionRequirements.observable("composer.events", JavaTypingUtils.castClass(CandidateCompositionEvent.class), compositionEventSupervision));
    }


    @Override
    protected boolean deliberateAcceptance(Place candidate) {

        //eventSupervisor.observe(new DebugEvent("read me"));

        //System.out.println("Evaluating Place " + candidate);

        if (!checkPrecisionGain(candidate)) {
            // no decrease in EE(a) for any activity a that was reevaluated

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

            for (Place pPotImpl : potImpl) {

                //System.out.println("ImplicitnessCheck: " + pPotImpl);
                if (checkImplicitness(pPotImpl)) {

                    revokeAcceptance(pPotImpl);
                    //System.out.println(pPotImpl + " implicit --> remove");
                }
            }

            removeFromActivityPlacesMapping(candidate);
            //System.out.println("----------------");

            newAddition = true;
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
            int[] evalRes = evaluateEscapingEdges(a);
            int oldEE = activityToEscapingEdges.get(a);
            int newEE = evalRes[0];
            int newAllowed = evalRes[1];

            if (oldEE > newEE) {
                isMorePrecise = true;
                //note: if p brings gain in precision we assume that it will be accepted (hence we update the scores)
                activityToEscapingEdges.put(a, newEE);
                activityToAllowed.put(a, newAllowed);

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

            int[] evalRes = evaluateEscapingEdges(a);
            int oldEE = activityToEscapingEdges.get(a);
            int newEE = evalRes[0];
            int oldAllowed = activityToAllowed.get(a);
            int newAllowed = evalRes[1];

            if (!(oldEE == newEE && oldAllowed == newAllowed)) {
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
        compositionEventSupervision.observe(new CandidateAcceptanceRevoked<>(candidate));
    }

    @Override
    protected void candidateAccepted(Place candidate) {
        //update ActivityPlaceMapping
        addToActivityPlacesMapping(candidate);

        compositionEventSupervision.observe(new CandidateAccepted<>(candidate));

    }

    @Override
    protected void candidateRejected(Place candidate) {

        //check levelChange

        compositionEventSupervision.observe(new CandidateRejected<>(candidate));

    }


    @Override
    public void candidatesAreExhausted() {

    }



    @Override
    protected void initSelf() {

        // Build Prefix-Automaton
        Log log = logSource.getData();
        for(IndexedVariant indexedVariant : log) {
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
                int[] evalRes = evaluateEscapingEdges(a);
                int EE = evalRes[0];
                int allowed = evalRes[1];
                activityToEscapingEdges.put(a, EE);
                activityToAllowed.put(a, allowed);
            }
        }
        System.out.println(".");
    }

    private void addToActivityPlacesMapping(Place p){
        for(Transition t : p.postset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<Place> tIn = activityToIngoingPlaces.get(a);
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

    private boolean newAddition = false;

    @Override
    public boolean isFinished() {

        if(newAddition) {
            newAddition = false;
            if (optimalPrecisionReached()) {
                return true;
            } else {
                return checkPrecisionThreshold(precisionThreshold.get().getP());
            }
        } else {
            return false;
        }
    }

    public boolean checkPrecisionThreshold(double p) {
        int EE = 0;
        for (int i : activityToEscapingEdges.values()) {
            EE += i;
        }
        int allowed = 0;
        for (int i : activityToAllowed.values()) {
            allowed += i;
        }

        double precisionApprox = (1 - ((double)EE/allowed));
        if(precisionApprox > p) {
            System.out.println("PREMATURE ABORT precision threshold " + precisionThreshold.get().getP() + " reached");
            return true;
        } else {
            return false;
        }

    }

    public boolean optimalPrecisionReached() {
        for(int i : activityToEscapingEdges.values()) {
            if(i != 0){
                return false;
            }
        }
        System.out.println("PREMATURE ABORT optimal precision reached");
        return true;
    }

    public int[] evaluateEscapingEdges(Activity a) {

        int escapingEdges = 0;
        int allowed = 0;
        Log log = logSource.getData();


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

                for(VariantMarkingHistories h : markingHistories) {
                    //check if column = 1 f.a. p in prerequites --> activity a is allowed
                    IntBuffer buffer = h.getAt(vIndex);
                    int p = buffer.position();

                    if(buffer.get(p + j) == 0) {
                        isAllowedO = false;
                        break;
                    }
                }

                if(isAllowedO) {
                    allowed += log.getVariantFrequency(vIndex);

                    //check if a is reflected
                    if (!logState.checkForOutgoingAct(a)) {
                        //a is not reflected, hence escaping
                        escapingEdges += log.getVariantFrequency(vIndex);
                    }
                }
            }
        }
        //System.out.println("EE(" + a +") = " + escapingEdges);
        return new int[]{escapingEdges, allowed};
    }




}
