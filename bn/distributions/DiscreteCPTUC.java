package bn.distributions;

import bn.BNException;

public class DiscreteCPTUC extends DiscreteDistribution
{
	public DiscreteCPTUC(double[] distr) throws BNException
	{
		super(0,distr.length);
		this.delta = false;
		this.dist = distr;
		this.index = -1;
		this.validate();
	}

	private final void validate() throws BNException
	{
		double sum = 0;
		for(int i = 0; i < dist.length; i++)
		{
			if(dist[i] < 0 || dist[i] > 1)
				throw new BNException("Attempted to create pdist with invalid entry (" + dist[i] + ")");
			sum += dist[i];
		}

		if(Math.abs(sum-1) > 1e-12)
			throw new BNException("Attempted to create unnormalized pdist.");

	}

	public int[] getConditionDimensions()
	{
		return new int[0];
	}

	public DiscreteCPTUC(int index,int card)
	{
		super(0,card);
		this.dist = null;
		this.delta = true;
		this.index = index;
	}

	public DiscreteCPTUC(DiscreteCPTUC orig)
	{
		super(0,orig.getCardinality());
		if(orig.dist==null)
		{
			this.dist = null;
			this.delta = true;
			this.index = orig.index;
		}
		else
		{
			this.delta = false;
			this.index = -1;
			this.dist = new double[orig.dist.length];
			for(int i = 0; i < this.dist.length; i++)
				this.dist[i] = orig.dist[i];
		}
	}

	public static DiscreteCPTUC uniform(int cardinality) throws BNException
	{
		double[] uniform = new double[cardinality];
		double value = ((double)1)/((double)cardinality);
		for(int i = 0; i < cardinality; i++)
			uniform[i] = value;
		return new DiscreteCPTUC(uniform);
	}

	public double evaluate(int[] indices, int value) throws BNException
	{
		if(indices.length!=0)
			throw new BNException("Passed conditions into unconditional distribution.");
		return dist[value];
	}

	public final boolean delta;
	public final int index;
	public final double[] dist;
}