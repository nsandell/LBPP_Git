package bn.dynamic;

import bn.BNException;
import bn.IBayesNode;
import bn.distributions.Distribution;


/**
 * The interface to a node in a Dynamic Bayesian Network
 * @author Nils F. Sandell
 */
public interface IDynNode extends IBayesNode
{
	/**
	 * Get an iterator over children in the next slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<? extends IDynNode> getInterChildren();
	
	/**
	 * Get an iterator over children in the same slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<? extends IDynNode> getIntraChildren();
	
	/**
	 * Get an iterator over parents in the previous slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<? extends IDynNode> getInterParents();
	
	/**
	 * Get an iterator over parents in the same slice.
	 * @return Iterator over those nodes.
	 */	
	Iterable<? extends IDynNode> getIntraParents();
	
	boolean hasInterChild(IDynNode child);
	boolean hasIntraChild(IDynNode child);
	boolean hasInterParent(IDynNode child);
	boolean hasIntraParent(IDynNode child);
	
	public double betheFreeEnergy() throws BNException;
	
	public void setInitialDistribution(Distribution dist) throws BNException;
	public void setAdvanceDistribution(Distribution dist) throws BNException;
	public Distribution getAdvanceDistribution();
	public Distribution getInitialDistribution();
}
