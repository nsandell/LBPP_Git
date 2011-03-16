package bn.dynamic;

import bn.BNException;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.messages.FiniteDiscreteMessage;

/**
 * Interface for a discrete node in a Dynamic Bayesian Network.
 * @author Nils F. Sandell
 * 
 */
public interface IFDiscDBNNode extends IDBNNode {
	
	/**
	 * Set the distribution for the first 'slice' of the network.  This is
	 * necessary where the node has inter-slice parents that don't exist
	 * in the first slice.  Overwrites the 'advance' distribution for the
	 * first slice if one has already been set.
	 * @param dist The distribution desired.
	 * @throws BNException If the distribution is of improper cardinality for this node.
	 */
	public void setInitialDistribution(DiscreteFiniteDistribution dist) throws BNException;
	
	/**
	 * Set the distribution for all slices of the network, excepting the first if 
	 * a distribution for the first has already been set.  In this way, nodes without
	 * inter-slice parents will take this distribution as its distribution for all time.
	 * @param dist The distribution desired.
	 * @throws BNException If the distribution is of improper cardinality for this node.
	 */
	public void setAdvanceDistribution(DiscreteFiniteDistribution dist) throws BNException;
	
	public DiscreteFiniteDistribution getInitialDistribution();
	public DiscreteFiniteDistribution getAdvanceDistribution();
	
	/**
	 * Get the marginal for this node at time slice 't'.  Initially set to uniform, this
	 * marginal only reflects the state of the node after the last round of belief propagation.
	 * @param t The time slice to get the marginal at.
	 * @return The marginal for this node.
	 * @throws BNException If t is out of range.
	 */
	public FiniteDiscreteMessage getMarginal(int t) throws BNException;
	
	/**
	 * Set the value of this node at time slice 't'.
	 * @param t The time slice to set the node value at.
	 * @param value The value to set the node to.
	 * @throws BNException If either argument is out of bounds.
	 */
	public void setValue(int t, int value) throws BNException;
	
	/**
	 * Set a consecutive set of values for this node, beginning at
	 * at a specified time and ending where the length of the values
	 * expires.
	 * @param values The consecutive set of values to specify.
	 * @param t0 The start time for the values to be set at.
	 * @throws BNException If t0 or t0+length of values is out of bounds,
	 * 		or if any of the values are out of bounds.
	 */
	public void setValue(int[] values,int t0) throws BNException;
	
	/**
	 * Get the value of this node at time slice 't'.
	 * @param t Time to get the node value at.
	 * @return Value of the node.
	 * @throws BNException If t is out of bounds, or if no value
	 * 	has been set at time t.
	 */
	public Integer getValue(int t) throws BNException;

	/**
	 * Get the cardinality of this node.
	 * @return The cardinality.
	 */
	int getCardinality();
}
