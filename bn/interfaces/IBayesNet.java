package bn.interfaces;

import bn.BNException;

public interface IBayesNet<BaseInterface>
{
	public void removeNode(String name) throws BNException;
	public void removeNode(BaseInterface node) throws BNException;
	
	public Iterable<String>	getNodeNames();
	public BaseInterface getNode(String name);
	
	public void validate() throws BNException;
	
	public void run(int max_iterations, double convergence) throws BNException;
}
