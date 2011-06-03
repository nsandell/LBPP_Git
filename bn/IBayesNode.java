package bn;

import bn.distributions.Distribution.SufficientStatistic;

/**
 * Basic Bayesian node interface.  Methods held by static and dynamic nodes
 * @author Nils F. Sandell
 */
public interface IBayesNode {
	
	/**
	 * Get a sufficient statistic object for this node's distribution.
	 * @return A sufficient statistic object, appropriate for this node's distribution type
	 * and matching its expected sufficient statistics.
	 * @throws BNException If error.
	 */
	public SufficientStatistic getSufficientStatistic() throws BNException;
	
	/**
	 * Optimize this node's distribution according to a sufficient statistic object.
	 * @param stat The sufficient statistic object to optimize based on.
	 * @return The maximum parameter change based on the optimization.
	 * @throws BNException Most likely if the statistic is wrong for this node's distribution
	 * type, else if the statistic is invalid in some way.
	 */
	public double optimizeParameters(SufficientStatistic stat) throws BNException;
	
	/**
	 * Optimize this node's distribution according to its expected sufficient statistics.
	 * @return The maximum parameter change based on the optimization.
	 * @throws BNException If an error occurs.
	 */
	public double optimizeParameters() throws BNException;
	
	/**
	 * Get the name of this node.
	 * @return Name in string form.
	 */
	String getName();
	
	/**
	 * Get the number of parents this node has.  For a dynamic node this is
	 * the number of intra-slice parents for internal reasons.
	 * @return The number of parents.
	 */
	int numParents();
	
	/**
	 * Get the number of children this node has.
	 * @return The number of children.
	 */
	int numChildren();
	
	/**
	 * Get an iterator over all the children of this node.  This is the intra-children
	 * for a dynamic node.
	 * @return
	 */
	Iterable<? extends IBayesNode> getChildren();
	
	/**
	 * Get an iterator over all the parents of this node. This is the intra-parents 
	 * for a dynamic node.
	 * @return 
	 */
	Iterable<? extends IBayesNode> getParents();
	
	/**
	 * Get the network this node belongs to.
	 * @return The network.
	 */
	IBayesNet<? extends IBayesNode> getNetwork();
	
	/**
	 * Validate the well-formedness of this node.
	 * @throws BNException If the node is not well formed.
	 */
	void validate() throws BNException;
	
	/**
	 * Lock this node's distribution parameters so that any optimization calls will 
	 * be ignored.  Useful for when certain node's paramters are "known" and global 
	 * optimization calls should be ignored.
	 */
	void lockParameters();
	
	/**
	 * Unlocks parameters so optimization calls are heeded.
	 */
	void unlockParameters();
	
	/**
	 * Determine whether or not this node's distribution parameters are locked.
	 * @return True of false, as appropriate.
	 */
	boolean isLocked();
}
