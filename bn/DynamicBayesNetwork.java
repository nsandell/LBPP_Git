package bn;

import bn.BayesNet.BNException;

public class DynamicBayesNetwork {
	
	public DynamicBayesNetwork(int T){this.T = T;}
	
	public void validate() throws BNException
	{
		
	}
	
	protected int T;
	protected BayesNet unrolled_network = new BayesNet();
	
	
	public static class DBNNode
	{
		
	}
}
