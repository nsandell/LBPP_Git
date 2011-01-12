package bn;

import java.io.PrintStream;
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
public interface IBayesNet<BaseInterface> {
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
	
	public void resetMessages();
	
	public int numNodes();
	
	public double getLogLikelihood() throws BNException;
	
	public String getDefinition();
	
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
	public BaseInterface getNode(String name) throws BNException;
	
	/**
	 * Sample all nodes in this network.  The nodes will be set as observed and the values
	 * will be stored inside the nodes themselves.
	 * @throws BNException If a node can't be sampled for any reason.
	 */
	public void sample() throws BNException;
	
	public void sample(String name) throws BNException;
	
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
	 * Run expectation maximization on this network.
	 * @param learnIt Will perform at most this many EM iterations.
	 * @param learnErr Will terminate EM earlier if this err criteria is met.
	 * @param runIt In the inference part of EM, will run at most this many iterations of belief propagation.
	 * @param runErr This is the convergence criteria for the belief propagation part of EM
	 * @return Results of the procedure.
	 * @throws BNException
	 */
	public RunResults optimize(int learnIt, double learnErr, int runIt, double runErr) throws BNException;
	
	/**
	 * Validate that this Bayesian Network is properly formed.  An exception would be thrown if,
	 * for instance, a node has an incorrect number of parents for the CPD it has been given.
	 * @throws BNException If the network is improperly formed.
	 */
	public void validate() throws BNException;
	
	public static class RunResults
	{
		public RunResults(int numIts, double timeElapsed, double error)
		{
			this.numIts = numIts;
			this.timeElapsed = timeElapsed;
			this.error = error;
		}
		public int numIts;
		public double timeElapsed; 
		public double error;
	}
	
	
	/**
	 * Get the log likelihood of all of the evidence present in the network.
	 * @return The log likelihood, 0 if there is no evidence.
	 * @throws BNException if message passing hasn't been run yet.
	 */
	//public double logLikelihood() throws BNException;
	
	public void print();
	public void print(PrintStream ps);
	
	public void printDistributionInfo(String name, PrintStream ps) throws BNException;
	
	/**
	 * Run belief propagation on this network.
	 * @param maxit The maximum number of iterations to be performed, regardless of error.
	 * @param convergence The convergence criteria - all distributions must change between 
	 * 			consecutive iterations less than this to be converged.  The change metric
	 * 			may vary by node but for discrete nodes it is the L-Infinity norm.
	 * @return Results of the run
	 * @throws BNException Shouldn't be thrown unless the network is invalid.(you should call validate first!)
	 */
	public RunResults run(int maxit, double convergence) throws BNException;
	
	
	/**
	 * Run belief propagation on the network, using default parameters.
	 * @return Results of the run - how many iterations were used and what
	 * 		the final error turned out to be.
	 * @throws BNException Shouldn't be thrown unless the network is invalid (you should call validate first!)
	 */
	public RunResults run() throws BNException;
	
	/**
	 * Clear evidence for a specific node.
	 * @param node Node to clear evidence for.
	 * @throws BNException If the node doesn't exist.
	 */
	public void clearEvidence(String node) throws BNException;
}
