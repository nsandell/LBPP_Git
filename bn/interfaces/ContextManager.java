package bn.interfaces;

import java.util.Vector;

import bn.BNException;
import bn.distributions.Distribution;
import bn.messages.Message;

public interface ContextManager<DistributionType extends Distribution, ContextType,MessageType extends Message, ValueType>
{
	void newChild(MessageType out_pi, MessageType inc_lambda, ContextType ctxt);
	void newParent(MessageType inc_pi, MessageType outgoing_lambda, ContextType context);
	
	void parentCleanup();
	void childCleanup();

	DistributionType getCPD(ContextType context);
	void setCPD(ContextType context, DistributionType dist);
	
	Vector<MessageType> getIncomingPis(ContextType context);
	Vector<MessageType> getIncomingLambdas(ContextType context);
	
	MessageType getLocalPi(ContextType context);
	MessageType getLocalLambda(ContextType context);
	
	Vector<MessageType> getOutgoingPis(ContextType context);
	Vector<MessageType> getOutgoingLambdas(ContextType context);
	
	Message getMarginal(ContextType context) throws BNException;
	void setMarginal(ContextType context, MessageType message);
	
	void resetMessages();
	
	boolean isObserved(ContextType context);
	
	ValueType getValue(ContextType context) throws BNException;
	void setValue(ContextType context, ValueType value) throws BNException;
	void clearValue(ContextType context) throws BNException;
	void clearValue();
	
}
