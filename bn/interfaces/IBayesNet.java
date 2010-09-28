package bn.interfaces;

import bn.BNException;
import bn.distributions.Distribution;

public interface IBayesNet<BaseInterface>
{
	public void removeNode(String name) throws BNException;
	public void removeNode(BaseInterface node) throws BNException;
	
	public void setDistribution(String nodeName, Distribution dist) throws BNException;
	
	public Iterable<String>	getNodeNames();
	public BaseInterface getNode(String name);
	
	public void validate() throws BNException;
	public double nodeLogLikelihood(String nodeName) throws BNException;
	
	public void run(int max_iterations, double convergence) throws BNException;
}
