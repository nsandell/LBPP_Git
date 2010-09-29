package bn;


public interface IBayesNode
{
	String getName();
	Iterable<IBayesNode> getChildren();
	Iterable<IBayesNode> getParents();
	void validate() throws BNException;
	void clearEvidence();
	double updateMessages() throws BNException;
	double getLogLikelihood() throws BNException;
}
