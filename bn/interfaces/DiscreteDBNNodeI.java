package bn.interfaces;

import bn.BayesNet.BNException;
import bn.distributions.DiscreteDistribution;
import bn.messages.DiscreteMessage;

public interface DiscreteDBNNodeI {
	public void setInitialDistribution(DiscreteDistribution dist);
	public void setAdvanceDistribution(DiscreteDistribution dist);
	public DiscreteMessage getMarginal(int t) throws BNException;
	public void setValue(int t, int value) throws BNException;
	public void setValue(int[] values,int t0) throws BNException;
}
