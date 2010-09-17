package bn;

class DynamicBayesNetwork {
	
	public DynamicBayesNetwork(int T){this.T = T;}
	
	public void validate() throws BNException
	{
		
	}
	
	public int getT()
	{
		return this.T;
	}
	
	protected int T;
	protected BayesNet unrolled_network = new BayesNet();
	
	public static class DBNNode
	{
		
	}
}
