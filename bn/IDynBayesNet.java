package bn;

import bn.distributions.Distribution;

public interface IDynBayesNet extends IBayesNet<IDynBayesNode>
{
	void addInterEdge(String from, String to) throws BNException;
	void addIntraEdge(String from, String to) throws BNException;
	void addInterEdge(IDynBayesNode from, IDynBayesNode to) throws BNException;
	void addIntraEdge(IDynBayesNode from, IDynBayesNode to) throws BNException;
	
	IDiscreteDynBayesNode addDiscreteNode(String name, int cardinality) throws BNException;
	
	int getT();
	
	void setInitialDistribution(String nodeName, Distribution dist) throws BNException;
	
	void setDiscreteEvidence(String nodeName, int t0, int[] evidence) throws BNException;
	
	public static interface ParallelCallback
	{
		void callback(IDynBayesNet neet);
		void error(IDynBayesNet net, String error);
	}
}
