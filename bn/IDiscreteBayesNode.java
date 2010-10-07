package bn;

import bn.distributions.DiscreteDistribution;
import bn.messages.DiscreteMessage;

/**
 * Interface for a discrete node in a static Bayesian network
 * @author Nils F. Sandell
 * 
 */
public interface IDiscreteBayesNode extends IBayesNode
{
	/**
	 * Get the current integer value of this node.
	 * @return The current value of the node.
	 * @throws BNException If the node's value isn't specified. 
	 */
	int getValue() throws BNException;
	
	/**
	 * Get the cardinality of this node.
	 * @return Integer representing the cardinality of the variable for this node.
	 */
	int getCardinality();
	
	/**
	 * Set the discrete conditional distribution for this node.  Note that the
	 * CPD need not be set for the correct number of parents for this node.  To
	 * ensure the network is properly formed, a call to 'validate' should be used
	 * before running inference.
	 * @param dist The desired discrete CPD for this node.
	 * @throws BNException If the CPD is of incorrect cardinality for this node.
	 */
	void setDistribution(DiscreteDistribution dist) throws BNException;
	
	/**
	 * Set an observed value for this node.
	 * @param o The value desired.
	 * @throws BNException If the value is out of the correct range for this node.
	 */
	void setValue(int o) throws BNException;
	
	/**
	 * Clear the observed value for this node.  Does nothing if there is no value
	 * set for this node.
	 */
	void clearValue();
	
	/**
	 * Get the marginal distribution for this node given previous iterations of
	 * belief propagation.  Note that this will not reflect changes to topology
	 * or parameters without again running belief propagation.
	 * @return The marginal distribution. (will be uniform initially)
	 */
	DiscreteMessage getMarginal();
}
