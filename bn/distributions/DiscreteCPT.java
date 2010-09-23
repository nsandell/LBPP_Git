package bn.distributions;

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
	
	protected boolean addLine(String line) throws BNException
	{
		if(!this.beingConstructed)
			throw new BNException("Attempted to construct CPT that is not under construction!");
		int[] indices = new int[this.dimSizes.length];
		
		String[] linebits = line.split(" ");
		if(linebits.length!=(this.dimSizes.length+2)) 
			throw new BNException("Insufficient number of conditions specified for CPT.");
		try {
			for(int j = 0; j < this.dimSizes.length; j++)
				indices[j] = Integer.parseInt(linebits[j]);
		} catch(NumberFormatException e) {
			throw new BNException("Expected a integer index, got something else in " + line);
		}
		int index = getIndex(indices, this.dimSizes);
		try {
			int value = Integer.parseInt(linebits[this.dimSizes.length]);
			values[index][value] = Double.parseDouble(linebits[this.dimSizes.length+1]);
		} catch(NumberFormatException e) {
			throw new BNException("Incorrect parameter type for variable value or probability...(" + line + ")");
		}
		return true;
	}
	
	protected DiscreteCPT finish() throws BNException
	{
		this.validate();
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