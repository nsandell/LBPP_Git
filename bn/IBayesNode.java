package bn;

/**
 * Basic Bayesian node
 * @author nsandell
 *
 */
public interface IBayesNode
{
	String getName();
	Iterable<IBayesNode> getChildren();
	Iterable<IBayesNode> getParents();
	void validate() throws BNException;
	void clearEvidence();
	void collectSufficientStats(boolean flag);
	double updateMessages(boolean collectEnabled) throws BNException;
	void optimizeParameters() throws BNException;
	double getLogLikelihood() throws BNException;
}
