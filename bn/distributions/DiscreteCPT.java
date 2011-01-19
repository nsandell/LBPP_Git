package bn.distributions;

import java.io.PrintStream;
import java.util.Vector;

import util.MathUtil;

import bn.distributions.SparseDiscreteCPT.Entry;
import bn.messages.DiscreteMessage;
import bn.BNException;

/**
 * Standard dense storage of a CPT of arbitrary number of conditions.
 * @author Nils F Sandell
 */
public class DiscreteCPT extends DiscreteDistribution
{
	/**
	 * Create a dense CPT
	 * @param dimSizes Size of the conditioned dimensions, in order
	 * @param cardinality Cardinality of this node.
	 * @param values Dense array that stores the values appropriately.  The first index is
	 * 		the product space if indices and the second index is the variable value.
	 * @throws BNException CPT isn't normalized per condition or some index-mismatches.
	 */
	public DiscreteCPT(int[] dimSizes, int cardinality, double[][] values) throws BNException
	{
		super(cardinality);
		this.dimSizes = dimSizes;
		this.values = values.clone();		
		this.dimprod = 1;
		for(int i = 0; i < this.dimSizes.length; i++)
			this.dimprod *= this.dimSizes[i];
		this.validate();
	}
	
	/**
	 * Special constructor for one parent CPT
	 * @param values The CPT	
	 * @param cardinality The cardinality of this variable.
	 */
	public DiscreteCPT(double[][] values, int cardinality) throws BNException
	{
		super(cardinality);
		this.dimSizes = new int[]{values.length};
		this.values = values;
		this.dimprod = values.length;
		this.validate();
	}
	
	
	/**
	 * Create a dense CPT with an easier to form creation method.
	 * @param dimSizes Size of the conditioning variables in order.
	 * @param cardinality Size of the variable of interest.
	 * @param entries Iterable over entries.  Each entry consists of conditionining indices,
	 * 		value for the variable of interest, and the probability.
	 * @throws BNException If the CPT formed with the arguments is invalid.
	 */
	public DiscreteCPT(int[] dimSizes, int cardinality, Iterable<SparseDiscreteCPT.Entry> entries) throws BNException
	{
		super(cardinality);
		this.dimSizes = dimSizes;
		this.dimprod = 1;
		for(int i = 0; i < dimSizes.length; i++)
			this.dimprod *= dimSizes[i];
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

	/**
	 * Sample this distribution given some instantiation of the parents.
	 */
	@Override
	public int sample(ValueSet<Integer> parents) throws BNException
	{
		int prod = 1;
		for(int i = 0; i < parents.length(); i++)
			prod *= parents.getValue(i);
		double val =  MathUtil.rand.nextDouble();
		double[] dist = this.values[prod];
		double sum = 0;
		for(int i = 0; i < dist.length; i++)
		{
			sum += dist[i];
			if(val < sum)
				return i;
		}
		return dist.length-1;
	}
	
	/**
	 * Validate this CPT
	 */
	private void validate() throws BNException
	{
		if(this.dimSizes.length==0)
			throw new BNException("Cannot create CPT with no parents - use probability vector instead!");
		int[] indices = new int[this.dimSizes.length];
		for(int i = 0; i < indices.length; i++)
			indices[i] = 0;
		do
		{
			int index = getIndex(indices,dimSizes);
			double[] dist = values[index];
			if(dist.length!=this.getCardinality())
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

	/**
	 * Change the distribution over the variable for a given set of parent indices
	 * @param indices Parent values
	 * @param dist New distribution
	 * @throws BNException If bad parent values or bad distribution.
	 */
	public void setDist(int[] indices, double[] dist) throws BNException
	{
		if(dist.length!=this.getCardinality())
			throw new BNException("Attempted to set CPT dist vector at indices " + indexString(indices) + " with wrong sized pdist vector");
		int index = getIndex(indices,dimSizes);

		this.values[index] = dist;
	}
	
	@Override
	public void validateConditionDimensions(int [] dimens) throws BNException
	{
		if(dimens.length!=this.dimSizes.length)
			throw new BNException("Invalid parent set for CPT!");
		for(int i = 0; i < dimens.length; i++)
			if(dimens[i]!=dimSizes[i])
				throw new BNException("Invalid parent set for CPT!");
	}

	@Override
	public double evaluate(int[] indices, int value) throws BNException
	{
		return values[getIndex(indices, this.dimSizes)][value];
	}

	/**
	 * Get the dimensions of the conditioning variable set.
	 * @return The dimensions in an array.
	 */
	public int[] getConditionDimensions(){return this.dimSizes;}
	
	@Override
	public void computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Integer value) throws BNException
	{
		int[] indices = initialIndices(dimSizes.length);
		do
		{
			int compositeindex = getIndex(indices,dimSizes);
			double tmp = 1;
			for(int j = 0; j < indices.length; j++)
				tmp *= incoming_pis.get(j).getValue(indices[j]);
			for(int i = 0; i < this.getCardinality(); i++)
				local_pi.setValue(i, local_pi.getValue(i)+tmp*this.values[compositeindex][i]);
		}
		while((indices = DiscreteDistribution.incrementIndices(indices, dimSizes))!=null);
		
		local_pi.normalize();
	}
	
	@Override
	public void computeLambdas(Vector<DiscreteMessage> lambdas_out, Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda, Integer obsvalue) throws BNException
	{
		int[] indices = initialIndices(dimSizes.length);

		do
		{
			double pi_product = 1;
			int zeroParent = -1;
			for(int i = 0; i < indices.length; i++)
			{
				double value = incoming_pis.get(i).getValue(indices[i]);
				if(value==0 && zeroParent==-1)
					zeroParent = i;
				else if(value==0){pi_product = 0;break;}
				else
					pi_product *= value;
			}

			if(obsvalue==null)
			{
				for(int i = 0; i < this.getCardinality(); i++)
				{
					double p = this.evaluate(indices, i);
					for(int j = 0; j < indices.length; j++)
					{
						double local_pi_product = pi_product;
						if(zeroParent!=-1 && j!=zeroParent)
							local_pi_product = 0;
						if(local_pi_product > 0 && zeroParent==-1)
							local_pi_product /= incoming_pis.get(j).getValue(indices[j]);

						lambdas_out.get(j).setValue(indices[j], lambdas_out.get(j).getValue(indices[j]) + p*local_pi_product*local_lambda.getValue(i));
					}
				}
			}
			else
			{
				Double p = this.evaluate(indices, obsvalue);
				if(p!=null)
				{
					for(int j= 0; j < indices.length; j++)
					{
						double local_pi_product = pi_product;
						if(local_pi_product > 0 && zeroParent==-1)
							local_pi_product /= incoming_pis.get(j).getValue(indices[j]);
						
						lambdas_out.get(j).setValue(indices[j], lambdas_out.get(j).getValue(indices[j]) + p*local_pi_product*local_lambda.getValue(obsvalue));
					}
				}
			}
		}
		while((indices = DiscreteDistribution.incrementIndices(indices, this.dimSizes))!=null);
	}
	
	@Override
	public CPTSufficient2SliceStat getSufficientStatisticObj()
	{
		return new CPTSufficient2SliceStat(this);
	}
	
	public void printDistribution(PrintStream ps)
	{
		ps.println("CPT:");
		int[] indices = initialIndices(this.dimSizes.length);
		int index = 0;
		do
		{
			for(int i =0; i < this.getCardinality(); i++)
				ps.println(indexString(indices) + " => " + i + " w.p. " + this.values[index][i]);
			index++;
		}
		while((indices=incrementIndices(indices, this.dimSizes))!=null);
	}

	/**
	 * Sufficient statistic class for a dense CPT
	 * @author Nils F. Sandell
	 */
	private static class CPTSufficient2SliceStat implements DiscreteSufficientStatistic
	{
		/**
		 * Create a sufficient statistic object
		 * @param cpt For this CPT
		 */
		public CPTSufficient2SliceStat(DiscreteCPT cpt)
		{
			this.cpt = cpt;
			this.exp_tr = new double[this.cpt.dimprod][this.cpt.getCardinality()];
			this.current = new double[this.cpt.dimprod][this.cpt.getCardinality()];
			this.reset();
		}
	
		@Override
		public void reset()
		{
				for(int i =  0; i < this.cpt.dimprod; i++)
				{
					for(int j = 0; j < this.cpt.getCardinality(); j++)
					{
						this.exp_tr[i][j] = 0;
						this.current[i][j] = 0;
					}
				}
		}

		@Override
		public DiscreteSufficientStatistic update(SufficientStatistic stat) throws BNException
		{
			if(!(stat instanceof CPTSufficient2SliceStat))
				throw new BNException("Attempted to combine sufficient statistics of differing types ("+this.getClass().getName()+","+stat.getClass().getName()+")");
			CPTSufficient2SliceStat other = (CPTSufficient2SliceStat)stat;
			if(other.cpt.dimprod!=this.cpt.dimprod || other.cpt.getCardinality()!=this.cpt.getCardinality())
				throw new BNException("Attempted to combine different CPTs statistics..");
			
			for(int i = 0; i < this.cpt.dimprod; i++)
				for(int j = 0; j < this.cpt.getCardinality(); j++)
					this.exp_tr[i][j] += other.exp_tr[i][j];
			return this;
		}
		
		@Override
		public DiscreteSufficientStatistic update(DiscreteMessage lambda,
				Vector<DiscreteMessage> incomingPis) throws BNException
		{
			int[] indices = initialIndices(this.cpt.dimSizes.length);
			double sum = 0;
			do
			{
				int absIndex = getIndex(indices, this.cpt.dimSizes);
				double current_prod = 1;
				for(int i = 0; i < indices.length; i++)
					current_prod *= incomingPis.get(i).getValue(indices[i]);
				for(int x = 0; x < this.cpt.getCardinality(); x++)
				{
					double jointBit = current_prod*this.cpt.values[absIndex][x];
					this.current[absIndex][x] = jointBit*lambda.getValue(x);
					sum += this.current[absIndex][x];
				}
			}
			while((indices = incrementIndices(indices, this.cpt.dimSizes))!=null);
			
			for(int i = 0; i < this.cpt.dimprod; i++)
				for(int j = 0; j < this.cpt.getCardinality(); j++)
					this.exp_tr[i][j] += this.current[i][j]/sum;
			return this;
		}
		
		
		@Override
		public DiscreteSufficientStatistic update(Integer value,
				Vector<DiscreteMessage> incomingPis) throws BNException
		{
			int[] indices = initialIndices(this.cpt.dimSizes.length);
			double sum = 0;
			do
			{
				int absIndex = getIndex(indices, this.cpt.dimSizes);
				double current_prod = 1;
				for(int i = 0; i < indices.length; i++)
					current_prod *= incomingPis.get(i).getValue(indices[i]);
				this.current[absIndex][value] = current_prod*this.cpt.values[absIndex][value];
				sum += this.current[absIndex][value];
			}
			while((indices = incrementIndices(indices, this.cpt.dimSizes))!=null);
			for(int i = 0; i < this.cpt.dimprod; i++)
				this.exp_tr[i][value] += this.current[i][value]/sum;
			return this;
		}
		
		double[][] exp_tr;
		double[][] current;
		private DiscreteCPT cpt;
	}
	
	@Override
	public double optimize(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof CPTSufficient2SliceStat))
			throw new BNException("Failure to optimize CPT parameters : invalid sufficient statistic object used..");
		CPTSufficient2SliceStat stato = (CPTSufficient2SliceStat)stat;
		double maxdiff = 0;
		if(stato.cpt.dimprod!=this.dimprod || stato.cpt.getCardinality() != this.getCardinality())
			throw new BNException("Failure to optimize CPT parameters : misfitting sufficient statistic object used...");
		
		for(int i = 0; i < this.values.length; i++)
		{
			double rowsum = 0;
			for(int j = 0; j < stato.cpt.getCardinality(); j++)
				rowsum += stato.exp_tr[i][j];
			if(rowsum > 0)
			{
				for(int j = 0; j < stato.cpt.getCardinality(); j++)
				{
					double newval = stato.exp_tr[i][j]/rowsum;
					maxdiff = Math.max(Math.abs(this.values[i][j]-newval), maxdiff);
					this.values[i][j] = newval;
				}
			}
		}
		return maxdiff;
	}
	
	@Override
	public DiscreteCPT copy() throws BNException
	{

		double[][] newvalues = new double[dimprod][this.getCardinality()];
		for(int i = 0; i < dimprod; i++)
			for(int j = 0; j < this.getCardinality(); j++)
				newvalues[i][j] = this.values[i][j];
		DiscreteCPT copy = new DiscreteCPT(this.dimSizes, this.getCardinality(),newvalues);
		return copy;
	}
	
	@Override
	public double computeBethePotential(Vector<DiscreteMessage> incoming_pis,
								DiscreteMessage local_lambda, DiscreteMessage marginal, 
								Integer value, int numChildren) throws BNException {

		double[][] marginal_family = new double[dimprod][this.getCardinality()];
		int[] indices = initialIndices(this.dimSizes.length);
		double E = 0, H1 = 0, H2 = 0;
		int numNeighbors = numChildren;
		double sum = 0;

		int iMin = 0, iMax = this.getCardinality();
		if(value!=null)
		{
			iMin = value;
			iMax = value+1;
		}
		do
		{
			int index = getIndex(indices, this.dimSizes); //TODO check if we can replace all of these with index++'s
			for(int i = iMin; i < iMax; i++)
			{
				double tmp = local_lambda.getValue(i)*this.values[index][i];
				for(int j = 0; j < incoming_pis.size(); j++)
					tmp *= incoming_pis.get(j).getValue(indices[j]);
				marginal_family[index][i] = tmp;
				sum += tmp;
			}
		} while((indices = incrementIndices(indices, this.dimSizes))!=null);

		for(int idx = 0; idx < dimprod; idx++)
		{	
			for(int i = iMin; i < iMax; i++)
			{
				if(marginal_family[idx][i] > 0)
				{
					marginal_family[idx][i] /= sum;
					if(this.values[idx][i] > 0)
						E -= marginal_family[idx][i]*Math.log(this.values[idx][i]);
					H1 += marginal_family[idx][i]*Math.log(marginal_family[idx][i]);
				}
			}
		}
		if(value==null)
		{
			for(int i = 0; i < this.getCardinality(); i++)
				if(marginal.getValue(i) > 0)
					H2 += marginal.getValue(i)*Math.log(marginal.getValue(i));
			H2*=numNeighbors;
		}
		return E+H1-H2;
	}
	
	@Override
	public String getDefinition() {
		try
		{
			String ret = "CPT("+this.getCardinality();
			for(int i = 0; i < this.dimSizes.length; i++)
				ret += "," + this.dimSizes[i];
			ret += ")\n";

			int[] indices = initialIndices(this.dimSizes.length);
			do
			{
				String conds = "";
				for(int i = 0; i < indices.length; i++)
					conds += indices[i] + " ";
				int idx = getIndex(indices, this.dimSizes);

				for(int i = 0; i < this.getCardinality(); i++)
					ret += conds + i + " " + values[idx][i] + "\n";

			} while((indices=incrementIndices(indices, this.dimSizes))!=null);
			ret += "*****\n";
			return ret;
			
		} catch(BNException e)
		{
			System.err.println("Error writing file: Problem with CPT output");
			return "";
		}
	}
	
	private int dimprod;
	private int[] dimSizes;
	private double[][] values;

	private static final long serialVersionUID = 50L;
}