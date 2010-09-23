package bn.distributions;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

import bn.BNException;
import bn.BNDefinitionLoader.BNIOException;

public class NoisyOr extends DiscreteDistribution
{
	public NoisyOr(int numparents, double p) throws BNException
	{
		super(numparents,2); // We will consider this to mean (from 0 -> Infty)
		if(p < 0 || p > 1) throw new BNException("Attempted to specify noisy or with invalid p ( " + p + ")");
		this.p = p;
	}
	
	public NoisyOr(BufferedReader reader, int numconditions) throws BNIOException
	{
		super(numconditions,2);
		try {
			this.p = Double.parseDouble(reader.readLine());
			if(this.p < 0 || this.p > 1) throw new BNIOException("Attempted to specify noisy or with invalid p=" + p + "!");
		} catch(Exception e) {
			throw new BNIOException("Error loading noisy or CPD",e);
		}
	}
	
	public double evaluate(int[] indices, int value)
	{
		int numact = 0;
		for(int i = 0; i< indices.length; i++)
			numact += indices[i]-1;
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