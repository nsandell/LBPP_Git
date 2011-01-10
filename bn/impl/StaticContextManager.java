package bn.impl;

import java.util.Iterator;
import java.util.Vector;

import bn.BNException;
import bn.distributions.Distribution;
import bn.interfaces.ContextManager;
import bn.messages.Message;

public class StaticContextManager<DistributionType extends Distribution, MessageType extends Message,ValueType> implements ContextManager<DistributionType,Void,MessageType,ValueType>
{
	
	public StaticContextManager(MessageType local_pi, MessageType local_lambda)
	{
		this.local_lambda = local_lambda;
		this.local_pi = local_pi;
	}
	
	@Override
	public DistributionType getCPD(Void context)
	{
		return this.cpd;
	}
	
	public void setCPD(DistributionType cpd) throws BNException
	{
		this.cpd = cpd;
	}

	@Override
	public Vector<MessageType> getIncomingPis(Void context)
	{
		return this.incoming_pis;
	}

	@Override
	public Vector<MessageType> getIncomingLambdas(Void context) {
		return this.incoming_lambdas;
	}
	
	@Override
	public Vector<MessageType> getOutgoingPis(Void context)
	{
		return this.outgoing_pis;
	}

	@Override
	public Vector<MessageType> getOutgoingLambdas(Void context) {
		return this.outgoing_lambdas;
	}

	@Override
	public MessageType getLocalPi(Void context) {
		return this.local_pi;
	}

	@Override
	public MessageType getLocalLambda(Void context) {
		return this.local_lambda;
	}

	@Override
	public boolean isObserved(Void context) {
		return value!=null;
	}

	@Override
	public ValueType getValue(Void context) {
		return value;
	}

	@Override
	public void setValue(Void context, ValueType value) throws BNException {
		this.value = value;
	}

	@Override
	public Message getMarginal(Void context) throws BNException {
		return this.marginal;
	}

	
	@Override
	public void newChild(MessageType out_pi, MessageType lambda, Void c)
	{
		this.outgoing_pis.add(out_pi);
		this.incoming_lambdas.add(lambda);
	}
	
	@Override
	public void newParent(MessageType inc_pi, MessageType outgoing_lambda, Void v)
	{
		this.incoming_pis.add(inc_pi);
		this.outgoing_lambdas.add(outgoing_lambda);
	}
	
	public void resetMessages()
	{
		for(MessageType msg : this.outgoing_lambdas)
			msg.setInitial();
		for(MessageType msg :  this.outgoing_pis)
			msg.setInitial();
		this.local_lambda.setInitial();
		this.local_pi.setInitial();
		this.marginal.setInitial();
	}
	
	@Override
	public void parentCleanup()
	{
		Iterator<MessageType> messIt = this.incoming_pis.iterator();
		while(messIt.hasNext())
		{
			if(!messIt.next().isValid())
				messIt.remove();
		}
		messIt = this.outgoing_lambdas.iterator();
		while(messIt.hasNext())
		{
			if(!messIt.next().isValid())
				messIt.remove();
		}
	}
	
	@Override
	public void setCPD(Void context, DistributionType dist)
	{
		this.cpd = dist;
	}
	
	@Override
	public void childCleanup()
	{
		Iterator<MessageType> messIt = this.outgoing_pis.iterator();
		while(messIt.hasNext())
		{
			if(!messIt.next().isValid())
				messIt.remove();
		}
		messIt = this.incoming_lambdas.iterator();
		while(messIt.hasNext())
		{
			if(!messIt.next().isValid())
				messIt.remove();
		}
	}
	
	@Override
	public void setMarginal(Void context, MessageType marginal)
	{
		this.marginal = marginal;
	}
	
	@Override
	public void clearValue(Void context) throws BNException {
		this.value = null;
	}

	@Override
	public void clearValue() {
		this.value = null;
	}
	
	@Override
	public void setLLNormalization(Void context,double value) {
		this.llNorm = value;
	}

	@Override
	public double getLLNormalization(Void context) {
		return this.llNorm;
	}
	
	private double llNorm =  0;
	private DistributionType cpd;
	private ValueType value = null;
	private Vector<MessageType> incoming_pis = new Vector<MessageType>();
	private Vector<MessageType> incoming_lambdas = new Vector<MessageType>();
	private Vector<MessageType> outgoing_pis = new Vector<MessageType>();
	private Vector<MessageType> outgoing_lambdas = new Vector<MessageType>();
	private MessageType local_pi;
	private MessageType local_lambda;
	private MessageType marginal;

}
