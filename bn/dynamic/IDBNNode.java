package bn.dynamic;

import bn.BNException;
import bn.IBayesNode;
import bn.distributions.Distribution;


/**
 * The interface to a node in a Dynamic Bayesian Network
 * @author Nils F. Sandell
 */
public interface IDBNNode extends IBayesNode
{
	/**
	 * Get an iterator over children in the next slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<? extends IDBNNode> getInterChildren();
	
	/**
	 * Get an iterator over children in the same slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<? extends IDBNNode> getIntraChildren();
	
	/**
	 * Get an iterator over parents in the previous slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<? extends IDBNNode> getInterParents();
	
	/**
	 * Get an iterator over parents in the same slice.
	 * @return Iterator over those nodes.
	 */	
	Iterable<? extends IDBNNode> getIntraParents();
	
	boolean hasInterChild(IDBNNode child);
	boolean hasIntraChild(IDBNNode child);
	boolean hasInterParent(IDBNNode child);
	boolean hasIntraParent(IDBNNode child);
	
	public double conditionalLL(int t);
	public double betheFreeEnergy() throws BNException;
	
	public void setInitialDistribution(Distribution dist) throws BNException;
	public void setAdvanceDistribution(Distribution dist) throws BNException;
	public Distribution getAdvanceDistribution();
	public Distribution getInitialDistribution();
}
