package bn;

import bn.distributions.Distribution.SufficientStatistic;

public interface Optimizable
{
	public SufficientStatistic getSufficientStatistic() throws BNException;
	public double optimizeParameters(SufficientStatistic stat) throws BNException;
	public double optimizeParameters() throws BNException;
}
