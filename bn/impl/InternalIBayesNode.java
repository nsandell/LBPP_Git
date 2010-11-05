package bn.impl;

import java.io.PrintStream;

import bn.BNException;
import bn.IBayesNode;
import bn.distributions.Distribution.SufficientStatistic;

interface InternalIBayesNode extends IBayesNode
{
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
	
	public void resetMessages();
	
	public Iterable<? extends InternalIBayesNode> getChildrenI();
	
	public Iterable<? extends InternalIBayesNode> getParentsI();
	
	/**
	 * Clear the observation this node holds.
	 */
	void clearEvidence();
}
