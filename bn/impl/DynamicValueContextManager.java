package bn.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import bn.BNException;
import bn.distributions.Distribution;
import bn.interfaces.ContextManager;
import bn.messages.Message;

public class DynamicValueContextManager<DistributionType extends Distribution, MessageType extends Message,ValueType> implements ContextManager<DistributionType, Integer,MessageType,ValueType>
{
	public DynamicValueContextManager(ArrayList<MessageType> local_pi, ArrayList<MessageType> local_lambda) throws BNException
	{
		if(local_lambda.size()!=local_pi.size())
			throw new BNException("Attempted to create dynamic context manager with unequal sized local message arrays..");
		this.T = local_lambda.size();
		this.incoming_lambdas = new ArrayList<Vector<MessageType>>(T);
		this.incoming_pis = new ArrayList<Vector<MessageType>>(T);
		this.outgoing_lambdas = new ArrayList<Vector<MessageType>>(T);
		this.outgoing_pis = new ArrayList<Vector<MessageType>>(T);
		this.value = new ArrayList<ValueType>(T);
		for(int i= 0 ; i < T; i++)
		{
			this.incoming_lambdas.add(new Vector<MessageType>(2));
			this.incoming_pis.add(new Vector<MessageType>(2));
			this.outgoing_lambdas.add(new Vector<MessageType>(2));
			this.outgoing_pis.add(new Vector<MessageType>(2));
			this.value.add(null);
		}
	}

	@Override
	public Vector<MessageType> getIncomingPis(Integer t)
	{
		return this.incoming_pis.get((int)t);
	}

	@Override
	public Vector<MessageType> getIncomingLambdas(Integer t)
	{
		return this.incoming_lambdas.get((int)t);
	}
	
	@Override
	public Vector<MessageType> getOutgoingPis(Integer t)
	{
		return this.outgoing_pis.get((int)t);
	}

	@Override
	public Vector<MessageType> getOutgoingLambdas(Integer t) {
		return this.outgoing_lambdas.get((int)t);
	}

	@Override
	public MessageType getLocalPi(Integer t) {
		return null; //TODO Pi?
	}

	@Override
	public MessageType getLocalLambda(Integer t) {
		return null; //TODO Delta?
	}

	@Override
	public boolean isObserved(Integer t) {
		return value.get(t)!=null;
	}

	@Override
	public ValueType getValue(Integer t) {
		return value.size() > t ? value.get(t) : null;
	}

	@Override
	public void setValue(Integer t, ValueType value) throws BNException {
		this.value.set(t, value);
	}
	
	public void resetMessages()
	{
		for(int t = 0; t < this.outgoing_lambdas.size(); t++)
		{
			for(MessageType msg : this.outgoing_lambdas.get(t))
				msg.setInitial();
			for(MessageType msg : this.outgoing_pis.get(t))
				msg.setInitial();
		}
	}
	
	public boolean neverprinted = true;
	
	@Override
	public Message getMarginal(Integer t) throws BNException {
		return null;
	}
	
	
	@Override
	public void setCPD(Integer t, DistributionType dist)
	{
		if(t==0)
			this.setInitialDistribution(dist);
		else
			this.setAdvanceDistribution(dist);
	}
	
	@Override
	public void setMarginal(Integer t, MessageType marginal)
	{
	}

	@Override
	public void newChild(MessageType out_pi, MessageType inc_lambda, Integer t)
	{
		this.outgoing_pis.get((int)t).add(out_pi);
		this.incoming_lambdas.get((int)t).add(inc_lambda);
	}
	
	@Override
	public void newParent(MessageType inc_pi, MessageType outgoing_lambda, Integer t)
	{
		this.incoming_pis.get(t).add(inc_pi);
		this.outgoing_lambdas.get(t).add(outgoing_lambda);
	}
	
	@Override
	public void parentCleanup()
	{
		for(int i = 0; i < this.T; i++)
			this.parentCleanup(i);
	}
	
	public void parentCleanup(int t)
	{
		Iterator<MessageType> messIt = this.incoming_pis.get(t).iterator();
		while(messIt.hasNext())
		{
			if(!messIt.next().isValid())
				messIt.remove();
		}
		messIt = this.outgoing_lambdas.get(t).iterator();
		while(messIt.hasNext())
		{
			if(!messIt.next().isValid())
				messIt.remove();
		}
	}
	
	@Override
	public void childCleanup()
	{
		for(int t = 0; t < T; t++)
			this.childCleanup(t);
	}
	
	public void childCleanup(int t)
	{
		Iterator<MessageType> messIt = this.outgoing_pis.get(t).iterator();
		while(messIt.hasNext())
		{
			if(!messIt.next().isValid())
				messIt.remove();
		}
		messIt = this.incoming_lambdas.get(t).iterator();
		while(messIt.hasNext())
		{
			if(!messIt.next().isValid())
				messIt.remove();
		}
	}

	@Override
	public void clearValue(Integer context) throws BNException {
		this.value.set(context, null);
	}

	@Override
	public void clearValue() {
		for(int t = 0; t < T; t++)
			this.value.set(t, null);
	}
	
	public void setInitialDistribution(DistributionType init)
	{
		this.initial = init;
	}
	
	public void setAdvanceDistribution(DistributionType adva)
	{
		this.advance = adva;
	}
	
	@Override
	public DistributionType getCPD(Integer context) {
		return (this.initial!=null && context==0) ? this.initial : this.advance;
	}
	
	public boolean hasInitial()
	{
		return initial!=null;
	}
	
	public DistributionType getInitial()
	{
		return this.initial;
	}
	
	public DistributionType getAdvance()
	{
		return this.advance;
	}
	
	private int T;
	private DistributionType initial = null;
	private DistributionType advance = null;
	private ArrayList<ValueType> value = null;
	private ArrayList<Vector<MessageType>> incoming_pis;
	private ArrayList<Vector<MessageType>> incoming_lambdas;
	private ArrayList<Vector<MessageType>> outgoing_pis;
	private ArrayList<Vector<MessageType>> outgoing_lambdas;
}
