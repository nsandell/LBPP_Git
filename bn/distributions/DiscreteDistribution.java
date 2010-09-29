package bn.distributions;

import java.util.Vector;

import bn.BNException;
import bn.messages.DiscreteMessage;

public abstract class DiscreteDistribution extends Distribution {

	protected DiscreteDistribution(int cardinality)
	{
		this.cardinality = cardinality;
	}
	
	public final int getCardinality()
	{
		return this.cardinality;
	}

	public abstract double evaluate(int[] indices, int value) throws BNException;
	public abstract void validateConditionDimensions(int[] dimensions) throws BNException;

	protected final static int getIndex(int[] indices, int[] dimSizes) throws BNException
	{
		int cinc = 1;
		int index = 0;
		for(int i = 0; i < indices.length; i++)
		{
			if(indices[i] >= dimSizes[i])
				throw new BNException("Out of bounds indices set " + indexString(indices) + " size = " + indexString(dimSizes));
			index += indices[i]*cinc;
			cinc *= dimSizes[i];
		}
		return index;
	}
	
	public final static int[] initialIndices(int[] dimSizes)
	{
		int[] indices = new int[dimSizes.length];
		for(int i= 0; i < dimSizes.length; i++)
			indices[i] = 0;
		return indices;
	}
	
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
	
	protected final static String indexString(int[] indices)
	{
		String ret = "(" + indices[0];
		for(int i = 1; i < indices.length; i++)
			ret += ", " + indices[i];
		ret += ")";
		return ret;
	}
	
	public abstract double computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Vector<DiscreteMessage> parent_pis, Integer value) throws BNException;
	public abstract void computeLambdas(Vector<DiscreteMessage> lambdas_out, Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda, Integer value) throws BNException;

	private int cardinality;
}
