package bn.distributions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import util.Parser.ParserException;

import bn.BNException;

public class SparseDiscreteCPT extends DiscreteDistribution
{
	public static class Entry
	{
		public int[] conditional_indices;
		public int value_index;
		public double p;
	}

	private static class IndexWrapper
	{
		public IndexWrapper(int[] indices)
		{
			this.indices = indices;
		}

		@Override
		public boolean equals(Object other)
		{
			return (other instanceof IndexWrapper) && this.equalsI((IndexWrapper)other);
		}

		@Override
		public int hashCode()
		{
			return Arrays.hashCode(this.indices);
		}

		private boolean equalsI(IndexWrapper other)
		{
			if(this.indices.length!=other.indices.length)
				return false;
			else
			{
				for(int i = 0; i < this.indices.length; i++)
				{
					if(this.indices[i]!=other.indices[i])
						return false;
				}
			}
			return true;
		}

		private int[] indices;
	}

	private static final Pattern patt = Pattern.compile("\\s*(\\d+\\s+)+(0*(\\.\\d+)?)");
	private static final int[] pattgroups = new int[]{1,2};
	
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
	
	public SparseDiscreteCPT(Iterator<Entry> entryTable, int[] dimSizes, int cardinality) throws BNException
	{
		super(dimSizes.length,cardinality);

		this.entries = new HashMap<IndexWrapper, HashMap<Integer,Double>>();
		this.cardinality = cardinality;
		this.dimSizes = dimSizes;

		while(entryTable.hasNext())
		{
			Entry next = entryTable.next();
			if(next.p < 0) throw new BNException("Attempted to initialize with an entry that has p < 0");
			if(next.value_index >= cardinality || !goodIndex(next.conditional_indices,dimSizes))
				throw new BNException("Bad entry with indices " + DiscreteDistribution.indexString(next.conditional_indices) + " for entry " 
						+ next.value_index + " where size is " +DiscreteDistribution.indexString(this.dimSizes) + " with cardinality "
						+ this.cardinality);
			IndexWrapper cur_indices = new IndexWrapper(next.conditional_indices);
			if(entries.get(cur_indices)==null)
				entries.put(cur_indices, new HashMap<Integer,Double>());
			else if(entries.get(cur_indices).get(next.value_index)!=null)
				throw new BNException("Duplicate entry specified for index " + indexString(next.conditional_indices));
			entries.get(cur_indices).put(next.value_index, next.p);
		}
		this.validate();
	}

	public SparseDiscreteCPT(int card, int numdim, int[] dims)
	{
		super(numdim,card);
		this.isBeingConstructed = true;
		this.cardinality = card;
		this.dimSizes = dims;
	}
	private boolean isBeingConstructed = false;

	protected boolean parseLine(String[] args) throws ParserException
	{
		if(!this.isBeingConstructed)
			throw new ParserException("Attempted to construct Sparse CPT not under construction!");

		String[] indexStrings = args[0].split(" ");
		int[] indices = new int[this.dimSizes.length];
		if(indexStrings.length!=(this.dimSizes.length+1))
			throw new ParserException("Insufficient number of condition indices set.");
		try
		{ 
			for(int j = 0; j < this.dimSizes.length; j++)
			{
				indices[j] = Integer.parseInt(indexStrings[j]);
				if(indices[j]>=this.dimSizes[j])
					throw new ParserException("Condition index " + j + " is out of range, should be less than " + this.dimSizes[j]);
			}
			int value = Integer.parseInt(indexStrings[this.dimSizes.length]);
			if(value >= this.cardinality)
				throw new ParserException("Variable value " + value + " is out of range.");
			double p = Double.parseDouble(args[1]);
			if(p < 0 || p > 1)
				throw new ParserException("Invalid probability set (" + p + ")");
			IndexWrapper wrap = new IndexWrapper(indices);
			if(this.entries.get(wrap)==null)
				this.entries.put(wrap, new HashMap<Integer, Double>());
			this.entries.get(wrap).put(value, p);
		} catch(NumberFormatException e) {
			throw new ParserException("Invalid numeric type, expected integer or double and got something else.");
		}
		return true;
	}
	
	protected SparseDiscreteCPT finish() throws ParserException
	{
		this.isBeingConstructed = false;
		try{this.validate();}
		catch(BNException e){throw new ParserException("Error creating sparse CPT: " + e.getMessage());}
		return this;
	}

	private void validate() throws BNException
	{
		int[] indices = new int[this.dimSizes.length];
		for(int i = 0; i < indices.length; i++)
			indices[i] = 0;

		do
		{
			HashMap<Integer,Double> ucdist = entries.get(new IndexWrapper(indices));

			double sum = 0;
			Iterator<Double> values = ucdist.values().iterator();
			while(values.hasNext())
				sum += values.next();

			if(Math.abs(sum-1) > 1e-12)
				throw new BNException("Failed to correctly specify distribution for indices " + indexString(indices));

		} while((indices = incrementIndices(indices, dimSizes))!=null);
	}

	private final static boolean goodIndex(int[] indexes, int[] dimSizes)
	{
		if(indexes.length!=dimSizes.length)
			return false;
		for(int i = 0; i < indexes.length; i++)
		{
			if(indexes[i] >= dimSizes[i])
				return false;
		}
		return true;
	}

	public double evaluate(int[] indices,int value) throws BNException
	{
		if(!goodIndex(indices,this.dimSizes))
			throw new BNException("Failure to evaluate CPT, invalid indices " + indexString(indices) + " for dimensions " + indexString(this.dimSizes));
		if(value >= this.cardinality)
			throw new BNException("Failure to evaluate CPT, bad value " + value + " where cardinality is " + this.cardinality);


		try
		{
			return this.entries.get(new IndexWrapper(indices)).get(value);
		} catch(NullPointerException e)
		{
			return 0;
		}
	}

	public double evaluateFast(int[] indices,int value)
	{
		return this.entries.get(new IndexWrapper(indices)).get(value);
	}

	public int getCardinality(){return this.cardinality;}
	public int[] getConditionDimensions(){return this.dimSizes;}

	int cardinality;
	int[] dimSizes;
	HashMap<IndexWrapper,HashMap<Integer,Double>> entries;
}