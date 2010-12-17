package bn;

import bn.distributions.Distribution;

/**
 * Basic Bayesian node interface.  Methods held by static and dynamic nodes
 * @author Nils F. Sandell
 * 
 */
public interface IBayesNode {
	
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
	 * Validate the well-formedness of this node.
	 * @throws BNException If the node is not well formed.
	 */
	void validate() throws BNException;
	
	/**
	 * Set the conditional distribution for use by a node.
	 * @param dist The distribution for the node.
	 * @throws BNException If the distribution is invalid for the node (e.g., discrete/continuous).
	 */
	public void setDistribution(Distribution dist) throws BNException;
}
