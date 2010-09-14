package bn.distributions;

import java.util.HashMap;
import java.util.Iterator;

import bn.BayesNet.BNException;

public class SparseDiscreteCPT extends DiscreteDistribution
{
	static class Entry
	{
		public int[] conditional_indices;
		public int value_index;
		public double p;
	}
	
	public SparseDiscreteCPT(Iterator<Entry> entryTable, int[] dimSizes, int cardinality) throws BNException
	{
		super(dimSizes.length,cardinality);
		
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
			if(entries.get(next.conditional_indices)==null)
				entries.put(next.conditional_indices, new HashMap<Integer,Double>());
			else if(entries.get(next.conditional_indices).get(next.value_index)!=null)
				throw new BNException("Duplicate entry specified for index " + indexString(next.conditional_indices));
			entries.get(next.conditional_indices).put(next.value_index, next.p);
		}
		
		int[] indices = new int[dimSizes.length];
		for(int i = 0; i < indices.length; i++)
			indices[i] = 0;
		
		do
		{
			HashMap<Integer,Double> ucdist = entries.get(indices);
			
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
		
		return this.entries.get(indices).get(value);
	}
	
	public double evaluateFast(int[] indices,int value)
	{
		return this.entries.get(indices).get(value);
	}
	
	public int getCardinality(){return this.cardinality;}
	public int[] getConditionDimensions(){return this.dimSizes;}

	int cardinality;
	int[] dimSizes;
	HashMap<int[],HashMap<Integer,Double>> entries;
}