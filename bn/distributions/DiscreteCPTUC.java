package bn.distributions;

import java.io.BufferedReader;

import bn.BNException;
import bn.BNDefinitionLoader.BNIOException;

public class DiscreteCPTUC extends DiscreteDistribution
{
	public DiscreteCPTUC(double[] distr) throws BNException
	{
		super(0,distr.length);
		this.innerConstructor(distr);
		this.delta = false;
		this.dist = distr;
		this.index = -1;
	}

	public DiscreteCPTUC(BufferedReader input, int cardinality, int numcond) throws BNIOException
	{
		super(0,cardinality);
		try
		{
			String[] probabilities = input.readLine().split(" ");
			if(probabilities.length!=this.getCardinality())
				throw new BNIOException(" - incorrect number of probabilities...");
			double[] distr = new double[probabilities.length];
			for(int i = 0; i < distr.length; i++)
				distr[i] = Double.parseDouble(probabilities[i]);
			this.innerConstructor(distr);
			this.delta = false;
			this.dist = distr;
			this.index = -1;
		} catch(Exception e)
		{
			throw new BNIOException("Erorr loading unconditional probability dist : " + e.toString(),e);
		}
	}

	private final void innerConstructor(double[] distr) throws BNException
	{
		double sum = 0;
		for(int i = 0; i < distr.length; i++)
		{
			if(distr[i] < 0 || distr[i] > 1)
				throw new BNException("Attempted to create pdist with invalid entry (" + distr[i] + ")");
			sum += distr[i];
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