package org.processmining.estminer.specpp.datastructures.tree.base;

import org.processmining.estminer.specpp.base.Constrainable;

public interface ConstrainableChildGenerationLogic<P extends NodeProperties, S extends NodeState, N extends LocalNode<P, S, N>, L extends GenerationConstraint> extends ChildGenerationLogic<P, S, N>, Constrainable<L> {
}
