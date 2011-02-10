package bn.distributions;

import bn.BNException;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;

/**
 * Abstract superclass for discrete distributions.
 * @author Nils F. Sandell
 */
public abstract class DiscreteDistribution implements Distribution {
	
	public static abstract class DiscreteFiniteDistribution extends DiscreteDistribution
	{
		public DiscreteFiniteDistribution(int cardinality){this.cardinality = cardinality;}
		
		@Override
		public void validateDimensionality(int[] dimensions, int cardinality) throws BNException
		{
			if(this.cardinality!=cardinality)
				throw new BNException("Failure to validate - finite discrete distribution has incorrect dimensionality for the support variable!");
			this.validateConditionDimensions(dimensions);
		}
		protected abstract void validateConditionDimensions(int[] dimensions) throws BNException;
		
		@Override
		public abstract DiscreteFiniteDistribution copy() throws BNException;
		
		public int getCardinality(){return this.cardinality;}
		private int cardinality;
	}

	/**
	 * Only constructor - all discrete distributions must be over some set cardinality.
	 * @param cardinality The cardinality this distribution is over.
	 */
	protected DiscreteDistribution(){}
	
	/**
	 * Sample this distribution given a set of parent values.
	 * @param parentVals Parent values
	 * @return An appropriate sample from this distribution.
	 * @throws BNException If the parentvals are bad.
	 */
	public abstract int sample(ValueSet<Integer> parentVals) throws BNException;
	
	@Override
	public abstract DiscreteSufficientStatistic getSufficientStatisticObj();
	
	@Override
	public abstract DiscreteDistribution copy() throws BNException;

	/**
	 * Evaluate the probability of a given value of the variable of interested
	 * given the set of parents.
	 * @param indices Indices of parents
	 * @param value Value of the variable of interest
	 * @return Probability of the variable given parents
	 * @throws BNException If the parent indices are bad.
	 */
	public abstract double evaluate(int[] indices, int value) throws BNException;
	
	/**
	 * Validate that this distribution can handle a set of parents with the dimensions
	 * provided.
	 * @param dimensions Dimensions of the conditioning variables.
	 * @param cardinality Dimension of the support node.
	 * @throws BNException If this node cannot handle the given dimension vector.
	 */
	public abstract void validateDimensionality(int[] dimensions, int cardinality) throws BNException;

	/**
	 * Static helper function, converts a set of conditioning variable values into
	 * a product index.
	 * @param indices Set of conditioning variable values
	 * @param dimSizes The dimensions of the conditioning variables.
	 * @return A product index.
	 * @throws BNException If the indices are invalid given the dimensions
	 */
	public final static int getIndex(int[] indices, int[] dimSizes) throws BNException
	{
		int cinc = 1;
		int index = 0;
		for(int i = 0; i < indices.length; i++)
		{
			if(indices[i] >= dimSizes[i] || indices[i] < 0)
				throw new BNException("Out of bounds indices set " + indexString(indices) + " size = " + indexString(dimSizes));
			index += indices[i]*cinc;
			cinc *= dimSizes[i];
		}
		return index;
	}
	
	/**
	 * Get a set of 0 indices for a set of conditioning variable sizes.
	 * @param dimSizes Number of conditioning variables
	 * @return An int[] of zeros of appropriately size..
	 */
	public final static int[] initialIndices(int numdims)
	{
		int[] indices = new int[numdims];
		for(int i= 0; i < numdims; i++)
			indices[i] = 0;
		return indices;
	}
	
	/**
	 * Increment a set of values for conditioning variables.
	 * @param indices Set of values for conditioning variables.
	 * @param dimSizes The sizes of the conditioning variables.
	 * @return The 'next' index set, null if none.
	 */
	public final static int[] incrementIndices(int[] indices, int[] dimSizes)
	{
		for(int i = 0; i < indices.length; i++)
		{
			if(indices[i]==(dimSizes[i]-1))
				indices[i] = 0;
			else
			{
				indices[i]++;
				return indices;
			}
		}
		return null;
	}
	
	/**
	 * Get a string representing a set of indices.
	 * @param indices Indices to get a string for
	 * @return The string.
	 */
	public final static String indexString(int[] indices)
	{
		String ret = "(" + indices[0];
		for(int i = 1; i < indices.length; i++)
			ret += ", " + indices[i];
		ret += ")";
		return ret;
	}
	
	/**
	 * Compute a local pi message for a node with this CPD as well as node likelhood 
	 * (if observed) given incoming pi messages, the local pi  messages of parents, 
	 * and the observed value (if observed).
	 * @param local_pi Local pi object to take result
	 * @param incoming_pis Pi messages from parents.
	 * @param value Value of the node
	 * @throws BNException 
	 */
	public abstract void computeLocalPi(FiniteDiscreteMessage local_pi, MessageSet<FiniteDiscreteMessage> incoming_pis, 
										  Integer value) throws BNException;
	
	/**
	 * Compute outgoing lambda messages for a node with this as its CPD given the incoming pi messages, and
	 * the local lambda message, and (optionally) the value of the node.
	 * @param lambdas_out Message vector for storing the output outgoing lamdba messages
	 * @param incoming_pis Pi messages from parents.
	 * @param local_lambda Local lambda message
	 * @param value Value of node if observed, else null
	 * @throws BNException
	 */
	public abstract void computeLambdas(MessageSet<FiniteDiscreteMessage> lambdas_out, MessageSet<FiniteDiscreteMessage> incoming_pis,
										FiniteDiscreteMessage local_lambda, Integer value) throws BNException;
	
	public abstract double computeBethePotential(MessageSet<FiniteDiscreteMessage> incoming_pis,
									FiniteDiscreteMessage local_lambda, FiniteDiscreteMessage marginal, Integer value, int numChildren) throws BNException;
	
	private static final long serialVersionUID = 50L;
}
