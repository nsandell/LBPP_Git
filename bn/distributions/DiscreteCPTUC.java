package bn.distributions;

import java.util.Vector;

import bn.BNException;
import bn.messages.DiscreteMessage;

public class DiscreteCPTUC extends DiscreteDistribution
{
	public DiscreteCPTUC(double[] distr) throws BNException
	{
		super(distr.length);
		this.dist = distr;
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
		super(card);
		this.dist = null;
	}

	public DiscreteCPTUC(DiscreteCPTUC orig)
	{
		super(orig.getCardinality());
		if(orig.dist==null)
		{
			this.dist = null;
		}
		else
		{
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
	
	public void validateConditionDimensions(int[] dims) throws BNException
	{
		if(dims.length!=0)
			throw new BNException("Probability vector should not have conditions..");
	}

	public double evaluate(int[] indices, int value) throws BNException
	{
		if(indices.length!=0)
			throw new BNException("Passed conditions into unconditional distribution.");
		return dist[value];
	}
	
	public double computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Vector<DiscreteMessage> parent_pis, Integer value) throws BNException
	{
		for(int i = 0; i < local_pi.getCardinality(); i++)
			local_pi.setValue(i, dist[i]);
		if(value!=null)
			return dist[value];
		else
			return 0;
	}
	
	// Should have no parents, so...
	public void computeLambdas(Vector<DiscreteMessage> lambdas_out, Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda, Integer value) throws BNException{}
	
	private final double[] dist;
}