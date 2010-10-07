package bn;

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
	 * Add edge from one node to another.
	 * @param from Child node
	 * @param to Parent node
	 * @throws BNException If the nodes can't be connected.
	 */
	public void addEdge(IBayesNode from, IBayesNode to) throws BNException;
	
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
	public void addDiscreteEvidence(String node, int obsv) throws BNException;
}
