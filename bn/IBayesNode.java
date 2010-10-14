package bn;

import java.io.PrintStream;

import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;

/**
 * Basic Bayesian node interface.  Methods held by static and dynamic nodes
 * @author Nils F. Sandell
 * 
 */
public interface IBayesNode
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
	Iterable<IBayesNode> getChildren();
	
	/**
	 * Get an iterator over all the parents of this node. This is the intra-parents 
	 * for a dynamic node.
	 * @return 
	 */
	Iterable<IBayesNode> getParents();
	
	/**
	 * Validate the well-formedness of this node.
	 * @throws BNException If the node is not well formed.
	 */
	void validate() throws BNException;
	
	/**
	 * Clear the observation this node holds.
	 */
	void clearEvidence();
	
	/**
	 * Update the belief propagation messages of this node based on the current
	 * messages inbound from neighbors, etc.
	 * @return The amount this node's CPD changed over the update.
	 * @throws BNException
	 */
	double updateMessages() throws BNException;
	
	/**
	 * Optimize the parameters of this node.  This will collect the sufficient
	 * statistics given the current evidence in the network, and then optimize
	 * the parameters accordingly.
	 * @return The change of the node in optimizing.
	 * @throws BNException
	 */
	double optimizeParameters() throws BNException;
	
	/**
	 * Print information about the distribution to a stream
	 * @throws BNException If distribution hasn't been set
	 */
	void printDistributionInfo(PrintStream ps) throws BNException;
	
	/**
	 * Optimize the parameters of this node according to a sufficient statistic
	 * passed in as an argument.
	 * @param stat The sufficient statistic to optimize according to.
	 * @return The change of the node in optimizing.
	 * @throws BNException If the sufficient statistic does not line up with
	 * the expectations of the node.
	 */
	double optimizeParameters(SufficientStatistic stat) throws BNException;
	
	/**
	 * Get the log likelihood of this node given the evidence "above" it
	 * in the network.
	 * @return The log likelihood.
	 * @throws BNException If the graph is invalid or hasn't run message
	 * passing yet.
	 */
	double getLogLikelihood() throws BNException;
	
	/**
	 * Sample this node from its CPT.  This requires that this node's set of
	 * parents be observed and have a value.  Sampling this node will set its
	 * value and set it to be observed.
	 * @throws BNException If the parents aren't observed. 
	 */
	void sample() throws BNException;
	
	/**
	 * Get a sufficient statistic about this node given the evidence currently 
	 * present in the network.  
	 * @return The sufficient statistic.
	 * @throws BNException If the network isn't valid in the region of this node.
	 */
	public SufficientStatistic getSufficientStatistic() throws BNException;
	
	/**
	 * Update a sufficient statistic object given the evidence present in the
	 * network.  
	 * @param stat The sufficient statistic object to update.
	 * @throws BNException If the sufficient statistic object is incorrect or
	 * 	if otherwise unable to extract current statistics.
	 */
	public void updateSufficientStatistic(SufficientStatistic stat) throws BNException;
	
	/**
	 * Get the distribution for this node.
	 * @return The distribution.
	 */
	public Distribution getDistribution();
}
