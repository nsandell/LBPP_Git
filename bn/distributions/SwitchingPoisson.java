package bn.distributions;

import java.io.PrintStream;

import cern.jet.random.Poisson;
import cern.jet.random.engine.DRand;

import bn.BNException;
import bn.distributions.DiscreteDistribution.InfiniteDiscreteDistribution;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;

public class SwitchingPoisson extends InfiniteDiscreteDistribution
{
	
	public SwitchingPoisson(double[] means, int[] dimensions)
	{
		this.means = means;
		this.parentDims = dimensions;
	}
	
	public SwitchingPoisson(double[] means)
	{
		this.means = means;
		this.parentDims = new int[1];
		this.parentDims[0] = means.length;
	}

	@Override
	public double optimize(SufficientStatistic obj) throws BNException {
		if(obj instanceof SwPoissonStat)
		{
			double maxdiff = 0;
			SwPoissonStat stat = (SwPoissonStat)obj;
			if(stat.weight_sums.length!=this.means.length)
				throw new BNException("Invalidly sized switching poisson statistic!");
			for(int i = 0; i < stat.weight_sums.length; i++)
			{
				double newmean = stat.weighted_sums[i]/stat.weight_sums[i];
				maxdiff = Math.max(maxdiff,newmean-this.means[i]);
				this.means[i] = newmean;
			}
			return maxdiff;
		}
		else
			throw new BNException("Expected switching poisson statistic to update switching poisson distribution.");
	}

	@Override
	public void printDistribution(PrintStream pr)
	{
		pr.println("Switching Poisson Distribution:");
		int[] indices = initialIndices(this.parentDims.length);
		int index = 0;
		do
		{
			pr.println(indexString(indices) + " => lambda = " + this.means[index]);
			index++;
		}
		while((indices=incrementIndices(indices, this.parentDims))!=null);
	}

	@Override
	public String getDefinition()
	{
		String ret = "SwitchingPoisson(";
		for(int i = 0; i < this.parentDims.length; i++)
			ret += "," + this.parentDims[i];
		ret += ")\n";

		int[] indices = initialIndices(this.parentDims.length);
		int idx = 0;
		do
		{
			String conds = "";
			for(int i = 0; i < indices.length; i++)
				conds += indices[i] + " ";

			ret += conds + " " + this.means[idx] + "\n";
			idx++;

		} while((indices=incrementIndices(indices, this.parentDims))!=null);
		ret += "*****\n";
		return ret;
	}

	@Override
	public int sample(ValueSet<Integer> parentVals) throws BNException {
		poiss.setMean(this.means[getIndex(parentVals, parentDims)]);
		return poiss.nextInt();
	}

	@Override
	public InfDiscDistSufficientStat getSufficientStatisticObj() {
		return new SwPoissonStat(this);
	}

	@Override
	public DiscreteDistribution copy() throws BNException {
		return new SwitchingPoisson(this.means.clone(),this.parentDims.clone());
	}

	@Override
	public double evaluate(int[] indices, int value) throws BNException {
		poiss.setMean(this.means[getIndex(indices, parentDims)]);
		return poiss.pdf(value);
	}

	@Override
	public void validateConditionDimensions(int[] dimens)
			throws BNException {
		if(dimens.length!=this.parentDims.length)
			throw new BNException("Invalid parent set for switching poisson!");
		for(int i = 0; i < dimens.length; i++)
			if(dimens[i]!=parentDims[i])
				throw new BNException("Invalid parent set for switching poisson!");
	}
	
	private static class SwPoissonStat extends InfDiscDistSufficientStat
	{
		
		public SwPoissonStat(SwitchingPoisson dist)
		{
			this.dist = dist;
			this.weight_sums = new double[dist.means.length];
			this.weighted_sums = new double[dist.means.length];
		}

		@Override
		public void reset() {
			for(int i = 0; i < this.weight_sums.length; i++)
			{
				this.weight_sums[i] = 0;
				this.weighted_sums[i] = 0;
			}
		}

		@Override
		public SufficientStatistic update(SufficientStatistic stat)
				throws BNException {
			if(stat instanceof SwPoissonStat)
			{
				SwPoissonStat swps = (SwPoissonStat)stat;
				if(swps.weight_sums.length!=this.weight_sums.length)
					throw new BNException("Attemped to update poisson statistic with differently-sized poisson statistic.");
				
				for(int i = 0; i < swps.weight_sums.length; i++)
				{
					this.weight_sums[i] += swps.weight_sums[i];
					this.weighted_sums[i] += swps.weighted_sums[i];
				}
				return this;
			}
			else throw new BNException("Attempted to update poisson statistic with non-poisson statistic.");
		}

		@Override
		public InfDiscDistSufficientStat update(
				MessageSet<FiniteDiscreteMessage> incPis, int value) throws BNException {
			if(incPis.size()!=this.dist.parentDims.length)
				throw new BNException("Attempted to update switching poisson statistic with invalid pi vector set.");
			int[] indices = initialIndices(this.dist.parentDims.length);
			
			double weights[] = new double[this.dist.means.length];
			double weightsums = 0;
			
			int index = 0;
			double valued = value;
			do
			{
				double weight = 1;
				for(int i = 0; i < indices.length; i++)
					weight *= incPis.get(i).getValue(indices[i]);
				SwitchingPoisson.poiss.setMean(this.dist.means[index]);
				weight *= SwitchingPoisson.poiss.pdf(value);
				weights[index] = weight;
				weightsums += weight;
				index++;
			} while((indices = incrementIndices(indices, this.dist.parentDims))!=null);
			
			for(int i = 0; i < weight_sums.length; i++)
			{
				this.weight_sums[i] += weights[i]/weightsums;
				this.weighted_sums[i] += weights[i]/weightsums*valued;
			}
			return this;
		}

		private SwitchingPoisson dist;
		private double weight_sums[], weighted_sums[];
	}

	@Override
	public void computeLambdas(MessageSet<FiniteDiscreteMessage> lambdas_out,
			MessageSet<FiniteDiscreteMessage> incoming_pis, int obsvalue)
			throws BNException {
		int[] indices = initialIndices(this.parentDims.length);
		int absindex = 0;
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
			poiss.setMean(this.means[absindex]);
			double p = poiss.pdf(obsvalue);
			for(int j= 0; j < indices.length; j++)
			{
				double local_pi_product = pi_product;
				if(local_pi_product > 0 && zeroParent==-1)
					local_pi_product /= incoming_pis.get(j).getValue(indices[j]);

				lambdas_out.get(j).setValue(indices[j], lambdas_out.get(j).getValue(indices[j]) + p*local_pi_product);
			}
			absindex++;
		}
		while((indices = DiscreteDistribution.incrementIndices(indices, this.parentDims))!=null);
	}

	@Override
	public double computeBethePotential(
			MessageSet<FiniteDiscreteMessage> incoming_pis, int value) {
		//H2 is always zero - no children, always observed.
		int indices[] = initialIndices(this.means.length);
		double pu[] = new double[this.means.length];
		double E = 0, H1 = 0;
		int index = 0;
		double pusum = 0;
		do
		{
			pu[index] = 1;
			for(int i = 0; i < indices.length; i++)
				pu[index] *= incoming_pis.get(i).getValue(indices[i]);
			poiss.setMean(this.means[index]);
			pu[index] *= poiss.pdf(value);
			pusum += pu[index];
			index++;
		}
		while((indices = incrementIndices(indices, this.parentDims))!=null);
		
		for(int i = 0; i < this.means.length; i++)
		{
			poiss.setMean(this.means[index]);
			double pun = pu[index]/pusum;
			E -= pun*Math.log(poiss.pdf(value));
			H1 += pun*Math.log(pun);
		}
		return E+H1;
	}

	private static Poisson poiss = new Poisson(1.0, new DRand());
	private double[] means;
	private int[] parentDims;

}
