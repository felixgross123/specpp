package org.processmining.specpp.datastructures.tree.heuristic;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.processmining.specpp.base.Candidate;
import org.processmining.specpp.base.Evaluable;
import org.processmining.specpp.componenting.delegators.ContainerUtils;
import org.processmining.specpp.componenting.delegators.DelegatingObservable;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.composition.events.CandidateAcceptanceRevoked;
import org.processmining.specpp.composition.events.CandidateAccepted;
import org.processmining.specpp.composition.events.CandidateCompositionEvent;
import org.processmining.specpp.composition.events.CandidateRejected;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.base.LocalNode;
import org.processmining.specpp.datastructures.tree.base.NodeProperties;
import org.processmining.specpp.datastructures.tree.base.TreeNode;
import org.processmining.specpp.datastructures.tree.base.traits.LocallyExpandable;
import org.processmining.specpp.supervision.piping.Observer;
import org.processmining.specpp.util.JavaTypingUtils;

public class UpdatableTreeExpansion<C extends Candidate & NodeProperties, N extends LocalNode<C, ?, N>, H extends HeuristicValue<? super H>> extends HeuristicTreeExpansion<N, H> implements Observer<CandidateCompositionEvent<C>> {

    public UpdatableTreeExpansion(HeuristicStrategy<? super N, H> heuristicStrategy) {
        super(heuristicStrategy);
        globalComponentSystem().require(SupervisionRequirements.observable("composer.events", JavaTypingUtils.castClass(CandidateCompositionEvent.class)), ContainerUtils.observeResults(this));
    }

    protected final BidiMap<C, N> connection = new DualHashBidiMap<>();

    @Override
    protected void addNode(N node, H heuristic) {
        super.addNode(node, heuristic);
        connection.put(node.getProperties(), node);
    }

    @Override
    protected void removeNode(N node) {
        super.removeNode(node);
        connection.remove(node.getProperties());
    }

    @Override
    public void observe(CandidateCompositionEvent<C> observation) {
        C candidate = observation.getCandidate();
        N corresponding = connection.get(candidate);
        switch (observation.getAction()) {
            case Accept:

                H previousValue = nodeHeuristics.get(corresponding);

                H h = getHeuristicStrategy().computeHeuristic(corresponding);

                // determine nodes to update
                break;
            case RevokeAcceptance:
                // determine nodes to update
                break;
            case Reject:
                break;
        }

        //updateNode(node, score);
        // or
        //registerNode(node); // existing nodes are just readded
    }
}
