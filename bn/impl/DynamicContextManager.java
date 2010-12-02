package bn.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import bn.BNException;
import bn.distributions.Distribution;
import bn.interfaces.ContextManager;
import bn.messages.Message;

public class DynamicContextManager<DistributionType extends Distribution, MessageType extends Message,ValueType> implements ContextManager<DistributionType, Integer,MessageType,ValueType>
{
	public DynamicContextManager(ArrayList<MessageType> local_pi, ArrayList<MessageType> local_lambda) throws BNException
	{
		if(local_lambda.size()!=local_pi.size())
			throw new BNException("Attempted to create dynamic context manager with unequal sized local message arrays..");
		this.T = local_lambda.size();
		this.local_lambda = local_lambda;
		this.local_pi = local_pi;
		this.llNorm = new double[this.T];
		this.incoming_lambdas = new ArrayList<Vector<MessageType>>(local_pi.size());
		this.incoming_pis = new ArrayList<Vector<MessageType>>(local_pi.size());
		this.outgoing_lambdas = new ArrayList<Vector<MessageType>>(local_pi.size());
		this.outgoing_pis = new ArrayList<Vector<MessageType>>(local_pi.size());
		this.value = new ArrayList<ValueType>(local_pi.size());
		this.marginals = new ArrayList<MessageType>();
		for(int i= 0 ; i < T; i++)
		{
			this.marginals.add(null);
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
		return this.local_pi.get((int)t);
	}

	@Override
	public MessageType getLocalLambda(Integer t) {
		return this.local_lambda.get((int)t);
	}

	@Override
	public boolean isObserved(Integer t) {
		return value.get(t)!=null;
	}

	@Override
	public ValueType getValue(Integer t) throws BNException {
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
			this.local_lambda.get(t).setInitial();
			this.local_pi.get(t).setInitial();
		}
	}
	
	public boolean neverprinted = true;
	
	@Override
	public Message getMarginal(Integer t) throws BNException {
		if(this.marginals.size() > t)
			return marginals.get(t);
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
		this.marginals.set(t, marginal);
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
	
	@Override
	public void setLLNormalization(Integer context, double value) {
		this.llNorm[context] = value;
	}

	@Override
	public double getLLNormalization(Integer context) {
		return this.llNorm[context];
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
	private double[] llNorm;
	private DistributionType initial = null;
	private DistributionType advance = null;
	private ArrayList<ValueType> value = null;
	private ArrayList<Vector<MessageType>> incoming_pis;
	private ArrayList<Vector<MessageType>> incoming_lambdas;
	private ArrayList<Vector<MessageType>> outgoing_pis;
	private ArrayList<Vector<MessageType>> outgoing_lambdas;
	private ArrayList<MessageType> local_pi;
	private ArrayList<MessageType> local_lambda;
	private ArrayList<MessageType> marginals;
}
