package org.processmining.estminer.specpp.datastructures.tree.base;

import org.processmining.estminer.specpp.datastructures.tree.base.traits.LocallyContractible;
import org.processmining.estminer.specpp.datastructures.tree.base.traits.LocallyExpandable;

public interface LocalTreeTraversalStrategy<N extends TreeNode & LocallyExpandable<N> & LocallyContractible<N>> extends ExpansionStrategy<N>, ContractionStrategy<N> {

}
