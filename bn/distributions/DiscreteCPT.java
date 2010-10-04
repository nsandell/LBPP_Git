package bn.distributions;

import java.util.Vector;

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
	
	public double computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Vector<DiscreteMessage> parent_pis, Integer value, SufficientStatistic stat, DiscreteMessage localLambda) throws BNException
	{
		CPTSufficient2SliceStat stato = null;
		if(stat!=null && !(stat instanceof CPTSufficient2SliceStat))
			throw new BNException("Must pass CPT update a CPD Sufficient Statistic object or none atall");
		else if(stat!=null)
		{
			if(localLambda==null)
				throw new BNException("Must pass local lambda message in order to compute sufficient statistics.");
			stato = (CPTSufficient2SliceStat)stat;
		}
		
		boolean observed = value!=null;
		double ll = 0;
		int[] indices = initialIndices(dimSizes);
		do
		{
			int compositeindex = getIndex(indices,dimSizes);
			double tmp = 1;
			double observation_ll_tmp = 1;
			for(int j = 0; j < indices.length; j++)
			{
				tmp *= incoming_pis.get(j).getValue(indices[j]);
				if(observed)
					observation_ll_tmp *= parent_pis.get(j).getValue(indices[j]);
			}
			for(int i = 0; i < this.getCardinality(); i++)
			{
				double jointBit = tmp*this.values[compositeindex][i];
				if(stato!=null)
				{
					stato.current[compositeindex][i] += jointBit*localLambda.getValue(i);
					stato.current_sum += jointBit*localLambda.getValue(i);
				}
				local_pi.setValue(i, local_pi.getValue(i)+jointBit);
			}
			if(observed)
				ll += observation_ll_tmp*this.evaluate(indices, value);
		}
		while((indices = DiscreteDistribution.incrementIndices(indices, dimSizes))!=null);
		
		if(stato!=null)
			stato.mergeInCurrent();
		local_pi.normalize();
		return ll;
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
		return new CPTSufficient2SliceStat(this.dimprod,this.getCardinality());
	}
	
	private static class CPTSufficient2SliceStat implements SufficientStatistic
	{
		public CPTSufficient2SliceStat(int dimprod, int card)
		{
			this.dimprod = dimprod;
			this.card = card;
			this.exp_tr = new double[dimprod][card];
			this.current = new double[dimprod][card];
			this.row_sum = new double[dimprod];
			this.reset();
		}
		
		public void reset()
		{
				for(int i =  0; i < this.dimprod; i++)
				{
					this.row_sum[i] = 0;
					for(int j = 0; j < this.card; j++)
					{
						this.exp_tr[i][j] = 0;
						this.current[i][j] = 0;
					}
				}
				this.current_sum = 0;
		}
		
		public void mergeInCurrent()
		{
			for(int i = 0; i < this.dimprod; i++)
			{
				for(int j = 0; j < this.card; j++)
				{
					double tmp = this.current[i][j]/this.current_sum;
					this.exp_tr[i][j] += tmp;
					this.row_sum[i] += tmp;
					this.current[i][j] = 0;
				}
			}
			this.current_sum = 0;
		}
		
		int dimprod, card;
		double[][] exp_tr;
		double[] row_sum;
		double[][] current;
		double current_sum;
	}
	
	public void optimize(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof CPTSufficient2SliceStat))
			throw new BNException("Failure to optimize CPT parameters : invalid sufficient statistic object used..");
		CPTSufficient2SliceStat stato = (CPTSufficient2SliceStat)stat;
		if(stato.dimprod!=this.values.length || stato.card != this.getCardinality())
			throw new BNException("Failure to optimize CPT parameters : misfitting sufficient statistic object used...");
		
		for(int i = 0; i < this.values.length; i++)
		{
			if(stato.row_sum[i] > 0)
			{
				for(int j = 0; j < stato.card; j++)
				{
					this.values[i][j] = stato.exp_tr[i][j]/stato.row_sum[i];
				}
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