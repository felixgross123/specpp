package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.processmining.specpp.base.Candidate;
import org.processmining.specpp.componenting.delegators.ContainerUtils;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.composition.events.CandidateCompositionEvent;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.base.LocalNode;
import org.processmining.specpp.datastructures.tree.base.NodeProperties;
import org.processmining.specpp.datastructures.tree.heuristic.HeuristicTreeExpansion;
import org.processmining.specpp.datastructures.tree.heuristic.HeuristicValue;
import org.processmining.specpp.supervision.piping.Observer;
import org.processmining.specpp.util.JavaTypingUtils;

public class UpdateGreedyETCPrecisonTreeHeuristic<C extends Candidate & NodeProperties, N extends LocalNode<C, ?, N>, H extends HeuristicValue<? super H>> extends HeuristicTreeExpansion<N, H> implements Observer<CandidateCompositionEvent<C>> {


    public UpdateGreedyETCPrecisonTreeHeuristic(HeuristicStrategy<? super N, H> heuristicStrategy) {
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
                // determine nodes to update


                // n in super.priorityQueue

                    // if n.postset CAP corresponding.postset != empty

                    //updateNode(node, getHeuristicStrategy().computeHeuristic(corresponding))



                break;
            case RevokeAcceptance:
                break;
            case Reject:
                break;
        }
    }
}
