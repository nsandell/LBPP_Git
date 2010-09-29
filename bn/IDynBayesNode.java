package bn;


public interface IDynBayesNode extends IBayesNode
{
	Iterable<IDynBayesNode> getInterChildren();
	Iterable<IDynBayesNode> getIntraChildren();
	Iterable<IDynBayesNode> getInterParents();
	Iterable<IDynBayesNode> getIntraParents();
	
	/**
	 * Get the log likelihood of all the observed instances of this node.
	 * @return The log likelihood desired.
	 */
	IBayesNode getInstance(int t);
	int getCardinality();
	double getLogLikelihood() throws BNException;
}