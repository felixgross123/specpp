package org.processmining.estminer.specpp.evaluation.implicitness;

import org.processmining.estminer.specpp.datastructures.encoding.BitEncodedSet;
import org.processmining.estminer.specpp.datastructures.encoding.MutatingSetOperations;
import org.processmining.estminer.specpp.datastructures.log.impls.DenseVariantMarkingHistories;
import org.processmining.estminer.specpp.datastructures.petri.Place;
import org.processmining.estminer.specpp.datastructures.petri.Transition;
import org.processmining.estminer.specpp.datastructures.util.Pair;

import java.util.Map;

public class ReplayBasedImplicitnessCalculator {


    private static Place computeP3(Place p1, Place p2) {
        Pair<BitEncodedSet<Transition>> preset_diffs = MutatingSetOperations.dualSetminus(p1.preset(), p2.preset());
        Pair<BitEncodedSet<Transition>> postset_diffs = MutatingSetOperations.dualSetminus(p1.postset(), p2.postset());

        BitEncodedSet<Transition> iip = preset_diffs.first();
        BitEncodedSet<Transition> opo = postset_diffs.second();
        BitEncodedSet<Transition> oop = postset_diffs.first();
        BitEncodedSet<Transition> ipi = preset_diffs.second();

        boolean isFeasible = !iip.intersects(opo) && !oop.intersects(ipi);

        if (isFeasible) {
            iip.union(opo);
            oop.union(ipi);
            return new Place(iip, oop);
        } else return null;
    }

    public static ImplicitnessRating replaySubregionImplicitness(Place place, DenseVariantMarkingHistories placeHistory, Map<Place, DenseVariantMarkingHistories> histories) {
        for (Map.Entry<Place, DenseVariantMarkingHistories> entry : histories.entrySet()) {
            DenseVariantMarkingHistories h = entry.getValue();
            if (placeHistory.gt(h)) {
                // the examined place is a log replay subregion of an existing place
                Place p3 = computeP3(place, entry.getKey());
                if (p3 == null) return new ReplacementPlaceInfeasible();
                else return new ReplaceExaminedPlace(place, entry.getKey(), p3);
            } else if (placeHistory.lt(h)) {
                // an existing place is a log replay subregion of the examined place
                Place p3 = computeP3(entry.getKey(), place);
                if (p3 == null) return new ReplacementPlaceInfeasible();
                else return new ReplaceExistingPlace(place, entry.getKey(), p3);
            }
        }
        return BooleanImplicitness.NOT_IMPLICIT;
    }

}
