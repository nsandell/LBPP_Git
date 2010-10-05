package bn.distributions;

import java.util.Vector;

import bn.BNException;
import bn.messages.DiscreteMessage;

public interface Distribution
{
	public SufficientStatistic getSufficientStatisticObj();
	public void optimize(SufficientStatistic obj) throws BNException;
	public Distribution copy() throws BNException;
	
	public static interface SufficientStatistic
	{
		public void reset();
		public SufficientStatistic update(SufficientStatistic stat) throws BNException;
	}
	
	public static interface DiscreteSufficientStatistic extends SufficientStatistic
	{
		@Override
		public DiscreteSufficientStatistic update(SufficientStatistic stat) throws BNException;
		public DiscreteSufficientStatistic update(DiscreteMessage lambda, DiscreteMessage pi, Vector<DiscreteMessage> incomingPis) throws BNException;
	}
}
