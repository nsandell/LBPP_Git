package bn;


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
	Iterable<IDynBayesNode> getInterChildren();
	
	/**
	 * Get an iterator over children in the same slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<IDynBayesNode> getIntraChildren();
	
	/**
	 * Get an iterator over parents in the previous slice.
	 * @return Iterator over those nodes.
	 */
	Iterable<IDynBayesNode> getInterParents();
	
	/**
	 * Get an iterator over parents in the same slice.
	 * @return Iterator over those nodes.
	 */	
	Iterable<IDynBayesNode> getIntraParents();
	
	/**
	 * Get the static instance node of this node at time t
	 * @param t Time of the node desired
	 * @return Static node.
	 */
	IBayesNode getInstance(int t);
	
	/**
	 * Get the log likelihood of all the observed instances of this node given
	 * the evidence "above them" in the network.
	 * @return The log likelihood.
	 */
	double getLogLikelihood() throws BNException;
}
