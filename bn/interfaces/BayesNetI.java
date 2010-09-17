package bn.interfaces;

import bn.BayesNet.BNException;

public interface BayesNetI {
	public DiscreteBNNodeI addDiscreteNode(String name, int cardinality) throws BNException;
	public void addEdge(String from, String to) throws BNException;
	public void addEdge(BNNodeI from, BNNodeI to) throws BNException;
	public BNNodeI getNode(String name);
	public void validate() throws BNException;
	public void run(int max_iterations, double convergence) throws BNException;
}
