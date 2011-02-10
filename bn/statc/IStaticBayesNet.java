package bn.statc;

import bn.BNException;
import bn.IBayesNet;
import bn.IBayesNode;
import bn.distributions.Distribution;

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
	public IFDiscBNNode addDiscreteNode(String name, int cardinality) throws BNException;
	
	/**
	 * Check whether an edge exists or not.
	 * @param from Parent node
	 * @param to Child node
	 * @return True if it does, else false
	 * @throws BNException If either of the nodes specified don't exist.
	 */
	public boolean edgeExists(String from, String to) throws BNException;
	
	public Distribution getDistribution(String name) throws BNException;
}
