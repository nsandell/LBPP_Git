package bn.interfaces;

import bn.BNException;
import bn.distributions.Distribution.SufficientStatistic;
import bn.messages.Message;

public interface InnerNode<Context, MessageType extends Message,ValueType>
{
	Message.MessageInterface<MessageType> newChildInterface(Context ctxt) throws BNException;
	void addParentInterface(Message.MessageInterface<?> interfce, Context ctxt) throws BNException;
	
	void clearInvalidChildren();
	void clearInvalidParents();
	
	MessageType getMarginal(Context ctxt) throws BNException;
	double updateMessages(Context ctxt) throws BNException;
	
	public void updateSufficientStatistic(Context ctxt, SufficientStatistic stat) throws BNException;
	public double optimize(Context ctxt, SufficientStatistic stat) throws BNException;
	public SufficientStatistic getSufficientStatistic(Context ctxt) throws BNException;
	
	public ContextManager getContextManager();	
	void validate(Context ctxt) throws BNException;
	
	void setValue(Context ctxt, ValueType value) throws BNException;
	void clearValue();
	void clearValue(Context ctxt) throws BNException;
	ValueType getValue(Context ctxt) throws BNException;
	
	double getLogLikelihood(Context ctxt);
	double getLogLikelihood();
}
