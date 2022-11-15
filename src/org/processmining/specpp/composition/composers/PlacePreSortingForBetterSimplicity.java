package org.processmining.specpp.composition.composers;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.base.AdvancedComposition;
import org.processmining.specpp.base.Result;
import org.processmining.specpp.base.impls.AbstractPostponingComposer;
import org.processmining.specpp.base.impls.CandidateConstraint;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.delegators.ContainerUtils;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.delegators.DelegatingObserver;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.componenting.system.link.ComposerComponent;
import org.processmining.specpp.datastructures.encoding.IntEncodings;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.log.Variant;
import org.processmining.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.constraints.AddWiredPlace;
import org.processmining.specpp.datastructures.tree.constraints.RemoveWiredPlace;
import org.processmining.specpp.datastructures.tree.nodegen.UnWiringMatrix;
import org.processmining.specpp.evaluation.heuristics.CandidateScore;
import org.processmining.specpp.proposal.ProposerSignal;
import org.processmining.specpp.util.JavaTypingUtils;

import java.util.*;

public class PlacePreSortingForBetterSimplicity<I extends AdvancedComposition<Place>, R extends Result> extends AbstractPostponingComposer<Place, I, R, CandidateConstraint<Place>> {

    protected final SortedSet<PlaceWrapper> collectedPlaces = new TreeSet<>();

    private final DelegatingDataSource<Log> logSource = new DelegatingDataSource<>();

    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

    protected int currentLevel;

    private HeuristicStrategy<Place, CandidateScore> heuristic;

    private final Map<Activity, Double> activityToAvgFirstOccurrenceIndex = new HashMap<>();
    private final Map<Activity, Integer> activityToFreqSum = new HashMap<>();

    public PlacePreSortingForBetterSimplicity(ComposerComponent<Place, I, R> childComposer) {
        super(childComposer);

        globalComponentSystem().require(DataRequirements.RAW_LOG, logSource)
                                .require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping);
    }



    @Override
    protected void initSelf() {
        computeAverageFirstOccurrenceIndices();
        currentLevel = 2;
    }

    @Override
    protected CandidateDecision deliberateCandidate(Place candidate) {
        if(candidate.size() > currentLevel){
            currentLevel = candidate.size();
            iteratePostponedCandidatesUntilNoChange();
        }
        return CandidateDecision.Postpone;
    }

    @Override
    protected boolean iteratePostponedCandidates() {
        for (PlaceWrapper p : collectedPlaces) {
            acceptCandidate(p.getPlace());
        }
        collectedPlaces.clear();
        return false;
    }


    @Override
    protected void postponeDecision(Place candidate) {
        collectedPlaces.add(new PlaceWrapper(candidate));
    }

    @Override
    protected void rejectCandidate(Place candidate) {

    }

    @Override
    protected void discardCandidate(Place candidate) {

    }

    @Override
    public Class<CandidateConstraint<Place>> getPublishedConstraintClass() {
        return JavaTypingUtils.castClass(CandidateConstraint.class);
    }

    public void computeAverageFirstOccurrenceIndices() {
        Log log = logSource.getData();


        for (IndexedVariant indexedVariant : log) {

            Set<Activity> seen = new HashSet<>();
            Variant variant = indexedVariant.getVariant();
            int variantFrequency = log.getVariantFrequency(indexedVariant.getIndex());

            int j = 0;
            for (Activity a : variant) {
                if (!seen.contains(a)) {
                    if(!activityToAvgFirstOccurrenceIndex.containsKey(a)) {
                        activityToAvgFirstOccurrenceIndex.put(a, Double.valueOf(j));
                        activityToFreqSum.put(a,variantFrequency);
                    } else {
                        int freqSumA = activityToFreqSum.get(a);

                        double newAvg = ((double)freqSumA / (double)(freqSumA + variantFrequency)) * activityToAvgFirstOccurrenceIndex.get(a) + ((double)variantFrequency / (double)(freqSumA + variantFrequency)) * j;
                        activityToAvgFirstOccurrenceIndex.put(a, newAvg);
                        activityToFreqSum.put(a,freqSumA + variantFrequency);
                    }
                }
                j++;
                seen.add(a);
            }
        }

    }
    
    
    private class PlaceWrapper implements Comparable {
        
        private Place p;
        private double simplicityScore;

        public PlaceWrapper(Place p) {
            this.p = p;
            simplicityScore = calculateScore(p);
        }
        
        private int calculateScore(Place p) {
            int avgIndexPreset = 0;
            for (Transition t : p.preset()) {
                Activity a = actTransMapping.getData().getKey(t);
                avgIndexPreset += activityToAvgFirstOccurrenceIndex.get(a);
            }
            avgIndexPreset /= p.preset().size();

            int avgIndexPostset = 0;
            for (Transition t : p.postset()) {
                Activity a = actTransMapping.getData().getKey(t);
                avgIndexPostset += activityToAvgFirstOccurrenceIndex.get(a);
            }
            avgIndexPostset /= p.postset().size();

            return avgIndexPostset - avgIndexPreset;
        }

        public double getSimplicityScore() {
            return simplicityScore;
        }

        public Place getPlace() {
            return p;
        }

        @Override
        public int compareTo(Object o) {
            double simplicityScoreOther = ((PlaceWrapper)o).getSimplicityScore();
            if(simplicityScoreOther < simplicityScore) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}
