package bn.distributions;

import java.util.HashMap;
import bn.BNException;

public class NoisyOr extends DiscreteDistribution
{
	public NoisyOr(int numparents, double p) throws BNException
	{
		super(numparents,2); // We will consider this to mean (from 0 -> Infty)
		if(p < 0 || p > 1) throw new BNException("Attempted to specify noisy or with invalid p ( " + p + ")");
		this.p = p;
	}
	
	public NoisyOr(int numconditions) 
	{
		super(numconditions,2);
		this.beingConstructed = true;
	}
	
	protected boolean addLine(String line) throws BNException
	{
		if(!beingConstructed)
			throw new BNException("Attempted to construct Noisy-Or node not under construction!");
		try {
			this.p = Double.parseDouble(line);
		} catch(NumberFormatException e) {
			throw new BNException("Expected probability for noisy-or parameter, got " + line);
		}
		if(this.p < 0 || this.p > 1) throw new BNException("Attempted to specify noisy or with invalid p=" + p + "!");
		this.beingConstructed = false;
		return false;
	}
	private boolean beingConstructed = false;
	
	protected NoisyOr finish() throws BNException
	{
		if(this.beingConstructed)
			throw new BNException("Expected to get noisy-or parameter!");
		else return this;
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