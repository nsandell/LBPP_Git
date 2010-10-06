package bn.distributions;

import java.util.Vector;

import util.MathUtil;

import bn.distributions.SparseDiscreteCPT.Entry;
import bn.messages.DiscreteMessage;
import bn.BNException;

public class DiscreteCPT extends DiscreteDistribution
{
	public DiscreteCPT(int[] dimSizes, int cardinality, double[][] values) throws BNException
	{
		super(cardinality);
		this.dimSizes = dimSizes;
		this.values = values;		
		this.dimprod = 1;
		for(int i = 0; i < this.dimSizes.length; i++)
			this.dimprod *= this.dimSizes[i];
		this.validate();
	}
	
	public int sample(IntegerValueSet parents) throws BNException
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

	private void validate() throws BNException
	{
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

	public void setDist(int[] indices, double[] dist) throws BNException
	{
		if(dist.length!=this.getCardinality())
			throw new BNException("Attempted to set CPT dist vector at indices " + indexString(indices) + " with wrong sized pdist vector");
		int index = getIndex(indices,dimSizes);

		this.values[index] = dist;
	}
	
	public void validateConditionDimensions(int [] dimens) throws BNException
	{
		if(dimens.length!=this.dimSizes.length)
			throw new BNException("Invalid parent set for CPT!");
		for(int i = 0; i < dimens.length; i++)
			if(dimens[i]!=dimSizes[i])
				throw new BNException("Invalid parent set for CPT!");
	}

	public double evaluate(int[] indices, int value) throws BNException
	{
		return values[getIndex(indices, this.dimSizes)][value];
	}

	public int[] getConditionDimensions(){return this.dimSizes;}
	
	public double computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Vector<DiscreteMessage> parent_pis, Integer value) throws BNException
	{
		boolean observed = value!=null;
		double p = 0;
		int[] indices = initialIndices(dimSizes);
		do
		{
			int compositeindex = getIndex(indices,dimSizes);
			double tmp = 1;
			double observation_p_tmp = 1;
			for(int j = 0; j < indices.length; j++)
			{
				tmp *= incoming_pis.get(j).getValue(indices[j]);
				if(observed)
					observation_p_tmp *= parent_pis.get(j).getValue(indices[j]);
			}
			for(int i = 0; i < this.getCardinality(); i++)
				local_pi.setValue(i, local_pi.getValue(i)+tmp*this.values[compositeindex][i]);
			if(observed)
				p += observation_p_tmp*this.evaluate(indices, value);
				//p += observation_p_tmp*this.values[compositeindex][value];
		}
		while((indices = DiscreteDistribution.incrementIndices(indices, dimSizes))!=null);
		
		local_pi.normalize();
		return p;
	}
	
	public void computeLambdas(Vector<DiscreteMessage> lambdas_out, Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda, Integer obsvalue) throws BNException
	{
		int[] indices = initialIndices(dimSizes);

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
	
	public CPTSufficient2SliceStat getSufficientStatisticObj()
	{
		return new CPTSufficient2SliceStat(this);
	}
	
	private static class CPTSufficient2SliceStat implements DiscreteSufficientStatistic
	{
		public CPTSufficient2SliceStat(DiscreteCPT cpt)
		{
			this.cpt = cpt;
			this.exp_tr = new double[this.cpt.dimprod][this.card];
			this.current = new double[this.cpt.dimprod][this.card];
			this.reset();
		}
		
		public void reset()
		{
				for(int i =  0; i < this.cpt.dimprod; i++)
				{
					for(int j = 0; j < this.card; j++)
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
			if(other.cpt.dimprod!=this.cpt.dimprod || other.card!=this.card)
				throw new BNException("Attempted to combine different CPTs statistics..");
			
			for(int i = 0; i < this.cpt.dimprod; i++)
				for(int j = 0; j < this.card; j++)
					this.exp_tr[i][j] += other.exp_tr[i][j];
			return this;
		}
		

		@Override
		public DiscreteSufficientStatistic update(DiscreteMessage lambda, DiscreteMessage pi,
				Vector<DiscreteMessage> incomingPis) throws BNException
		{
			int[] indices = initialIndices(this.cpt.dimSizes);
			double sum = 0;
			do
			{
				int absIndex = getIndex(indices, this.cpt.dimSizes);
				double current_prod = 1;
				for(int i = 0; i < indices.length; i++)
					current_prod *= incomingPis.get(i).getValue(indices[i]);
				for(int x = 0; x < this.card; x++)
				{
					double jointBit = current_prod*this.cpt.values[absIndex][x];
					this.current[absIndex][x] = jointBit*lambda.getValue(x);//*pi.getValue(x);
					sum += this.current[absIndex][x];
				}
			}
			while((indices = incrementIndices(indices, this.cpt.dimSizes))!=null);
			
			for(int i = 0; i < this.cpt.dimprod; i++)
				for(int j = 0; j < this.card; j++)
					this.exp_tr[i][j] += this.current[i][j]/sum;
			return this;
		}
		
		int card;
		double[][] exp_tr;
		double[][] current;
		private DiscreteCPT cpt;
	}
	
	public void optimize(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof CPTSufficient2SliceStat))
			throw new BNException("Failure to optimize CPT parameters : invalid sufficient statistic object used..");
		CPTSufficient2SliceStat stato = (CPTSufficient2SliceStat)stat;
		if(stato.cpt.dimprod!=this.dimprod || stato.card != this.getCardinality())
			throw new BNException("Failure to optimize CPT parameters : misfitting sufficient statistic object used...");
		
		for(int i = 0; i < this.values.length; i++)
		{
			double rowsum = 0;
			for(int j = 0; j < stato.card; j++)
				rowsum += stato.exp_tr[i][j];
			if(rowsum > 0)
			{
				for(int j = 0; j < stato.card; j++)
					this.values[i][j] = stato.exp_tr[i][j]/rowsum;
			}
		}
	}
	
	public DiscreteCPT copy() throws BNException
	{

		double[][] newvalues = new double[dimprod][this.getCardinality()];
		for(int i = 0; i < dimprod; i++)
			for(int j = 0; j < this.getCardinality(); j++)
				newvalues[i][j] = this.values[i][j];
		DiscreteCPT copy = new DiscreteCPT(this.dimSizes, this.getCardinality(),newvalues);
		return copy;
	}
	
	private int dimprod;
	private int[] dimSizes;
	private double[][] values;
}