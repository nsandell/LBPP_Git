package bn.statc;

import bn.BNException;
import bn.IBayesNode;
import bn.distributions.Distribution;

public interface IBNNode extends IBayesNode {
	Distribution getDistribution();
	void setDistribution(Distribution dist) throws BNException;
}
