package bn;

import bn.distributions.DiscreteDistribution;
import bn.messages.DiscreteMessage;

public interface IDiscreteBayesNode extends IBayesNode
{
	int getValue() throws BNException;
	int getCardinality();
	void setDistribution(DiscreteDistribution dist) throws BNException;
	void setValue(int o) throws BNException;
	void clearValue() throws BNException;
	DiscreteMessage getMarginal();
}
