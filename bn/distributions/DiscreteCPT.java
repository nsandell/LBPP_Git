package bn.distributions;

import java.io.BufferedReader;

import bn.BNException;
import bn.BNDefinitionLoader.BNIOException;

public class DiscreteCPT extends DiscreteDistribution
{
	public DiscreteCPT(int[] dimSizes, int cardinality, double[][] values) throws BNException
	{
		super(dimSizes.length,cardinality);
		this.cardinality = cardinality;
		this.innerConstructor(dimSizes, values);
	}

	public DiscreteCPT(BufferedReader input, int cardinality, int numdim, int[] dimsizes) throws BNIOException
	{
		super(numdim,cardinality);
		try
		{
			this.cardinality = cardinality;
			this.dimSizes = dimsizes;
			int dimprod = 1;
			for(int i = 0; i < dimsizes.length; i++)
				dimprod *= dimsizes[i];
			double[][] values = new double[dimprod][];
			for(int i = 0; i < dimsizes.length; i++)
				values[i] = new double[cardinality];
			int numNZ = Integer.parseInt(input.readLine());
			int[] indices = new int[numdim];
			for(int i = 0; i < numNZ; i++)
			{
				String[] linebits = input.readLine().split(" ");
				if(linebits.length!=(dimsizes.length+2)) 
					throw new BNIOException("Insufficient conditions specified for CPT.");
				for(int j = 0; j < dimsizes.length; j++)
					indices[j] = Integer.parseInt(linebits[j]);
				int index = getIndex(indices, dimsizes);
				int value = Integer.parseInt(linebits[dimsizes.length]);
				values[index][value] = Double.parseDouble(linebits[dimsizes.length+1]);
			}
			this.innerConstructor(dimsizes, values);
		} catch(Exception e) {
			throw new BNIOException("Failure to load CPT : " + e.toString(),e);
		}
	}

	private void innerConstructor(int[] dimSizes, double[][] values) throws BNException
	{
		this.dimSizes = dimSizes;
		this.values = values;
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