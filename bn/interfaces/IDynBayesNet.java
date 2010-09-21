package bn.interfaces;

import bn.BNException;

public interface IDynBayesNet extends IBayesNet<IDynBayesNode>
{
	void addInterEdge(String from, String to) throws BNException;
	void addIntraEdge(String from, String to) throws BNException;
	void addInterEdge(IDynBayesNode from, IDynBayesNode to) throws BNException;
	void addIntraEdge(IDynBayesNode from, IDynBayesNode to) throws BNException;
	
	IDiscreteDynBayesNode addDiscreteNode(String name, int cardinality) throws BNException;
	
	int getT();
	
	void run_parallel(int max_iteration, double tolerance, ParallelInferenceCallback callback) throws BNException;
	
	public static interface ParallelInferenceCallback
	{
		void callback(IDynBayesNet neet);
		void error(IDynBayesNet net, String error);
	}
}
