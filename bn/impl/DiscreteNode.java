package bn.impl;

import java.io.Serializable;

import java.util.Vector;

import bn.BNException;
import bn.distributions.DiscreteDistribution;
import bn.distributions.Distribution;
import bn.distributions.Distribution.DiscreteSufficientStatistic;
import bn.distributions.Distribution.SufficientStatistic;
import bn.interfaces.InnerNode;
import bn.interfaces.ContextManager;
import bn.messages.DiscreteMessage;
import bn.messages.Message.MessageInterface;

public class DiscreteNode<Context> implements InnerNode<Context>, Serializable
{
	public DiscreteNode(int cardinality, ContextManager<DiscreteDistribution,Context,DiscreteMessage,Integer> contextManager)
	{
		this.cardinality = cardinality;
		this.contextManager = contextManager;
	}
	
	public void resetMessages()
	{
		this.contextManager.resetMessages();
	}

	@Override
	public double updateMessages(Context ctxt) throws BNException
	{
		this.updateLocalMessages(ctxt);
		this.updateLambdas(ctxt);
		this.updatePis(ctxt);
		DiscreteMessage local_lambda = this.contextManager.getLocalLambda(ctxt), local_pi = this.contextManager.getLocalPi(ctxt);
		DiscreteMessage newmarg = local_lambda.multiply(local_pi);
		newmarg.normalize();
		DiscreteMessage oldMarginal = (DiscreteMessage)this.contextManager.getMarginal(ctxt);
		
		if(oldMarginal==null)
		{
			this.contextManager.setMarginal(ctxt, newmarg);
			return Double.POSITIVE_INFINITY;
		}
		else
		{
			double maxChange = 0;
			for(int i = 0; i < oldMarginal.getCardinality(); i++)
				maxChange = Math.max(maxChange,Math.abs(oldMarginal.getValue(i)-newmarg.getValue(i)));
			this.contextManager.setMarginal(ctxt, newmarg);
			return maxChange;
		}
	}
	
	@Override
	public Distribution getDistribution(Context ctxt)
	{
		return this.contextManager.getCPD(ctxt);
	}
	
	@Override
	public void setDistribution(Context ctxt, Distribution dist) throws BNException
	{
		if(!(dist instanceof DiscreteDistribution))
			throw new BNException("Attempted to set CPD of discrete node to non-discrete distribution.");
		this.contextManager.setCPD(ctxt,(DiscreteDistribution) dist);
	}
	
	protected void updateLocalMessages(Context ctxt) throws BNException
	{
		DiscreteMessage local_lambda = contextManager.getLocalLambda(ctxt);
		DiscreteMessage local_pi = contextManager.getLocalPi(ctxt);
		local_lambda.empty();
		local_pi.empty();
		
		Vector<DiscreteMessage> incomingLambdaMessages = contextManager.getIncomingLambdas(ctxt);
		Vector<DiscreteMessage> incomingPiMessages = contextManager.getIncomingPis(ctxt);
		DiscreteDistribution cpt = contextManager.getCPD(ctxt);
		if(this.contextManager.isObserved(ctxt))
		{
			int value = this.contextManager.getValue(ctxt);
			for(int i = 0; i < this.cardinality; i++)
				local_lambda.setValue(i, 0);
			double tmp = 1;
			for(int i = 0; i < incomingLambdaMessages.size(); i++)
				tmp *= incomingLambdaMessages.get(i).getValue(value);
			local_lambda.setValue(value,tmp);
		}
		else
		{
			for(int i = 0; i < this.cardinality; i++)
			{
				double tmp = 1;
				for(int j = 0; j < incomingLambdaMessages.size(); j++)
					tmp *= incomingLambdaMessages.get(j).getValue(i);
				local_lambda.setValue(i,tmp);
			}
		}
		local_lambda.normalize();

		for(int i = 0; i < this.cardinality; i++)
			local_pi.setValue(i, 0);
	
		Integer valueTmp = this.contextManager.isObserved(ctxt) ? this.contextManager.getValue(ctxt) : null;
		cpt.computeLocalPi(local_pi, incomingPiMessages, valueTmp);
	}
	
	protected void updateLambdas(Context ctxt) throws BNException
	{
		Vector<DiscreteMessage> outgoing_lambdas = this.contextManager.getOutgoingLambdas(ctxt);
		Vector<DiscreteMessage> incoming_pis = this.contextManager.getIncomingPis(ctxt);
		DiscreteDistribution cpt = this.contextManager.getCPD(ctxt);
		DiscreteMessage local_lambda = this.contextManager.getLocalLambda(ctxt);
		for(DiscreteMessage lambda : outgoing_lambdas)
			lambda.empty();
		
		cpt.computeLambdas(outgoing_lambdas, incoming_pis, local_lambda, this.contextManager.getValue(ctxt));
	}
	

	@Override
	public void setValue(Context ctxt, Object value) throws BNException
	{
		if(!(value instanceof Integer))
			throw new BNException("Discrete node only holds integer-values.");
		this.contextManager.setValue(ctxt,(Integer)value);
	}
	
	@Override
	public void clearValue()
	{
		this.contextManager.clearValue();
	}
	
	@Override
	public void clearValue(Context ctxt) throws BNException
	{
		this.contextManager.clearValue(ctxt);
	}
	
	public Integer getValue(Context ctxt) throws BNException
	{
		return this.contextManager.getValue(ctxt);
	}
	
	protected void updatePis(Context ctxt) throws BNException
	{
		int imin = 0; int imax = this.cardinality;
		if(this.contextManager.isObserved(ctxt)){imin = this.contextManager.getValue(ctxt); imax = this.contextManager.getValue(ctxt)+1;}
		
		Vector<DiscreteMessage> incomingLambdaMessages = this.contextManager.getIncomingLambdas(ctxt);
		Vector<DiscreteMessage> outgoingPiMessages = this.contextManager.getOutgoingPis(ctxt);
		DiscreteMessage local_pi = this.contextManager.getLocalPi(ctxt);	
		
		Integer[] zeroNodes = new Integer[imax-imin];
		double[] lambda_prods = new double[imax-imin];
		for(int i = imin; i < imax; i++)
		{
			zeroNodes[i-imin] = null;
			lambda_prods[i-imin] = 1;
		}
		
		for(int i = imin; i < imax; i++)
		{
			for(int j = 0; j < incomingLambdaMessages.size(); j++)
			{
				DiscreteMessage dm = incomingLambdaMessages.get(j);
				if(dm.getValue(i)!=0)	
					lambda_prods[i-imin] *= dm.getValue(i);
				else if(zeroNodes[i-imin]==null)
					zeroNodes[i-imin] = j;
				else
				{
					lambda_prods[i-imin] = 0;
					break;
				}
			}
		}	
		//for(DiscreteParentSubscriber child : this.ds_children)
		for(int j = 0; j < incomingLambdaMessages.size(); j++)
		{
			DiscreteMessage from_child = incomingLambdaMessages.get(j);
			DiscreteMessage pi_child = outgoingPiMessages.get(j);
			pi_child.empty();
			for(int i = imin; i < imax; i++)
			{
				double lambda_prod_local = lambda_prods[i-imin];
				if(lambda_prod_local > 0 && zeroNodes[i-imin]==null)
					lambda_prod_local /= from_child.getValue(i);
				else if(lambda_prod_local > 0 && zeroNodes[i-imin]!=j)
					lambda_prod_local = 0;
				pi_child.setValue(i, lambda_prod_local*local_pi.getValue(i));
			}
		}
	}
	
	public void clearInvalidParents()
	{
		this.contextManager.parentCleanup();
	}
	
	public void clearInvalidChildren()
	{
		this.contextManager.childCleanup();
	}

	@Override
	public DiscreteMessage getMarginal(Context ctxt) throws BNException
	{
		return (DiscreteMessage)this.contextManager.getMarginal(ctxt);
	}

	@Override
	public MessageInterface newChildInterface(Context ctxt)
	{
		DiscreteMessage pi = DiscreteMessage.normalMessage(this.cardinality);
		DiscreteMessage lambda = DiscreteMessage.normalMessage(this.cardinality);
		this.contextManager.newChild(pi, lambda, ctxt);
		return new MessageInterface(lambda, pi);
	}
	
	@Override
	public void addParentInterface(MessageInterface interfce, Context ctxt) throws BNException
	{
		if(!(interfce.lambda instanceof DiscreteMessage && interfce.pi instanceof DiscreteMessage))
			throw new BNException("Failed to add parent to discrete node.. must have discrete-message only interface.");
		this.contextManager.newParent((DiscreteMessage)interfce.pi,(DiscreteMessage)interfce.lambda, ctxt);
	}
	
	public void updateSufficientStat(Context ctxt, DiscreteSufficientStatistic stat) throws BNException
	{
		stat.update(this.contextManager.getLocalLambda(ctxt), this.contextManager.getIncomingPis(ctxt));
	}
	
	@Override
	public void updateSufficientStatistic(Context ctxt, SufficientStatistic stat) throws BNException
	{ 
		if(!(stat instanceof DiscreteSufficientStatistic))
			throw new BNException("Attempted to update a non-discrete sufficient statistic with a discrete node.");
		this.updateSufficientStat(ctxt,(DiscreteSufficientStatistic)stat);
	}
	
	public double optimize(Context ctxt, SufficientStatistic stat) throws BNException
	{
		return this.contextManager.getCPD(ctxt).optimize(stat);
	}
	
	public final int getCardinality()
	{
		return this.cardinality;
	}
	
	public SufficientStatistic getSufficientStatistic(Context ctxt) throws BNException
	{
		return this.contextManager.getCPD(ctxt).getSufficientStatisticObj().update(this.contextManager.getLocalLambda(ctxt),this.contextManager.getIncomingPis(ctxt));
	}
	
	public void validate(Context ctxt) throws BNException
	{
		DiscreteDistribution cpt = this.contextManager.getCPD(ctxt);
		if(cpt==null) throw new BNException("No CPT Set!");
		try
		{
			cpt.validateConditionDimensions(this.getParentDimensions(ctxt));
		} catch(BNException e) {
			throw new BNException("CPT has incorrect number of conditions: " + e.getMessage());
		}
	}
	
	private int[] getParentDimensions(Context ct)
	{
		Vector<DiscreteMessage> incomingPis = this.contextManager.getIncomingPis(ct);
		int[] parentDimesions = new int[incomingPis.size()];
		for(int i = 0; i < parentDimesions.length; i++)
			parentDimesions[i] = incomingPis.get(i).getCardinality();
		return parentDimesions;
	}
	
	public static final long serialVersionUID = 50L;
	
	private int cardinality;
	private ContextManager<DiscreteDistribution,Context,DiscreteMessage,Integer> contextManager;

}
