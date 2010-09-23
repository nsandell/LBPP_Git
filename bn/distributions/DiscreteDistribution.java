package bn.distributions;

import bn.BNException;

public abstract class DiscreteDistribution {

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
	
	
	public static class DiscreteDistributionBuilder {
		
		public DiscreteDistributionBuilder(String type, int cardinality, int numconditions, int[] dimensions) throws BNException
		{
			switch(DiscreteDistributionType.valueOf(type))
			{
			case UnconditionedDiscrete:
				this.inner = new DiscreteCPTUC(cardinality);
				break;
			case CPT:
				this.inner = new DiscreteCPT(cardinality,numconditions,dimensions);
				break;
			case SparseCPT:
				this.inner = new SparseDiscreteCPT(cardinality,numconditions,dimensions);
				break;
			case NoisyOr:
				this.inner = new NoisyOr(numconditions);
				break;
			default:
				throw new BNException("Unrecognized discrete probability distribution type " + type);
			}
		}
		
		public boolean addLine(String line) throws BNException
		{
			return this.inner.addLine(line);
		}
		
		public DiscreteDistribution getFinished() throws BNException
		{
			return this.inner.finish();
		}
		
		private DiscreteDistribution inner;
	}
	
	protected abstract boolean addLine(String line) throws BNException;
	protected abstract DiscreteDistribution finish() throws BNException;
	
	public static DiscreteDistributionBuilder getDistributionBuilder(String type, int cardinality, int numconditions, int[] dimensions) throws BNException
	{
		return new DiscreteDistributionBuilder(type, cardinality, numconditions, dimensions);
	}

	public static enum DiscreteDistributionType
	{
		UnconditionedDiscrete,
		CPT,
		SparseCPT,
		NoisyOr
	}

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
