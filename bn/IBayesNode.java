package bn;

import bn.distributions.Distribution.SufficientStatistic;

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
	double updateMessages() throws BNException;
	void optimizeParameters() throws BNException;
	void optimizeParameters(SufficientStatistic stat) throws BNException;
	double getLogLikelihood() throws BNException;
	
	public SufficientStatistic getSufficientStatistic() throws BNException;
	public void updateSufficientStatistic(SufficientStatistic stat) throws BNException;
}
