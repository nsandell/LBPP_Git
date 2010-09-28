package bn.interfaces;

import bn.BNException;
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
	
	void run_parallel(int max_iteration, double tolerance, ParallelInferenceCallback callback) throws BNException;
	void run_parallel_block(int max_iteration, double tolerance) throws BNException;
	
	public static interface ParallelInferenceCallback
	{
		void callback(IDynBayesNet neet);
		void error(IDynBayesNet net, String error);
	}
}
