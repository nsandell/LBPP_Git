package bn.distributions;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import bn.BNException;
import bn.BNDefinitionLoader.BNIOException;

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
	
	public SparseDiscreteCPT(Iterator<Entry> entryTable, int[] dimSizes, int cardinality) throws BNException
	{
		super(dimSizes.length,cardinality);
		this.innerConstructor(entryTable, cardinality, dimSizes);
	}
	
	public SparseDiscreteCPT(BufferedReader br, int card, int numdim, int[] dims) throws BNIOException
	{
		super(numdim,card);
		try
		{
			ArrayList<Entry> entrieslist =  new ArrayList<Entry>();
			int numNZ = Integer.parseInt(br.readLine());
			int[] indices = new int[numdim];
			for(int i = 0; i < numNZ; i++)
			{
				String[] linebits = br.readLine().split(" ");
				if(linebits.length!=(numdim+2))
					throw new BNIOException("Insufficient number of condition indices set.");
				for(int j = 0; j < numdim; j++)
				{	
					indices[j] = Integer.parseInt(linebits[j]);
				}
				int value = Integer.parseInt(linebits[numdim]);
				double p = Double.parseDouble(linebits[numdim+1]);
				Entry curr = new Entry();
				curr.conditional_indices = indices.clone();
				curr.value_index = value;
				curr.p = p;
				entrieslist.add(curr);
			}
			this.innerConstructor(entrieslist.iterator(), card, dims);
		} catch(Exception e) {
			throw new BNIOException("Error while loading Sparse CPT : " + e.toString(), e);
		}
	}
	
	private void innerConstructor(Iterator<Entry> entryTable, int cardinality, int[] dimSizes) throws BNException
	{
		
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
		int[] indices = new int[dimSizes.length];
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
		
		return this.entries.get(new IndexWrapper(indices)).get(value);
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