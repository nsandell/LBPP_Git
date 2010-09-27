package bn.distributions;

import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.LineHandler;
import util.Parser.ParserException;
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
	
	
	public static class DiscreteDistributionBuilder implements DistributionBuilder{
		
		public DiscreteDistributionBuilder(String type, String name, int cardinality, int[] dimensions, HashMap<String,Distribution> distmap) throws ParserException {
			try {
				int numconditions = dimensions.length;
				this.name = name;
				this.distMap = distmap;
				switch(DiscreteDistributionType.valueOf(type))
				{
				case PV:
					if(numconditions!=0)
						throw new ParserException("Specified a number of conditions for an unconditional PV");
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
					throw new ParserException("Unsupported discrete probability distribution type " + type);
				}
			} catch(IllegalArgumentException e) {
				throw new ParserException("Unrecognized discrete probabiltiy distribution type " + type);
			}
		}

		public LineHandler parseLine(String[] args) throws ParserException
		{
			if(this.inner.parseLine(args))
				return this;
			this.distMap.put(this.name,this.inner.finish());
			return null;
		}
		
		public DiscreteDistribution getFinished() throws ParserException {return this.inner.finish();}
		
		public int[] getGroups() {return this.inner.getRegExGroups();}

		public Pattern getRegEx(){return this.inner.getBuilderRegex();}

		public String getPrompt() {return this.inner.getBuilderPrompt();}
		
		public void finish() throws ParserException
		{
			this.distMap.put(this.name, this.inner.finish());
		}

		private HashMap<String, Distribution> distMap;
		private DiscreteDistribution inner;
		String name;
	}
		
	protected abstract String getBuilderPrompt();
    protected abstract Pattern getBuilderRegex();
    protected abstract int[] getRegExGroups();
	
	protected abstract boolean parseLine(String[] args) throws ParserException;
	protected abstract DiscreteDistribution finish() throws ParserException;
	
	public static DiscreteDistributionBuilder getDiscreteDistributionBuilder(String type, String name, String args, HashMap<String, Distribution> distMap) throws ParserException
	{
		try {
			String[] indexArgs = args.split(",");
			int[] conditions = new int[indexArgs.length-1];
			for(int i = 0; i < indexArgs.length-1; i++)
				conditions[i] = Integer.parseInt(indexArgs[i]);
			int card = Integer.parseInt(indexArgs[indexArgs.length-1]);
			return new DiscreteDistributionBuilder(type, name, card, conditions, distMap);
		} catch(NumberFormatException e) {
			throw new ParserException("Invalid discrete distribution arguments..");
		}
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
