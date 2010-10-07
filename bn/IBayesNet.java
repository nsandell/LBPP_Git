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
	 * Get the node in this network with specified name.
	 * @param name Name of the node desired.
	 * @return The node, null if no node exists with provided name.
	 */
	public BaseInterface getNode(String name);
	
	/**
	 * Sample all nodes in this network.  The nodes will be set as observed and the values
	 * will be stored inside the nodes themselves.
	 * @throws BNException If a node can't be sampled for any reason.
	 */
	public void sample() throws BNException;
	
	/**
	 * Clear all the observations set in this network.  Destroys the values the nodes hold.
	 */
	public void clearAllEvidence();
	
	/**
	 * Collect a set of sufficient statistics for a specified set of nodes, aggregating them
	 * with any statistics that have been provided in the hashmap.
	 * @param nodeNames Iterable over the nodes we wish to collect sufficient statistics for.
	 * @param stats A HashMap of sufficient statistics previously collected (empty if none have been.)
	 * @throws BNException 
	 */
	public void collectSufficientStatistics(Iterable<String> nodeNames, HashMap<String,SufficientStatistic> stats) throws BNException;
	
	/**
	 * Optimize a specified subset of nodes with sufficient statistic objects provided.
	 * @param nodenames Nodes whose CPD parameters should be optimized.
	 * @param stats HashMap containing the sufficient statistic objects that should be used for optimization.
	 * @throws BNException
	 */
	public void optimize(Iterable<String> nodenames, HashMap<String,SufficientStatistic> stats) throws BNException; // Update the parameters of 'this' network using the stats in the hashmap
	
	/**
	 * This method first collects sufficient statistics for all nodes, and then optimizes
	 * the node CPD parameters based on those nodes.
	 */
	public void optimize(); 
	
	/**
	 * Validate that this Bayesian Network is properly formed.  An exception would be thrown if,
	 * for instance, a node has an incorrect number of parents for the CPD it has been given.
	 * @throws BNException If the network is improperly formed.
	 */
	public void validate() throws BNException;
	
	/**
	 * Get the log likelihood of an observed node given all the evidence 'above' it in the graph.
	 * @param nodeName Node to get the log likelihood of.
	 * @return The log likelihood.
	 * @throws BNException If the node isn't observed or doesn't exist.
	 */
	public double nodeLogLikelihood(String nodeName) throws BNException;
	
	/**
	 * Run belief propagation on this network.
	 * @param maxit The maximum number of iterations to be performed, regardless of error.
	 * @param convergence The convergence criteria - all distributions must change between 
	 * 			consecutive iterations less than this to be converged.  The change metric
	 * 			may vary by node but for discrete nodes it is the L-Infinity norm.
	 * @throws BNException Shouldn't be thrown unless the network is invalid.
	 */
	public void run(int maxit, double convergence) throws BNException;
	
	/**
	 * Clear evidence for a specific node.
	 * @param node Node to clear evidence for.
	 * @throws BNException If the node doesn't exist.
	 */
	public void clearEvidence(String node) throws BNException;
}
