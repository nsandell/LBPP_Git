package bn.interfaces;

import bn.BayesNet.BNException;

public interface BayesNetI {
	public DiscreteBNNodeI addDiscreteNode(String name, int cardinality) throws BNException;
}
