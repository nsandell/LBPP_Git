package bn.interfaces;

import bn.BNException;

import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.messages.Message;
import bn.messages.Message.MessageInterface;

public interface InnerNode<Context>
{
	MessageInterface newChildInterface(Context ctxt) throws BNException;
	void addParentInterface(MessageInterface interfce, Context ctxt) throws BNException;
	
	void clearInvalidChildren();
	void clearInvalidParents();
	
	Message getMarginal(Context ctxt) throws BNException;
	double updateMessages(Context ctxt) throws BNException;
	
	public void updateSufficientStatistic(Context ctxt, SufficientStatistic stat) throws BNException;
	public double optimize(Context ctxt, SufficientStatistic stat) throws BNException;
	public SufficientStatistic getSufficientStatistic(Context ctxt) throws BNException;
	
	void validate(Context ctxt) throws BNException;
	
	void setDistribution(Context ctxt, Distribution dist) throws BNException;
	Distribution getDistribution(Context ctxt);
	
	void resetMessages();
	
	void setValue(Context ctxt, Object value) throws BNException;
	void clearValue();
	void clearValue(Context ctxt) throws BNException;
	Object getValue(Context ctxt) throws BNException;
}
