package bn.distributions;

import bn.distributions.SparseDiscreteCPT.Entry;
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