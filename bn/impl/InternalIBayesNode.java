package bn.impl;

import java.io.PrintStream;
import bn.BNException;

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
	
	public double betheFreeEnergy() throws BNException;
	
	public String getNodeDefinition();
	public String getEdgeDefinition();
	
	/**
	 * Clear the observation this node holds.
	 */
	void clearEvidence();
}
