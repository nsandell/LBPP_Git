package bn.impl;

import java.util.ArrayList;
import java.util.Vector;

import java.util.HashMap;

import bn.BNException;
import bn.IBayesNode;
import bn.IDiscreteBayesNode;
import bn.distributions.DiscreteDistribution;
import bn.distributions.Distribution.DiscreteSufficientStatistic;
import bn.distributions.Distribution.SufficientStatistic;
import bn.interfaces.DiscreteChildSubscriber;
import bn.interfaces.DiscreteParentSubscriber;
import bn.messages.DiscreteMessage;

class DiscreteBNNode extends BNNode implements DiscreteParentSubscriber, DiscreteChildSubscriber, IDiscreteBayesNode, DiscreteDistribution.IntegerValueSet.IntegerValueObject
{
	
	DiscreteBNNode(StaticBayesianNetwork net, String name, int cardinality)
	{
		super(net,name);
		this.cardinality = cardinality;
		this.local_pi = new DiscreteMessage(cardinality);
		this.local_lambda = new DiscreteMessage(cardinality);
	}
	
	public void validate() throws BNException
	{
		if(this.cpt==null) throw new BNException("Error while validating node " + this.getName() + ", no CPT set!");
		try
		{
			this.cpt.validateConditionDimensions(this.getParentDimensions());
		} catch(BNException e) {
			throw new BNException("Error while validating node " + this.getName() + ", CPT has incorrect number of conditions: " + e.getMessage());
		}
	}
	
	protected void addParentI(BNNode parent) throws BNException
	{
		if(!(parent instanceof DiscreteBNNode))
			throw new BNException("Parent of discrete node must also be a discrete node.");
		if(this.parentMap.get(parent)!=null)
			return;
		this.parentMap.put(parent, this.numParents);
		this.ds_parents.add((DiscreteBNNode)parent);
		this.incomingPiMessages.add(DiscreteMessage.allOnesMessage(((DiscreteBNNode)parent).getCardinality())); 
		this.parents_local_pis.add(((DiscreteBNNode)parent).local_pi);
		this.outgoing_lambdas.add(DiscreteMessage.allOnesMessage(((DiscreteBNNode)parent).getCardinality()));
		this.parent_dims = null;
		this.numParents++;
	}
	
	protected void removeParentI(BNNode parent) throws BNException
	{
		if(!(parent instanceof DiscreteBNNode))
			throw new BNException("Attempted to remove a parent from discrete node that isn't discrete!");
		Integer parentIndex = this.parentMap.get(parent);
		if(parentIndex==null)
			return;
		this.parentMap.remove(parent);
		this.incomingPiMessages.remove(parentIndex);
		this.outgoing_lambdas.remove(parentIndex);
		this.ds_parents.remove(parentIndex);
		this.parents_local_pis.remove(parentIndex);
		this.parent_dims = null;
		this.numParents--;
	}
	
	protected void addChildI(BNNode child) throws BNException
	{
		if(!(child instanceof DiscreteParentSubscriber))
			throw new BNException("Child of discrete node must be able to handle discrete parents.");
		this.childMap.put((DiscreteParentSubscriber)child,this.numChildren);
		this.incomingLambdaMessages.add(DiscreteMessage.allOnesMessage(this.getCardinality())); 
		this.ds_children.add((DiscreteParentSubscriber)child);
		this.numChildren++;
	}
	
	protected void removeChildI(BNNode child) throws BNException
	{
		if(!(child instanceof DiscreteParentSubscriber))
			throw new BNException("Attempted to remove child from parent who could never have been a child!");
		Integer childIndex = this.childMap.get((DiscreteParentSubscriber)child);
		if(childIndex==null)
			return;
		this.childMap.remove((DiscreteParentSubscriber)child);
		this.incomingLambdaMessages.remove(childIndex);
		this.ds_children.remove(childIndex);
		this.numChildren--;
	}
	
	public void setDistribution(DiscreteDistribution distribution) throws BNException
	{
		if(distribution.getCardinality()!=this.cardinality)
			throw new BNException("Attempted to set a CPT of incorrect cardinality!");
		this.cpt = distribution;
	}

	@Override
	public void handleLambda(IBayesNode child, DiscreteMessage dm)
	{
		this.incomingLambdaMessages.set(this.childMap.get(child), dm);
	}

	@Override
	public void handlePi(IBayesNode parent, DiscreteMessage dm)
	{
		this.incomingPiMessages.set(this.parentMap.get(parent), dm);
	}

	@Override
	public double updateMessages() throws BNException
	{
		this.updateLocalMessages();
		this.updateLambdas();
		this.updatePis();
		DiscreteMessage oldmarg = this.marginalDistribution;
		this.marginalDistribution = this.local_lambda.multiply(this.local_pi);
		this.marginalDistribution.normalize();
		if(oldmarg==null)
			return Double.POSITIVE_INFINITY;
		else
		{
			double maxChange = 0;
			for(int i = 0; i < oldmarg.getCardinality(); i++)
				maxChange = Math.max(maxChange,Math.abs(oldmarg.getValue(i)-this.marginalDistribution.getValue(i)));
			return maxChange;
		}
	}
	
	public DiscreteMessage getMarginal()
	{
		return this.marginalDistribution;
	}
	
	protected void updateLocalMessages() throws BNException
	{
		if(this.observed)
		{
			for(int i = 0; i < this.cardinality; i++)
				this.local_lambda.setValue(i, 0);
			double tmp = 1;
			for(DiscreteParentSubscriber child : this.ds_children) //TODO probably replace these with for(int..) loops
				tmp *= this.incomingLambdaMessages.get(this.childMap.get(child)).getValue(this.value);
			this.local_lambda.setValue(this.value,tmp);
		}
		else
		{
			for(int i = 0; i < this.cardinality; i++)
			{
				double tmp = 1;
				for(DiscreteParentSubscriber child : this.ds_children)
					tmp *= this.incomingLambdaMessages.get(this.childMap.get(child)).getValue(i);
				this.local_lambda.setValue(i,tmp);
			}
		}
		this.local_lambda.normalize();

		for(int i = 0; i < this.cardinality; i++)
			this.local_pi.setValue(i, 0);
	
		Integer valueTmp = (this.observed) ? this.value : null;
			this.likelihoodGivenPast = this.cpt.computeLocalPi(this.local_pi, this.incomingPiMessages, this.parents_local_pis, valueTmp);
	}
	
	protected void updateLambdas() throws BNException
	{
		for(DiscreteMessage lambda : this.outgoing_lambdas)
		{
			for(int i = 0; i < lambda.getCardinality(); i++)
				lambda.setValue(i, 0);
		}
		
		if(this.observed)
			this.cpt.computeLambdas(this.outgoing_lambdas, this.incomingPiMessages, this.local_lambda, this.value);
		else
			this.cpt.computeLambdas(this.outgoing_lambdas, this.incomingPiMessages, this.local_lambda, null);

		for(int i = 0; i < this.ds_parents.size(); i++)
			this.ds_parents.get(i).handleLambda(this, this.outgoing_lambdas.get(i));
	}
	
	protected void updatePis()
	{
		int imin = 0; int imax = this.cardinality;
		if(this.observed){imin = this.value; imax = this.value+1;}
		
		IBayesNode[] zeroNodes = new IBayesNode[imax-imin+1];
		double[] lambda_prods = new double[imax-imin+1];
		for(int i = imin; i < imax; i++)
		{
			zeroNodes[i-imin+1] = null;
			lambda_prods[i-imin+1] = 1;
		}
		
		for(int i = imin; i < imax; i++)
		{
			//for(IBayesNode nd : this.incomingLambdaMessages.keySet())
			for(DiscreteParentSubscriber nd : this.ds_children)
			{
				DiscreteMessage dm = this.incomingLambdaMessages.get(this.childMap.get(nd));
				if(dm.getValue(i)!=0)	
					lambda_prods[i-imin+1] *= dm.getValue(i);
				else if(zeroNodes[i-imin+1]==null)
					zeroNodes[i-imin+1] = nd;
				else
				{
					lambda_prods[i-imin+1] = 0;
					break;
				}
			}
		}	
		for(DiscreteParentSubscriber child : this.ds_children)
		{
			DiscreteMessage from_child = this.incomingLambdaMessages.get(this.childMap.get(child));
			DiscreteMessage pi_child = new DiscreteMessage(this.cardinality);
			for(int i = imin; i < imax; i++)
			{
				double lambda_prod_local = lambda_prods[i-imin+1];
				if(lambda_prod_local > 0 && zeroNodes[i-imin+1]==null)
					lambda_prod_local /= from_child.getValue(i);
				else if(lambda_prod_local > 0 && zeroNodes[i-imin+1]!=child)
					lambda_prod_local = 0;
				pi_child.setValue(i, lambda_prod_local*this.local_pi.getValue(i));
			}
			child.handlePi(this, pi_child);
		}
	}
	
	public int getCardinality()
	{
		return this.cardinality;
	}
	
	public void clearValue()
	{
		this.observed = false;
		this.value = -1;
	}
	
	public int getValue() throws BNException
	{
		if(!this.observed)
			throw new BNException("Attempted to extract evidence from unobserved node...");
		return value;
	}
	
	public void sample() throws BNException
	{
		this.value = this.cpt.sample(new DiscreteDistribution.IntegerValueSet(this.ds_parents));
		this.observed = true;
	}
	
	public void setValue(int value) throws BNException
	{
		if(value >= this.cardinality)
			throw new BNException("Attempted to set node to have value of " + value + " where cardinality is " + cardinality);
		this.observed = true;
		this.value = value;
	}
	
	public boolean isObserved()
	{
		return this.observed;
	}
	
	
	int[] getParentDimensions()
	{
		if(parent_dims==null)
		{
			parent_dims = new int[this.ds_parents.size()];
			for(int i = 0; i < parent_dims.length; i++)
				parent_dims[i] = this.ds_parents.get(i).getCardinality();
		}
		return parent_dims;
	}
	
	private int[] parent_dims = null;
	
	public double getLogLikelihood()
	{
		return this.likelihoodGivenPast;
	}
	
	public void clearEvidence()
	{
		this.value = -1;
		this.observed = false;
	}
	
	public void updateSufficientStatistic(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof DiscreteSufficientStatistic))
			throw new BNException("Attempted to get a non-discrete sufficient statistic update froma discrete node.");
		((DiscreteSufficientStatistic)stat).update(this.local_lambda,this.incomingPiMessages);
	}
	
	public DiscreteSufficientStatistic getSufficientStatistic() throws BNException
	{
		return this.cpt.getSufficientStatisticObj().update(this.local_lambda, this.incomingPiMessages);
	}
	
	private double likelihoodGivenPast = 0;

	private boolean observed = false;
	private int value;
	private int cardinality;
	private DiscreteDistribution cpt;
	
	private DiscreteMessage local_lambda;
	private DiscreteMessage local_pi;
	
	private DiscreteMessage marginalDistribution = null;
	
	int numParents = 0;
	int numChildren = 0;
	private HashMap<IBayesNode, Integer> parentMap = new HashMap<IBayesNode, Integer>();
	private HashMap<DiscreteParentSubscriber, Integer> childMap = new HashMap<DiscreteParentSubscriber, Integer>();
	private Vector<DiscreteMessage> incomingLambdaMessages = new Vector<DiscreteMessage>();
	private Vector<DiscreteMessage> incomingPiMessages = new Vector<DiscreteMessage>();
	private Vector<DiscreteMessage> parents_local_pis = new Vector<DiscreteMessage>();
	private ArrayList<DiscreteBNNode> ds_parents = new ArrayList<DiscreteBNNode>();
	private ArrayList<DiscreteParentSubscriber> ds_children = new ArrayList<DiscreteParentSubscriber>();
	private Vector<DiscreteMessage> outgoing_lambdas= new Vector<DiscreteMessage>();

	public void optimizeParameters() throws BNException
	{
		this.cpt.optimize(this.getSufficientStatistic());
	}
	
	public void optimizeParameters(SufficientStatistic stat) throws BNException
	{
		this.cpt.optimize(stat);
	}
}
