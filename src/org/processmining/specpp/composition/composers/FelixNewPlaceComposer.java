package org.processmining.specpp.composition.composers;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.base.AdvancedComposition;
import org.processmining.specpp.base.impls.AbstractComposer;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.delegators.DelegatingEvaluator;
import org.processmining.specpp.componenting.evaluation.EvaluationRequirements;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.petri.CollectionOfPlaces;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.vectorization.VariantMarkingHistories;
import org.processmining.specpp.evaluation.implicitness.ImplicitnessRating;
import org.processmining.specpp.supervision.EventSupervision;
import org.processmining.specpp.supervision.observations.DebugEvent;
import org.processmining.specpp.supervision.piping.PipeWorks;

public class FelixNewPlaceComposer<I extends AdvancedComposition<Place>> extends AbstractComposer<Place, I, CollectionOfPlaces> {

    private final DelegatingDataSource<Log> logSource = new DelegatingDataSource<>();
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();
    private final DelegatingEvaluator<Place, ImplicitnessRating> implicitnessEvaluator = new DelegatingEvaluator<>();
    private final DelegatingEvaluator<Place, VariantMarkingHistories> markingHistoriesEvaluator = new DelegatingEvaluator<>();
    private final EventSupervision<DebugEvent> eventSupervisor = PipeWorks.eventSupervision();

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
        VariantMarkingHistories variantMarkingHistories = markingHistoriesEvaluator.eval(candidate);
        ImplicitnessRating implicitnessRating = implicitnessEvaluator.eval(candidate);
        eventSupervisor.observe(new DebugEvent("read me"));
        return false;
    }

    @Override
    protected void acceptanceRevoked(Place candidate) {

    }

    @Override
    protected void candidateAccepted(Place candidate) {

    }

    @Override
    protected void candidateRejected(Place candidate) {

    }

    @Override
    public void candidatesAreExhausted() {

    }


    @Override
    protected void initSelf() {
        initSelf();

    }

    //Test
}
