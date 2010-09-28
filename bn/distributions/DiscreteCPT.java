package bn.distributions;

import bn.distributions.SparseDiscreteCPT.Entry;
import java.util.regex.Pattern;

import util.Parser.ParserException;

import bn.BNException;

public class DiscreteCPT extends DiscreteDistribution
{
	public DiscreteCPT(int[] dimSizes, int cardinality, double[][] values) throws BNException
	{
		super(dimSizes.length,cardinality);
		this.dimSizes = dimSizes;
		this.values = values;
		this.cardinality = cardinality;
		this.validate();
	}
	
	public DiscreteCPT(int[] dimSizes, int cardinality, Iterable<SparseDiscreteCPT.Entry> entries) throws BNException
	{
		super(dimSizes.length, cardinality);
		this.dimSizes = dimSizes;
		this.cardinality = cardinality;
		int dimprod = 1;
		for(int i = 0; i < dimSizes.length; i++)
			dimprod *= dimSizes[i];
		this.values = new double[dimprod][];
		for(int i = 0; i < dimprod; i++)
			this.values[i] = new double[cardinality];
		for(Entry entry : entries)
		{
			int index = getIndex(entry.conditional_indices, dimSizes);
			values[index][entry.value_index] = entry.p;
		}
		this.validate();
	}

	DiscreteCPT(int cardinality, int numdim, int[] dimsizes)
	{
		super(numdim,cardinality);
		this.beingConstructed = true;
		this.dimSizes = dimsizes;
		this.cardinality = cardinality;
		this.dimSizes = dimsizes;
		int dimprod = 1;
		for(int i = 0; i < dimsizes.length; i++)
			dimprod *= dimsizes[i];
		values = new double[dimprod][];
		for(int i = 0; i < dimprod; i++)
			values[i] = new double[cardinality];
	}

	private boolean beingConstructed = false;
	
	protected boolean parseLine(String[] args) throws ParserException
	{
		if(!this.beingConstructed)
			throw new ParserException("Attempted to construct CPT that is not under construction!");
		int[] indices = new int[this.dimSizes.length];
		
		String[] indexStrings = args[0].split(" ");
		if(indexStrings.length!=(this.dimSizes.length+1)) 
			throw new ParserException("Insufficient number of conditions specified for CPT.");
		try {
			for(int j = 0; j < this.dimSizes.length; j++)
				indices[j] = Integer.parseInt(indexStrings[j]);
		} catch(NumberFormatException e) {
			throw new ParserException("Expected a integer index, got something else.");
		}
		try {
			int index = getIndex(indices, this.dimSizes);
			int value = Integer.parseInt(indexStrings[this.dimSizes.length]);
			values[index][value] = Double.parseDouble(args[1]);
		} catch(NumberFormatException e) {
			throw new ParserException("Incorrect parameter type for variable value or probability...");
		} catch(BNException e){throw new ParserException("Indexing exception : " + e.getMessage());}
		return true;
	}
	
	private static final Pattern patt = Pattern.compile("^\\s*((\\d+\\s+)+)(0*(\\.\\d+)?)$");
	private static final int[] pattgroups = new int[]{1,3};
	
	protected String getBuilderPrompt()
	{
		return "Enter CPT row:";
	}
	
	protected Pattern getBuilderRegex()
	{
		return patt;
	}
	
	protected int[] getRegExGroups()
	{
		return pattgroups;
	}
	
	protected DiscreteCPT finish() throws ParserException
	{
		try{this.validate();}
		catch(BNException e){throw new ParserException("Error creating CPT: " + e.getMessage());}
		return this;
	}

	private void validate() throws BNException
	{
		int[] indices = new int[this.dimSizes.length];
		for(int i = 0; i < indices.length; i++)
			indices[i] = 0;
		do
		{
			int index = getIndex(indices,dimSizes);
			double[] dist = values[index];
			if(dist.length!=cardinality)
				throw new BNException("Attempted to initialize CPT with wrong sized dist vector at indices " + indexString(indices));
			double sum = 0;
			for(int i = 0; i < dist.length; i++)
			{
				sum += dist[i];
				if(dist[i] < 0)
					throw new BNException("Discrete CPT negative for indices" + indexString(indices) + " at entry" + i);
			}
			if(Math.abs(sum-1) > 1e-12)
				throw new BNException("Discrete CPT non-normalized for indices " + indexString(indices));
		} while((indices = incrementIndices(indices, dimSizes))!=null);
	}

	public void setDist(int[] indices, double[] dist) throws BNException
	{
		if(dist.length!=cardinality)
			throw new BNException("Attempted to set CPT dist vector at indices " + indexString(indices) + " with wrong sized pdist vector");
		int index = getIndex(indices,dimSizes);

		this.values[index] = dist;
	}

	public double evaluate(int[] indices, int value) throws BNException
	{
		return values[getIndex(indices, this.dimSizes)][value];
	}

	public int getCardinality(){return this.cardinality;}
	public int[] getConditionDimensions(){return this.dimSizes;}

	int cardinality;
	int[] dimSizes;
	double[][] values;

}