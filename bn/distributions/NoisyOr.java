package bn.distributions;

import java.util.HashMap;
import bn.BNException;

public class NoisyOr extends DiscreteDistribution
{
	public NoisyOr(int numparents, double p) throws BNException
	{
		super(numparents,2); // We will consider this to mean (from 0 -> Infty)
		if(p < 0 || p > 1) throw new BNException("Attempted to specify noisy or with invalid p ( " + p + ")");
		this.p = 1-p;
	}
	
	public double evaluate(int[] indices, int value)
	{
		int numact = 0;
		for(int i = 0; i< indices.length; i++)
			numact += indices[i];
		if(value==1)
			return getProbability1(numact);
		else
			return 1-getProbability1(numact);
	}
	
	double getProbability1(int numActiveParents)
	{
		return 1-Math.pow(p, numActiveParents);
	}
	
	public int[] getConditionDimensions()
	{
		int[] ret = dimensions.get(this.getNumConditions());
		if(ret==null)
		{
			ret = new int[this.getNumConditions()];
			for(int i =0; i < this.getNumConditions(); i++)
				ret[i] = 2;
			dimensions.put(this.getNumConditions(), ret);
		}
		return ret;
	}
	
	static HashMap<Integer, int[]> dimensions = new HashMap<Integer, int[]>();
	
	double p;
}