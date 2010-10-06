package bn;

import java.util.HashMap;

import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
/**
 * Highest level bayesian network interface - functions that are in common both with 
 * static and dynamic bayesian networks.  Probably shouldn't be carried around by
 * the user but rather they should be using StaticBayesNet or DynBayesNet.
 * @author Nils F. Sandell
 * 
 * @param <BaseInterface> This determines what interface a general node in this network
 * 						  will use.
 */
public interface IBayesNet<BaseInterface>
{
	/**
	 * Remove a node corresonding to provided name from the network.  
	 * @param name Name of the node to be removed
	 * @throws BNException If the node doesn't exist or otherwise can't be removed.
	 */
	public void removeNode(String name) throws BNException;
	
	/**
	 * Remove specified node from the network.
	 * @param node The node to remove.
	 * @throws BNException If node can't be removed, or doesn't exist (or is null).
	 */
	public void removeNode(BaseInterface node) throws BNException;
	
	/**
	 * Set the conditional distribution for use by a node.
	 * @param nodeName The name of a node to use.
	 * @param dist The distribution for the node.
	 * @throws BNException If the distribution is invalid for the node (e.g., discrete/continuous).
	 */
	public void setDistribution(String nodeName, Distribution dist) throws BNException;
	
	/**
	 * Get the names of all the nodes in this network.
	 * @return An iterable over the names.
	 */
	public Iterable<String>	getNodeNames();
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public BaseInterface getNode(String name);
	
	public void sample() throws BNException;
	public void clearAllEvidence();
	
	public void collectSufficientStatistics(Iterable<String> nodeNames, HashMap<String,SufficientStatistic> stats) throws BNException;// Load stats for the nodes specified by the iterable
	public void optimize(Iterable<String> nodenames, HashMap<String,SufficientStatistic> stats) throws BNException; // Update the parameters of 'this' network using the stats in the hashmap
	public void optimize(); // Collect local sufficient statistics, update the graph
	
	public void validate() throws BNException;
	public double nodeLogLikelihood(String nodeName) throws BNException;
	public void run(int maxit, double convergence) throws BNException;
	public void clearEvidence(String node) throws BNException;
}
