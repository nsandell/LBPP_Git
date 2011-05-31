package bn.distributions;

import java.io.PrintStream;

import cern.jet.random.Poisson;
import cern.jet.random.engine.DRand;

import bn.BNException;
import bn.messages.FiniteDiscreteMessage;

public class PoissonCountdown extends CountdownDistribution {
	
	public PoissonCountdown(int trunc, double l)
	{
		super(trunc);
		this.l = l;
	}

	@Override
	public PSCDStat getSufficientStatisticObj() {
		return new PSCDStat(this);
	}

	@Override
	public double optimize(SufficientStatistic obj) throws BNException {
		if(!(obj instanceof PSCDStat))
			throw new BNException("Attempted to optimize Poisson Countdown distribution with incorrect stat.");
		PSCDStat stat = (PSCDStat)obj;
		double oldl = this.l;
		this.l = stat.weightedmean/stat.weight;
		this.newParameters = true;
		return Math.abs(oldl-this.l);
	}
	
	public static class PSCDStat implements CountdownStatistic
	{
		public PSCDStat(PoissonCountdown pc) {
			this.dist = pc;
		}
		
		@Override
		public void reset() {
			this.weight = 0;
			this.weightedmean = 0;
		}
		@Override
		public SufficientStatistic update(SufficientStatistic stat)
				throws BNException {
			if(!(stat instanceof PSCDStat))
				throw new BNException("Attempted to udpate poisson countdown dist with wrong stat type.");
			else
			{
				PSCDStat statp = (PSCDStat)stat;
				this.weight += statp.weight;
				this.weightedmean += statp.weightedmean;
			}
			return this;
		}
		
		@Override
		public CountdownStatistic update(FiniteDiscreteMessage pi,
				FiniteDiscreteMessage lambda) {
	
			double[] pdist = this.dist.getDist();
			double[] pxandprev0 = new double[dist.truncation];
			double sum = 0;
			for(int i = 0; i < dist.truncation; i++)
			{
				pxandprev0[i] = pi.getValue(0)*pdist[i]*lambda.getValue(i);
				sum += pxandprev0[i];
				if(i < dist.truncation-1)
					sum += lambda.getValue(i)*pi.getValue(i+1);
			}
	
			for(int i = 0; i < dist.truncation; i++)
			{
				double wght = pxandprev0[i]/sum;
				this.weight += wght;
				this.weightedmean += wght*((double)i);
			}
			
			return this;
		}
		
		@Override
		public CountdownStatistic update(FiniteDiscreteMessage pi,
				FiniteDiscreteMessage lambda,double totalweight) {
	
			double[] pdist = this.dist.getDist();
			double[] pxandprev0 = new double[dist.truncation];
			double sum = 0;
			for(int i = 0; i < dist.truncation; i++)
			{
				pxandprev0[i] = pi.getValue(0)*pdist[i]*lambda.getValue(i);
				sum += pxandprev0[i];
				if(i < dist.truncation-1)
					sum += lambda.getValue(i)*pi.getValue(i+1);
			}
	
			for(int i = 0; i < dist.truncation; i++)
			{
				double wght = pxandprev0[i]/sum;
				this.weight += wght*totalweight;
				this.weightedmean += wght*((double)i)*totalweight;
			}
			
			return this;
		}
		
		PoissonCountdown dist;
		double weightedmean = 0;
		double weight = 0;
	}

	@Override
	public PoissonCountdown copy() throws BNException {
		return new PoissonCountdown(this.truncation, this.l);
	}

	@Override
	public void printDistribution(PrintStream pr) {
		pr.print(this.getDefinition());
	}

	@Override
	public String getDefinition() {
		return "PoissonCountdown(" + this.truncation + "," + this.l + ")\n";
	}

	@Override
	public double[] getDist() {
		if(this.newParameters || this.dist==null)
		{
			this.dist = new double[this.truncation];
			Poisson ps = new Poisson(this.l, new DRand());
			double sum = 0;
			for(int i = 0; i < this.truncation; i++)
			{
				dist[i] = ps.pdf(i);
				sum += dist[i];
			}
			for(int i = 0; i < this.truncation; i++)
				dist[i] /= sum;
		}
		return dist; 
	}

	private boolean newParameters = false;
	private double[] dist;
	private double l;
}
