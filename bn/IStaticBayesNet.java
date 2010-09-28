package bn;


public interface IStaticBayesNet extends IBayesNet<IBayesNode>
{
	public void addEdge(String from, String to) throws BNException;
	public void addEdge(IBayesNode from, IBayesNode to) throws BNException;
	public IDiscreteBayesNode addDiscreteNode(String name, int cardinality) throws BNException;
	public void addDiscreteEvidence(String node, int obsv) throws BNException;
}
