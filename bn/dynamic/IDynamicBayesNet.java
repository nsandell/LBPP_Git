package bn.dynamic;

import bn.BNException;

import bn.IBayesNet;
import bn.IBayesNode;
import bn.distributions.Distribution;

/**
 * Interface for a Dynamic Bayesian Network.
 * @author Nils F. Sandell
 */
public interface IDynamicBayesNet extends IBayesNet<IDBNNode>
{
	/**
	 * Add a dependency from a node in time slices t+1 to another node
	 * in time slices t.
	 * @param from Name of the child node.
	 * @param to Name of the parent node.
	 * @throws BNException If either node doesn't exist.  Or if the specified
	 *  	nodes are unable to have this relationship.
	 */
	void addInterEdge(String from, String to) throws BNException;
	
	/**
	 * Add a dependency from a node to another node within the same time slice
	 * @param from Name of the child node.
	 * @param to Name of the parent node.
	 * @throws BNException If either node doesn't exist.  Or if the specified
	 * 		nodes are unable to have this relationship.
	 */
	void addIntraEdge(String from, String to) throws BNException;
	
	/**
	 * Add a dependency from a node in time slices t+1 to another node
	 * in time slices t.
 	 * @param from The parent node.
	 * @param to The child node.
	 * @throws BNException If either node doesn't exist.  Or if the specified
	 *  	nodes are unable to have this relationship.
	 */	
	void addInterEdge(IBayesNode from, IBayesNode to) throws BNException;
	
	/**
	 * Add a dependency from a node to another node within the same time slice
	 * @param from The parent node.
	 * @param to The child node.
	 * @throws BNException If either node doesn't exist in this network.  Or if 
	 *		 the specified nodes are unable to have this relationship.
	 */
	void addIntraEdge(IBayesNode from, IBayesNode to) throws BNException;
	
	/**
	 * Remove a dependency from a node in time slices t+1 to another node
	 * in time slices t.
	 * @param from Name of the child node.
	 * @param to Name of the parent node.
	 * @throws BNException If either node doesn't exist, or aren't connected.
	 */
	void removeInterEdge(String from, String to) throws BNException;
	
	/**
	 * Remove a dependency from a node to another node within the same time slice
	 * @param from Name of the child node.
	 * @param to Name of the parent node.
	 * @throws BNException If either node doesn't exist, or aren't connected.
	 */
	void removeIntraEdge(String from, String to) throws BNException;

	/**
	 * Remove a dependency from a node in time slices t+1 to another node
	 * in time slices t.
 	 * @param from The parent node.
	 * @param to The child node.
	 * @throws BNException If either node doesn't exist, or aren't connected.
	 */	
	void removeInterEdge(IBayesNode from, IBayesNode to) throws BNException;
	
	/**
	 * Remove a dependency from a node to another node within the same time slice
	 * @param from The parent node.
	 * @param to The child node.
	 * @throws BNException If either node doesn't exist in this network, or aren't connected.
	 */
	void removeIntraEdge(IBayesNode from, IBayesNode to) throws BNException;

	/**
	 * Test whether an edge exists between a node at time slice t and another at t+1
	 * @param fromName Parent node
	 * @param toName Child node
	 * @throws BNException If either node doesn't exist
	 */
	boolean existsInterEdge(String fromName, String toName) throws BNException;

	/**
	 * Test whether an edge exists between a node within the same time slice 
	 * @param fromName Parent node
	 * @param toName Child node
	 * @throws BNException If either node doesn't exist
	 */
	boolean existsIntraEdge(String fromName, String toName) throws BNException;
	
	/**
	 * Add a discrete node to this network.
	 * @param name Name of the node to add.
	 * @param cardinality Cardinality of the discrete node.
	 * @return Interface to the node.
	 * @throws BNException If a node of that name already exists.
	 */
	IFDiscDBNNode addDiscreteNode(String name, int cardinality) throws BNException;
	
	IInfDiscEvDBNNode addDiscreteEvidenceNode(String name, int[] value) throws BNException;
	
	/**
	 * Get the number of time slices in this network
	 * @return The number of time slices in the network.
	 */
	int getT();
	
	/**
	 * Run belief propagation over the network in parallel.  The way in which this is done
	 * is that if there are N available cores, the dynamic network is split into N roughly
	 * equal sized regions (split by time slices) with the boundaries between the regions
	 * reserved until all the parallel threads finish, then the boundaries are done serially.
	 * This procedure alternates until convergence.  **IMPORTANT** IT IS NOT SAFE TO USE
	 * THE NETWORK OR ANY NODE OBJECT WHILE THE PARALLEL INFERENCE IS BEING RUN.
	 * @param maxint Maximum number of iterations to run.
	 * @param conv Convergence criterion.
	 * @param callback A callback object to invoke when convergence has been reached (or the
	 * 		maximum number of iterations has been reached.)
	 */
	public void run_parallel(int maxint, double conv, ParallelCallback callback);
	public void run_parallel(int maxint, double conv, Iterable<String> nodes, ParallelCallback callback) throws BNException;
	public void run_parallel(int maxint, double conv, String node, ParallelCallback callback) throws BNException;
	
	/**
	 * Run belief propagation over the network in parallel as in run_parallel, but this method
	 * will block until converged.
	 * @param maxit Maximum number of iterations.
	 * @param conv Convergence criterion.
	 * @return Results of the run.
	 * @throws BNException
	 */
	public RunResults run_parallel_block(int maxit, double conv) throws BNException;
	public RunResults run_parallel_block(int maxit, double conv, Iterable<String> nodes) throws BNException;
	public RunResults run_parallel_block(int maxit, double conv, String nodes) throws BNException;
	
	/**
	 * Perform expectation maximization with inference in parallel
	 * @param maxit Maximum number of EM iterations
	 * @param conv EM Convergence criterion
	 * @param infMaxIt Maximimum number of BP iterations
	 * @param infConv BP convergence criterion
	 * @return Results
	 * @throws BNException 
	 */
	public RunResults optimize_parallel(int maxit, double conv, int infMaxIt, double infConv) throws BNException;
	
	public void setInitialDistribution(String name, Distribution dist) throws BNException;
	public void setAdvanceDistribution(String name, Distribution dist) throws BNException;
	public Distribution getInitialDistribution(String name) throws BNException;
	public Distribution getAdvanceDistribution(String name) throws BNException;
	
	/**
	 * Callback interface for calling parallel inference.
	 * @author Nils F. Sandell
	 */
	public static interface ParallelCallback
	{
		/**
		 * This is called when the inference has completed
		 * @param net The network which has finished inference.
		 * @param numIts The number of iterations it took to converge
		 * @param err The final error when converged
		 * @param timeElapsed The time in seconds it took to converge.
		 */
		void callback(IDynamicBayesNet net, int numIts, double err, double timeElapsed);
		
		/**
		 * This is called if parallel inference encountered a problem.
		 * @param net The network which had a problem.
		 * @param error String reasoning for the error.
		 */
		void error(IDynamicBayesNet net, String error);
	}
}
