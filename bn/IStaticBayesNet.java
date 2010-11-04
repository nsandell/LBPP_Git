package bn;

import bn.messages.Message;

/**
 * Interface for Static Bayes network-specific functions.
 * @author Nils F. Sandell
 *
 */
public interface IStaticBayesNet extends IBayesNet<IBayesNode>
{
	/**
	 * Add edge from one node to another.
	 * @param from Child node name
	 * @param to Parent node name
	 * @throws BNException If the nodes don't exist or can't be connected.
	 */
	public void addEdge(String from, String to) throws BNException;
	
	/**
	 * Remove an existing edge from one node to another.
	 * @param from Child node name
	 * @param to Parent node name
	 * @throws BNException If the nodes aren't connected, or they don't exist.
	 */
	public void removeEdge(String from, String to) throws BNException;
	
	/**
	 * Add a discrete node to this network.
	 * @param name Name of the node to add.
	 * @param cardinality Cardinality of the node to add.
	 * @return Interface to the new node.
	 * @throws BNException If the new node can't be created (e.g. same name already exists)
	 */
	public IDiscreteBayesNode addDiscreteNode(String name, int cardinality) throws BNException;
	
	/**
	 * Add evidence to a discrete node.
	 * @param node Name of the node to get evidence.
	 * @param obsv Observation to set the node.
	 * @throws BNException If the node isn't discrete or the observation is invalid.
	 */
	public void addEvidence(String node, Object obsv) throws BNException;
	
	/**
	 * Get the evidence from a node
	 * @param node Node to get evidence from.
	 * @return The evidence, as an object.
	 * @throws BNException If no node exists or the node has no evidence.
	 */
	public Object getEvidence(String node) throws BNException;
	
	/**
	 * Get the marginal for a node.
	 * @param nodename The name of the node.
	 * @return  The marginal for the node.
	 * @throws BNException If inference hasn't been run or there is otherwise no marginal.
	 */
	public Message getMarginal(String nodename) throws BNException;
	
	/**
	 * Check whether an edge exists or not.
	 * @param from Parent node
	 * @param to Child node
	 * @return True if it does, else false
	 * @throws BNException If either of the nodes specified don't exist.
	 */
	public boolean edgeExists(String from, String to) throws BNException;
}
