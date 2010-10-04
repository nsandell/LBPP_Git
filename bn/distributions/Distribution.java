package bn.distributions;

import bn.BNException;

public interface Distribution
{
	public static enum DiscreteDistributionType
	{
		PV,
		CPT,
		SparseCPT,
		NoisyOr
	}
	
	public SufficientStatistic getSufficientStatisticObj();
	public void optimize(SufficientStatistic obj) throws BNException;
	public Distribution copy() throws BNException;
	
	public static interface SufficientStatistic
	{
		public void reset();
	}
}
