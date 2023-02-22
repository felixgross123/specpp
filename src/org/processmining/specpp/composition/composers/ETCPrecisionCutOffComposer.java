package org.processmining.specpp.composition.composers;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.base.ConstrainingComposer;
import org.processmining.specpp.base.Result;
import org.processmining.specpp.base.impls.CandidateConstraint;
import org.processmining.specpp.base.impls.FilteringComposer;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.data.StaticDataSource;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.delegators.DelegatingEvaluator;
import org.processmining.specpp.componenting.evaluation.EvaluationRequirements;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.componenting.system.link.ComposerComponent;
import org.processmining.specpp.componenting.system.link.CompositionComponent;
import org.processmining.specpp.config.parameters.ETCPrecisionThresholdRho;
import org.processmining.specpp.config.parameters.TauFitnessThresholds;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.tree.constraints.ClinicallyOverfedPlace;
import org.processmining.specpp.datastructures.tree.constraints.ClinicallyUnderfedPlace;
import org.processmining.specpp.datastructures.tree.constraints.ETCPrecisionConstraint;
import org.processmining.specpp.datastructures.util.BasicCache;
import org.processmining.specpp.evaluation.fitness.BasicFitnessEvaluation;
import org.processmining.specpp.evaluation.fitness.DetailedFitnessEvaluation;
import org.processmining.specpp.evaluation.fitness.FitnessThresholder;
import org.processmining.specpp.supervision.EventSupervision;
import org.processmining.specpp.supervision.piping.Observable;
import org.processmining.specpp.supervision.piping.PipeWorks;
import org.processmining.specpp.util.JavaTypingUtils;

import java.util.HashMap;
import java.util.Map;

import static org.processmining.specpp.componenting.data.DataRequirements.dataSource;

public class ETCPrecisionCutOffComposer<I extends CompositionComponent<Place>, R extends Result> extends FilteringComposer<Place, I, R> implements ConstrainingComposer<Place, I, R, CandidateConstraint<Place>> {

    protected final DelegatingDataSource<ETCPrecisionThresholdRho> rho = new DelegatingDataSource<>();
    protected final EventSupervision<CandidateConstraint<Place>> constraintEvents = PipeWorks.eventSupervision();
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

    private final DelegatingDataSource<Map<Activity, Integer>> activitiesToAllowed = new DelegatingDataSource<>();
    private final DelegatingDataSource<Map<Activity, Integer>> activitiesToEscapingEdges = new DelegatingDataSource<>();

    public ETCPrecisionCutOffComposer(ComposerComponent<Place, I, R> childComposer) {
        super(childComposer);
        globalComponentSystem().require(ParameterRequirements.PRECISION_TRHESHOLD_RHO, rho)
                                .require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                .require(dataSource("activitiesToAllowed", JavaTypingUtils.castClass(Map.class)), activitiesToAllowed)
                .require(dataSource("activitiesToEscapingEdges", JavaTypingUtils.castClass(Map.class)), activitiesToEscapingEdges)
                                .provide(SupervisionRequirements.observable("composer.constraints.ETCCutOff", getPublishedConstraintClass(), getConstraintPublisher()));
        localComponentSystem().provide(SupervisionRequirements.observable("composer.constraints.ETCCutOff", getPublishedConstraintClass(), getConstraintPublisher()));
    }

    @Override
    protected void initSelf() {

    }

    @Override
    public void accept(Place place) {

        if(activitiesToAllowed.getData().size()>0 && activitiesToEscapingEdges.getData().size()>0) {
            int sumAllowed = 0;
            int sumEscaping = 0;
            for(Transition t : place.postset()) {
                Activity a = actTransMapping.getData().getKey(t);
                sumAllowed += activitiesToAllowed.getData().get(a);
                sumEscaping += activitiesToEscapingEdges.getData().get(a);
            }
            double partPrecision = 1.0 - ((double) sumEscaping /(double) sumAllowed);


            if(partPrecision >= rho.getData().getP()) {
                constraintEvents.observe(new ETCPrecisionConstraint(place));
                gotFiltered(place);
            }else {
                forward(place);
            }
        }else {
            forward(place);
        }

    }

    protected void gotFiltered(Place place) {
    }

    @Override
    public Observable<CandidateConstraint<Place>> getConstraintPublisher() {
        return constraintEvents;
    }

    @Override
    public Class<CandidateConstraint<Place>> getPublishedConstraintClass() {
        return JavaTypingUtils.castClass(CandidateConstraint.class);
    }

}
