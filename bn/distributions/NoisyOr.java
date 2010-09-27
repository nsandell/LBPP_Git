package bn.distributions;

import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.ParserException;

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
	
	protected boolean parseLine(String[] args) throws ParserException
	{
		if(!beingConstructed)
			throw new ParserException("Attempted to construct Noisy-Or node not under construction!");
		try {
			this.p = Double.parseDouble(args[0]);
		} catch(NumberFormatException e) {
			throw new ParserException("Expected probability for noisy-or parameter, got " + args[0]);
		}
		if(this.p < 0 || this.p > 1) throw new ParserException("Attempted to specify noisy or with invalid p=" + p + "!");
		this.beingConstructed = false;
		return false;
	}
	
	protected Pattern getBuilderRegex(){return argumentPattern;}
	protected int[] getRegExGroups(){return argGroups;}
	protected String getBuilderPrompt(){return "Enter activation probability:";};
	
	private boolean beingConstructed = false;
	private static Pattern argumentPattern = Pattern.compile("0*(\\.\\d+)?");
	private static int[] argGroups = new int[]{0};
	
	protected NoisyOr finish() throws ParserException
	{
		if(this.beingConstructed)
			throw new ParserException("Expected to get noisy-or parameter!");
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