package bn.distributions;

import bn.BNException;

public abstract class DiscreteDistribution extends Distribution {

	protected DiscreteDistribution(int numConditions, int cardinality)
	{
		this.numConditions = numConditions;
		this.cardinality = cardinality;
	}
	
	public int getCardinality()
	{
		return this.cardinality;
	}

	public int getNumConditions()
	{
		return numConditions;
	}
	
	public abstract int[] getConditionDimensions();
	public abstract double evaluate(int[] indices, int value) throws BNException;
	

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
	
	public int[] initialIndices()
	{
		int[] indices = new int[this.numConditions];
		for(int i= 0; i < this.numConditions; i++)
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

	private int cardinality;
	private int numConditions;
}
