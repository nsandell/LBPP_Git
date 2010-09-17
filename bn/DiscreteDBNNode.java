package bn;

import bn.BayesNet.BNException;
import bn.interfaces.BNNodeI;

public class DiscreteDBNNode extends DBNNode<DiscreteBNNode> {
	
	public DiscreteDBNNode(DynamicBayesNetwork bn, BayesNet unrolled, String basename, int cardinality) throws BNException
	{
		super(bn);
		for(int t = 0; t < bn.getT(); t++)
		{
			this.nodeInstances.set(t, unrolled.addDiscreteNode(basename+"["+t+"]", cardinality));
		}
	}

	@Override
	public BNNodeI getInstance(int t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Class getBNNodeClass() {
		// TODO Auto-generated method stub
		return null;
	}

}
