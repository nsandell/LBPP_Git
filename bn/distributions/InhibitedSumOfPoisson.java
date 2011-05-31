package bn.distributions;

import java.io.PrintStream;

import util.MathUtil;

import cern.jet.random.Poisson;
import cern.jet.random.engine.DRand;

import Jama.Matrix;
import bn.BNException;
import bn.distributions.DiscreteDistribution.InfiniteDiscreteDistribution;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;

/*
 * Assume binary parents, parent being 0 means no affect, 1 means some mean is added
 * into the poisson parameter
 */
public class InhibitedSumOfPoisson extends InfiniteDiscreteDistribution
{
	
	public static double minimumMean = 1e-8;
	public static double minimumAll0Mean = 1e-8;
	
	public InhibitedSumOfPoisson(double[] means, double pinhibit)
	{
		this.all0Inhibitor = pinhibit;
		this.all0mean = minimumAll0Mean;
		this.all0MeanLocked = true;
		this.means = means.clone();
		for(int i = 0; i < means.length; i++)
			this.means[i] = Math.max(this.means[i],minimumMean);
	}
	
	
	public InhibitedSumOfPoisson(double[] means, double all0mean, double pinhibit)
	{
		this(means,pinhibit);
		this.all0mean = Math.max(all0mean,minimumAll0Mean);
		this.all0MeanLocked = false;
	}
	
	public void killParent(int index) throws BNException
	{
		if(index < 0 || index >= this.means.length)
			throw new BNException("Attempted to kill sum of poisson parent that does not exist.");
		
		double[] newmeans = new double[this.means.length-1];
		int ind = 0;
		for(int i = 0; i < this.means.length; i++)
		{
			if(i==index)
				continue;
			newmeans[ind] = this.means[i];
			ind++;
		}
		this.means = newmeans;
	}
	
	public void newParent(double mean)
	{
		double[] newmeans = new double[this.means.length+1];
		for(int i = 0; i < this.means.length; i++)
			newmeans[i] = this.means[i];
		newmeans[this.means.length] = mean;
		this.means = newmeans;
	}
	
	@Override
	public double optimize(SufficientStatistic obj) throws BNException {
		if(obj instanceof InhibitedSumOfPoissonStat)
		{
			InhibitedSumOfPoissonStat stat = (InhibitedSumOfPoissonStat)obj;
			if(stat.weight_sums.length!=(int)Math.pow(2,this.means.length))
				throw new BNException("Invalidly sized additive poisson statistic!");

			double maxdiff = 0;
			
			if(this.means.length > 0)
			{

				Matrix B = new Matrix(stat.weight_sums.length-1,this.means.length);
				Matrix WB = new Matrix(stat.weight_sums.length-1,this.means.length);
				Matrix lambdasums = new Matrix(1,stat.weight_sums.length-1);
				
				int[] indices = initialIndices(this.means.length);
				indices = binaryIndicesIncrement(indices);
				int absindex = 1;
				do 
				{
					if(stat.weight_sums[absindex]==0)
						continue;
					lambdasums.set(0, absindex-1, stat.weighted_sums[absindex]/stat.weight_sums[absindex]);
					for(int i = 0; i < this.means.length; i++)
					{
						B.set(absindex-1, i, indices[i]);
						WB.set(absindex-1, i, indices[i]*stat.weight_sums[absindex]);
					}
					
					absindex++;
				} while((indices = binaryIndicesIncrement(indices))!=null);

				Matrix inv = MathUtil.pseudoInverse(B.transpose().times(WB));
				Matrix newmeans = lambdasums.times(WB).times(inv);

				for(int i = 0; i < newmeans.getColumnDimension(); i++)
				{
					if(stat.weighted_sums[i]==0)
						continue;
					maxdiff = Math.max(Math.abs(this.means[i]-Math.max(newmeans.get(0,i),minimumMean)),maxdiff);
					this.means[i] = Math.max(newmeans.get(0,i),minimumMean);
				}
				if(!this.all0InhibitorLocked)
					this.all0Inhibitor = stat.inhibitPsum/(stat.inhibitPNsum + stat.inhibitPsum);
			}
			
			if(!this.all0MeanLocked)
			{
				double n0m = Math.max(stat.weighted_sums[0]/stat.weight_sums[0],minimumAll0Mean);
				maxdiff = Math.max(Math.abs(this.all0mean-n0m),maxdiff);
				this.all0mean = n0m;
			}
	
			return maxdiff;
		}
		else
			throw new BNException("Expected switching poisson statistic to update switching poisson distribution.");
	}

	@Override
	public void printDistribution(PrintStream pr)
	{
		pr.println(this.getDefinition());
	}

	@Override
	public String getDefinition()
	{
		String ret = "AdditivePoissonInhib(" + this.means.length + "," + this.all0Inhibitor + ")\n"+ this.all0mean;
		for(int i = 0; i < means.length; i++)
			ret += " " + this.means[i];
		ret += "\n";
		return ret;
	}
	
	public int numParents()
	{
		return this.means.length;
	}
	
	public double getMean(int i)
	{
		return this.means[i];
	}

	@Override
	public int sample(ValueSet<Integer> parentVals) throws BNException {
		double mean = 0;
		boolean allZero = true;
		if(MathUtil.rand.nextDouble() < this.all0Inhibitor)
			mean = this.all0mean;
		else
		{
			for(int i = 0; i < parentVals.length(); i++)
			{
				if(parentVals.getValue(i)==1)
				{
					allZero = false;
					mean += this.means[i];
				}
			}
			if(allZero)
				mean = this.all0mean;
		}
		
		Poisson poiss = new Poisson(mean,new DRand(MathUtil.rand.nextInt()));
		return poiss.nextInt();
	}

	@Override
	public InfDiscDistSufficientStat getSufficientStatisticObj() {
		return new InhibitedSumOfPoissonStat(this);
	}

	@Override
	public InhibitedSumOfPoisson copy() throws BNException {
		if(this.all0MeanLocked)
			return new InhibitedSumOfPoisson(this.means,this.all0Inhibitor);
		else 
			return new InhibitedSumOfPoisson(this.means,this.all0mean,this.all0Inhibitor);
	}
	
	@Override
	public double evaluate(int[] indices, int value) throws BNException {
		double mean = 0;
		boolean allZero = true;
		for(int i = 0; i < indices.length; i++)
		{
			if(indices[i]!=0)
			{
				allZero = false;
				mean += indices[i];
			}
		}
		if(allZero)
		{
			Poisson poiss = new Poisson(this.all0mean, new DRand());
			return poiss.pdf(value);
		}
		else
		{
			Poisson poiss1 = new Poisson(mean, new DRand());
			Poisson poiss2 = new Poisson(this.all0mean, new DRand());
			return this.all0Inhibitor*poiss2.pdf(value) + (1-this.all0Inhibitor)*poiss1.pdf(value);
		}
	}

	@Override
	public void validateConditionDimensions(int[] dimens)
			throws BNException {
		if(dimens.length!=this.means.length)
			throw new BNException("Invalid parent set for additive poisson, number of parents incorrect!");
		for(int i = 0; i < dimens.length; i++)
			if(dimens[i]!=2)
				throw new BNException("Invalid parent set for additive poisson, nonbinary parent!");
	}
	
	private static class 
	InhibitedSumOfPoissonStat extends InfDiscDistSufficientStat
	{
		
		public InhibitedSumOfPoissonStat(InhibitedSumOfPoisson dist)
		{
			this.dist = dist;
			this.weight_sums = new double[(int)Math.pow(2,dist.means.length)];
			this.weighted_sums = new double[this.weight_sums.length];
			this.inhibitPsum = 0;
		}

		@Override
		public void reset() {
			for(int i = 0; i < this.weight_sums.length; i++)
			{
				this.weight_sums[i] = 0;
				this.weighted_sums[i] = 0;
			}
			this.inhibitPsum = 0;
		}

		@Override
		public SufficientStatistic update(SufficientStatistic stat)
				throws BNException {
			if(stat instanceof InhibitedSumOfPoissonStat)
			{
				InhibitedSumOfPoissonStat swps = (InhibitedSumOfPoissonStat)stat;
				if(swps.weight_sums.length!=this.weight_sums.length)
					throw new BNException("Attempted to update poisson statistic with differently-sized poisson statistic.");
				
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
			if(incPis.size()!=this.dist.means.length)
				throw new BNException("Attempted to update switching poisson statistic with invalid pi vector set.");
			int[] indices = initialIndices(this.dist.means.length);
			Poisson poiss = new Poisson(0.0, new DRand());

			double weights[] = new double[this.weight_sums.length];
			double weightsums = 0;
			double weightNIsum = 0;
			
			if(this.weight_sums.length > 1)
			{
				int index = 0;
				double valued = value;
				do
				{
					double weight = 1;
					for(int i = 0; i < indices.length; i++)
						weight *= incPis.get(i).getValue(indices[i]);

					double p = 0;
					if(index==0)
					{
						poiss.setMean(dist.all0mean);
						p = poiss.pdf(value);
						weights[0] = (1-this.dist.all0Inhibitor)*weight*p;
						weightsums += weights[0];
					}
					else
					{
						double mean = 0;
						for(int i = 0; i < this.dist.means.length; i++)
							if(indices[i]==1)
								mean += this.dist.means[i];
						poiss.setMean(mean);
						weights[index] = (1-this.dist.all0Inhibitor)*weight*poiss.pdf(value);
						weightsums += weights[index];
					}
					index++;
				} while((indices = binaryIndicesIncrement(indices))!=null);
				
				poiss.setMean(this.dist.all0mean);
				double p_inibited = this.dist.all0Inhibitor*poiss.pdf(value);
				weightsums += p_inibited;
				
				this.weight_sums[0] += p_inibited/weightsums;
				this.weighted_sums[0] += p_inibited/weightsums*valued;
				this.inhibitPsum  += p_inibited/weightsums;
				this.inhibitPNsum += (1-p_inibited/weightsums);
				
				for(int i = 0; i < weight_sums.length; i++)  // Statistics assuming not inhibited.
				{
					this.weight_sums[i] += weights[i]/weightsums;
					this.weighted_sums[i] += weights[i]/weightsums*valued;
				}
			}
			else
			{
				this.weight_sums[0]++;
				this.weighted_sums[0] += value;
			}
			return this;
		}

		private InhibitedSumOfPoisson dist;
		private double weight_sums[], weighted_sums[];
		private double inhibitPsum;
		private double inhibitPNsum;
	}

	@Override
	public void computeLambdas(MessageSet<FiniteDiscreteMessage> lambdas_out,
			MessageSet<FiniteDiscreteMessage> incoming_pis, int obsvalue)
			throws BNException {
		int[] indices = initialIndices(this.means.length);
		Poisson poiss = new Poisson(0.0, new DRand());
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

			double p = 0;
			if(absindex==0)
			{
				//poiss.setMean(minimumAll0Mean);
				//p = this.all0Inhibitor*poiss.pdf(obsvalue);
				poiss.setMean(this.all0mean);
				p = poiss.pdf(obsvalue);
				//p += (1-this.all0Inhibitor)*poiss.pdf(obsvalue);
			}
			else
			{
				double mean = 0;
				for(int i = 0; i < this.means.length; i++)
					if(indices[i]==1)
						mean += this.means[i];
				poiss.setMean(mean);
				p = (1-this.all0Inhibitor)*poiss.pdf(obsvalue);
				poiss.setMean(this.all0mean);
				p += this.all0Inhibitor*poiss.pdf(obsvalue);
			}
			for(int j= 0; j < indices.length; j++)
			{
				double local_pi_product = pi_product;
				if(local_pi_product > 0 && zeroParent!=j)
					local_pi_product /= incoming_pis.get(j).getValue(indices[j]);

				lambdas_out.get(j).setValue(indices[j], lambdas_out.get(j).getValue(indices[j]) + p*local_pi_product);
			}
			absindex++;
		}
		while((indices = binaryIndicesIncrement(indices))!=null);
	}
	
	@Override
	public double computeBethePotential(
			MessageSet<FiniteDiscreteMessage> incoming_pis, int value) {
		//H2 is always zero - no children, always observed.
		Poisson poiss = new Poisson(0.0, new DRand());
		int indices[] = initialIndices(this.means.length);
		double pu[] = new double[(int)Math.pow(2,this.means.length)];
		double E = 0, H1 = 0;
		int index = 0;
		double pusum = 0;
		do
		{
			pu[index] = 1;
			for(int i = 0; i < indices.length; i++)
				pu[index] *= incoming_pis.get(i).getValue(indices[i]);
			
			if(index==0)
			{
				poiss.setMean(this.all0mean);
				double p = poiss.pdf(value);
				pu[index] *= p;
			}
			else
			{
				double mean = 0;
				for(int i = 0; i < this.means.length; i++)
					if(indices[i]==1)
						mean += this.means[i];
				poiss.setMean(mean);
				double p = (1-this.all0Inhibitor)*poiss.pdf(value);
				poiss.setMean(this.all0mean);
				p += this.all0Inhibitor*poiss.pdf(value);
				pu[index] *= p;
			}
			pusum += pu[index];
			index++;
		}
		while((indices = binaryIndicesIncrement(indices))!=null);

		indices = initialIndices(this.means.length);
		index = 0;
		do
		{
			double p = 0;
			if(index==0)
			{
				poiss.setMean(all0mean);
				p = poiss.pdf(value);
			}
			else
			{
				double mean = 0;
				for(int i = 0; i < this.means.length; i++)
					if(indices[i]==1)
						mean += this.means[i];		
				poiss.setMean(mean);
				p = (1-this.all0Inhibitor)*poiss.pdf(value);
				poiss.setMean(this.all0mean);
				p += this.all0Inhibitor*poiss.pdf(value);
			}
			
			double pun = pu[index]/pusum;
			if(pun > 0)
			{
				E -= pun*Math.log(p);
				H1 += pun*Math.log(pun);
			}
			index++;
		}
		while((indices = binaryIndicesIncrement(indices))!=null);

		return E+H1;
	}
	
	private static int[] binaryIndicesIncrement(int[] indices)
	{
		for(int i = indices.length-1; i >=0; i--)
		{
			if(indices[i]==0)
			{
				indices[i] = 1;
				return indices;
			}
			else
				indices[i] = 0;
		}
		return null;
	}
	
	//TODO Verify this method
	@Override
	public double computeObsLL(MessageSet<FiniteDiscreteMessage> incoming_pis,
			int value) {
		Poisson poiss = new Poisson(1,new DRand());
		int indices[] = initialIndices(this.means.length);
		double pu[] = new double[(int)Math.pow(2,this.means.length)];
		int index = 0;
		double pusum = 0;
		do
		{
			pu[index] = 1;
			for(int i = 0; i < indices.length; i++)
				pu[index] *= incoming_pis.get(i).getValue(indices[i]);
			
			/*{
				double mean = 0;
				for(int i = 0; i < this.means.length; i++)
					if(indices[i]==1)
						mean += this.means[i];
				poiss.setMean(mean);
				double p = (1-this.all0Inhibitor)*poiss.pdf(value);
				poiss.setMean(this.all0mean);
				p += this.all0Inhibitor*poiss.pdf(value);
				pu[index] *= p;
			}*/
			pusum += pu[index];
			index++;
		}
		while((indices = binaryIndicesIncrement(indices))!=null);
		
		poiss.setMean(this.all0mean);
		double p = this.all0Inhibitor*poiss.pdf(value);
		index = 0;
		indices = initialIndices(this.means.length);
		do
		{
			double mean = 0;
			if(index==0)
			{
				mean = this.all0mean;
			}
			else
			{
				for(int i = 0; i < this.means.length; i++)
					if(indices[i]==1)
						mean += this.means[i];
			}
			poiss.setMean(mean);
			p += pu[index]/pusum*(1-this.all0Inhibitor)*poiss.pdf(value);
			if(Double.isNaN(p))
			{
				System.err.println("Error NAN : " + mean + " - " + value);
			}
			index++;
		}
		while((indices = binaryIndicesIncrement(indices))!=null);
		
		return -Math.log(p);
	}

	public double all0Inhibitor;
	public boolean all0MeanLocked, all0InhibitorLocked = false;
	public double all0mean = 0;
	private double[] means;

}
