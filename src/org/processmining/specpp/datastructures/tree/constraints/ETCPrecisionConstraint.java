package org.processmining.specpp.datastructures.tree.constraints;

import org.processmining.specpp.base.impls.CandidateConstraint;
import org.processmining.specpp.datastructures.petri.Place;

public class ETCPrecisionConstraint extends CandidateConstraint<Place> {

    public ETCPrecisionConstraint(Place affectedPlace) {
        super(affectedPlace);
    }

    @Override
    public String toString() {
        return "ETCPrecisionConstrainedPlace(" + getAffectedCandidate() + ")";
    }
}
