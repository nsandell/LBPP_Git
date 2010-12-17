package bn;

import bn.distributions.Distribution;


/**
 * The interface to a node in a Dynamic Bayesian Network
 * @author Nils F. Sandell
 */
public interface IDynBayesNode extends IBayesNode
{
	/**
	 * Get an iterator over children in the next slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<? extends IDynBayesNode> getInterChildren();
	
	/**
	 * Get an iterator over children in the same slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<? extends IDynBayesNode> getIntraChildren();
	
	/**
	 * Get an iterator over parents in the previous slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<? extends IDynBayesNode> getInterParents();
	
	/**
	 * Get an iterator over parents in the same slice.
	 * @return Iterator over those nodes.
	 */	
	Iterable<? extends IDynBayesNode> getIntraParents();
	
	boolean hasInterChild(IDynBayesNode child);
	boolean hasIntraChild(IDynBayesNode child);
	boolean hasInterParent(IDynBayesNode child);
	boolean hasIntraParent(IDynBayesNode child);
	
	public double betheFreeEnergy() throws BNException;
	
	/**
	 * Get the initial distribution for this node.
	 * @return Initial distribution, null if initial is the same as the rest of the distributions.
	 */
	Distribution getInitialDistribution();
}
