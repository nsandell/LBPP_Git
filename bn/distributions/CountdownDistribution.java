package bn.distributions;

import java.io.PrintStream;

import bn.BNException;
import bn.messages.FiniteDiscreteMessage;

public abstract class CountdownDistribution implements Distribution {
	
	public static class SwitchingCountdownDistribution implements Distribution
	{
		public CountdownDistribution[] distributions;
		
		public static class SwitchingCDStat implements SufficientStatistic
		{
			public CountdownStatistic[] stats;

			@Override
			public void reset() {
				for(int i = 0; i < stats.length; i++)
					stats[i].reset();
			}

			@Override
			public SwitchingCDStat update(SufficientStatistic stat)
					throws BNException {
				if(!(stat instanceof SwitchingCDStat))
					throw new BNException("EXpected a switching countdown stat..");
				
				SwitchingCDStat stat2 = (SwitchingCDStat)stat;
				if(this.stats.length!=stat2.stats.length)
					throw new BNException("Inappropriately matched switching countdown statistics!");
				for(int i = 0; i < this.stats.length; i++)
					this.stats[i].update(stat2.stats[i]);
				
				return this;
			}
			
			public SwitchingCDStat update(FiniteDiscreteMessage pi, FiniteDiscreteMessage lambda, double[] weights)
				throws BNException {
				
				if(weights.length!=stats.length)
					throw new BNException("Expected weight vector of proper dimension for updating switching countdown distribution.");
				
				for(int i = 0; i < this.stats.length; i++)
					this.stats[i].update(pi, lambda, weights[i]);
				return this;
			}
			
		}

		@Override
		public SwitchingCDStat getSufficientStatisticObj() {
			SwitchingCDStat stat = new SwitchingCDStat();
			stat.stats = new CountdownStatistic[distributions.length];
			for(int i = 0; i < distributions.length; i++)
				stat.stats[i] = this.distributions[i].getSufficientStatisticObj();
			return stat;
		}

		@Override
		public double optimize(SufficientStatistic obj) throws BNException {
			if(!(obj instanceof SwitchingCDStat))
				throw new BNException("EXpected a switching countdown stat..");

			SwitchingCDStat stat2 = (SwitchingCDStat)obj;
			if(this.distributions.length!=stat2.stats.length)
				throw new BNException("Inappropriate optimization for countdown statistics!");

			double err = 0;
			for(int i = 0; i < this.distributions.length; i++)
				err = Math.max(err,this.distributions[i].optimize(stat2.stats[i]));
			
			return err;
		}

		@Override
		public SwitchingCountdownDistribution copy() throws BNException {
			SwitchingCountdownDistribution dist = new SwitchingCountdownDistribution();
			dist.distributions =new CountdownDistribution[this.distributions.length];
			for(int i = 0; i < dist.distributions.length; i++)
				dist.distributions[i] = this.distributions[i].copy();
			return dist;
		}

		@Override
		public void printDistribution(PrintStream pr) {
			pr.println(this.getDefinition());
		}

		@Override
		public String getDefinition() {
			String ret = "SwitchingCountdownDistribution(" + this.distributions.length + ")\n";
			for(int i =0; i < this.distributions.length; i++)
				ret += this.distributions[i].getDefinition();
			return ret;
		}
		
	}

	public CountdownDistribution(int truncation)
	{
		this.truncation = truncation;
	}
	
	public static interface CountdownStatistic extends SufficientStatistic
	{
		public CountdownStatistic update(FiniteDiscreteMessage incpi, FiniteDiscreteMessage lambda);
		public CountdownStatistic update(FiniteDiscreteMessage incpi, FiniteDiscreteMessage lambda, double weight);
	}
	
	@Override
	public abstract CountdownStatistic getSufficientStatisticObj();

	@Override
	public abstract CountdownDistribution copy() throws BNException;

	public abstract double[] getDist();
	
	protected int truncation;
}
