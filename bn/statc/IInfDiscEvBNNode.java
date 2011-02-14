package bn.statc;

import bn.BNException;
import bn.IBayesNode;
import bn.distributions.DiscreteDistribution.InfiniteDiscreteDistribution;

public interface IInfDiscEvBNNode extends IBayesNode
{

	/**
	 * Get the current integer value of this node.
	 * @return The current value of the node.
	 * @throws BNException If the node's value isn't specified. 
	 */
	int getValue() throws BNException;
	
	/**
	 * Set the discrete conditional distribution for this node.  Note that the
	 * CPD need not be set for the correct number of parents for this node.  To
	 * ensure the network is properly formed, a call to 'validate' should be used
	 * before running inference.
	 * @param dist The desired discrete CPD for this node.
	 * @throws BNException If the CPD is of incorrect cardinality for this node.
	 */
	void setDistribution(InfiniteDiscreteDistribution dist) throws BNException;
	
	/**
	 * Set an observed value for this node.
	 * @param o The value desired.
	 * @throws BNException If the value is out of the correct range for this node.
	 */
	void setValue(int o) throws BNException;
}