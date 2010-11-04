package bn;

import bn.distributions.Distribution;
import bn.interfaces.Printable;

/**
 * Basic Bayesian node interface.  Methods held by static and dynamic nodes
 * @author Nils F. Sandell
 * 
 */
public interface IBayesNode extends Printable
{
	
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
	 * Get the log likelihood of this node given the evidence "above" it
	 * in the network.
	 * @return The log likelihood.
	 * @throws BNException If the graph is invalid or hasn't run message
	 * passing yet.
	 */
	double getLogLikelihood() throws BNException;
	
	/**
	 * Set the conditional distribution for use by a node.
	 * @param dist The distribution for the node.
	 * @throws BNException If the distribution is invalid for the node (e.g., discrete/continuous).
	 */
	public void setDistribution(Distribution dist) throws BNException;
}
