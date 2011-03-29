package bn.impl;

import java.io.PrintStream;
import bn.BNException;
import bn.distributions.Distribution.SufficientStatistic;

//TODO figure out encapsulation
public interface InternalIBayesNode
{
	/**
	 * Update the belief propagation messages of this node based on the current
	 * messages inbound from neighbors, etc.
	 * @return The amount this node's CPD changed over the update.
	 * @throws BNException
	 */
	double updateMessages() throws BNException;
	
	/**
	 * Print information about the distribution to a stream
	 * @throws BNException If distribution hasn't been set
	 */
	void printDistributionInfo(PrintStream ps) throws BNException;
	
	public String getName();
	
	public void validate() throws BNException;
	
	public void resetMessages();
	
	public Iterable<? extends InternalIBayesNode> getChildrenI();
	
	public Iterable<? extends InternalIBayesNode> getParentsI();
	
	public Iterable<? extends InternalIBayesNode> getNeighborsI();
	
	public double betheFreeEnergy() throws BNException;
	
	public SufficientStatistic getSufficientStatistic() throws BNException;
	public double optimizeParameters(SufficientStatistic stat) throws BNException;
	public double optimizeParameters() throws BNException;
	
	public String getNodeDefinition();
	public String getEdgeDefinition();
	
	public void sample();
	public void setSample(boolean sample);
	
	/**
	 * Clear the observation this node holds.
	 */
	void clearEvidence();
}
