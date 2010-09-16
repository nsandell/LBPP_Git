package bn;

import java.util.ArrayList;
import java.util.HashMap;

import bn.BayesNet.BNException;
import bn.distributions.DiscreteDistribution;
import bn.messages.DiscreteMessage;
import bn.nodeInterfaces.BNNodeI;
import bn.nodeInterfaces.DiscreteChildSubscriber;
import bn.nodeInterfaces.DiscreteParentSubscriber;

public class DiscreteBNNode extends BNNode implements DiscreteParentSubscriber, DiscreteChildSubscriber
{
	
	DiscreteBNNode(int cardinality){this.cardinality = cardinality;}
	
	public void validate() throws BNException
	{
		if(this.cpt==null) throw new BNException("Error while validating, no CPT set!");
		int[] dimensions = this.cpt.getConditionDimensions();
		if(dimensions.length!=this.parents.size())
			throw new BNException("Error while validating, CPT has incorrect number of conditions.");
		for(int i = 0; i < this.parents.size(); i++)
			if(((DiscreteBNNode)this.parents.get(i)).cardinality!=dimensions[i])
				throw new BNException("Error while validating, CPT dimension " + i + " is of incorrect size.");
	}
	
	protected void addParentI(BNNodeI parent) throws BNException
	{
		if(!(parent instanceof DiscreteBNNode))
			throw new BNException("Parent of discrete node must also be a discrete node.");
		this.ds_parents.add((DiscreteBNNode)parent);
	}
	
	protected void removeParentI(BNNodeI parent) throws BNException
	{
		if(!(parent instanceof DiscreteBNNode))
			throw new BNException("Attempted to remove a parent from discrete node that isn't discrete!");
		this.ds_parents.remove((DiscreteBNNode)parent);
	}
	
	protected void addChildI(BNNodeI child) throws BNException
	{
		if(!(child instanceof DiscreteParentSubscriber))
			throw new BNException("Child of discrete node must be able to handle discrete parents.");
		this.ds_children.add((DiscreteParentSubscriber)child);
	}
	
	protected void removeChildI(BNNodeI child) throws BNException
	{
		if(!(child instanceof DiscreteParentSubscriber))
			throw new BNException("Attempted to remove child from parent who could never have been a child!");
		this.ds_children.remove((DiscreteParentSubscriber)child);
	}
	
	public void setDistribution(DiscreteDistribution distribution) throws BNException
	{
		if(distribution.getCardinality()!=this.cardinality)
			throw new BNException("Attempted to set a CPT of incorrect cardinality!");
		this.cpt = distribution;
	}

	@Override
	public void handleLambda(BNNodeI child, DiscreteMessage dm)
	{
		this.incomingLambdaMessages.put(child, dm);
	}

	@Override
	public void handlePi(BNNodeI parent, DiscreteMessage dm)
	{
		this.incomingPiMessages.put(parent, dm);
	}

	@Override
	public void sendInitialMessages()
	{
		this.local_lambda = new DiscreteMessage(this.cardinality);
		this.local_pi = new DiscreteMessage(this.cardinality);
		for(int i = 0; i < this.cardinality; i++)
		{
			this.local_lambda.setValue(i, 0);
			this.local_pi.setValue(i, 0);
		}
		for(DiscreteBNNode parent : ds_parents)
			parent.handleLambda(this, DiscreteMessage.allOnesMessage(parent.cardinality));
		for(DiscreteParentSubscriber child : ds_children)
			child.handlePi(this, DiscreteMessage.allOnesMessage(this.cardinality));
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
			double tmp = 1;
			for(DiscreteParentSubscriber child : this.ds_children)
				tmp *= this.incomingLambdaMessages.get(child).getValue(this.value);
			this.local_lambda.setValue(this.value,tmp);
		}
		else
		{
			for(int i = 0; i < this.cardinality; i++)
			{
				double tmp = 1;
				for(DiscreteParentSubscriber child : this.ds_children)
					tmp *= this.incomingLambdaMessages.get(child).getValue(i);
				this.local_lambda.setValue(i,tmp);
			}
		}
		
		for(int i = 0; i < this.cardinality; i++)
			this.local_pi.setValue(i, 0);
		
		int[] indices = this.cpt.initialIndices();
		int[] dims = this.cpt.getConditionDimensions();
		do
		{
			double tmp = 1;
			for(int j = 0; j < this.ds_parents.size(); j++)
				tmp *= this.incomingPiMessages.get(this.ds_parents.get(j)).getValue(indices[j]);
			for(int i = 0; i < this.cardinality; i++)
				this.local_pi.setValue(i, this.local_pi.getValue(i)+tmp*this.cpt.evaluate(indices, i));
		}
		while((indices = DiscreteDistribution.incrementIndices(indices, dims))!=null);
		
		this.local_lambda.normalize();
		this.local_pi.normalize();
	}
	
	protected void updateLambdas() throws BNException
	{
		int[] indices = this.cpt.initialIndices();
		DiscreteMessage[] dms = new DiscreteMessage[this.ds_parents.size()];
		int[] sizes = this.cpt.getConditionDimensions();
		for(int i = 0; i < dms.length; i++)
			dms[i] = new DiscreteMessage(sizes[i]);
		do
		{
			double pi_product = 1;
			DiscreteBNNode zeroParent = null;
			for(int i = 0; i < indices.length; i++)
			{
				double value = this.incomingPiMessages.get(this.parents.get(i)).getValue(indices[i]);
				if(value==0 && zeroParent==null)
					zeroParent = this.ds_parents.get(i);
				else if(value==0){pi_product = 0;break;}
				else
					pi_product *= value;
			}
			
			for(int i = 0; i < this.cardinality; i++)
			{
				double p = this.cpt.evaluate(indices, i);

				for(int j = 0; j < indices.length; j++)
				{
					double local_pi_product = pi_product;
					if(local_pi_product > 0 && zeroParent==null)
						local_pi_product /= this.incomingPiMessages.get(this.parents.get(j)).getValue(indices[j]);
					
					dms[j].setValue(indices[j], dms[j].getValue(indices[j]) + p*local_pi_product*this.local_lambda.getValue(i));
				}
			}
		}
		while((indices = DiscreteDistribution.incrementIndices(indices, cpt.getConditionDimensions()))!=null);
		
		for(int i = 0; i < this.ds_parents.size(); i++)
			this.ds_parents.get(i).handleLambda(this, dms[i]);
	}
	
	protected void updatePis()
	{
		int imin = 0; int imax = this.cardinality;
		if(this.observed){imin = this.value; imax = this.value+1;}
		
		BNNodeI[] zeroNodes = new BNNodeI[imax-imin+1];
		double[] lambda_prods = new double[imax-imin+1];
		for(int i = imin; i < imax; i++)
		{
			zeroNodes[i-imin+1] = null;
			lambda_prods[i-imin+1] = 1;
		}
		
		for(int i = imin; i < imax; i++)
		{
			for(BNNodeI nd : this.incomingLambdaMessages.keySet())
			{
				DiscreteMessage dm = this.incomingLambdaMessages.get(nd);
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
			DiscreteMessage from_child = this.incomingLambdaMessages.get(child);
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
	
	public int getValue() throws BNException
	{
		if(!this.observed)
			throw new BNException("Attempted to extract evidence from unobserved node...");
		return value;
	}
	
	public void setValue(int value) throws BNException
	{
		if(value >= this.cardinality)
			throw new BNException("Attempted to set node to have value of " + value + " where cardinality is " + cardinality);
		this.observed = true;
		this.value = value;
	}

	private boolean observed = false;
	private int value;
	private int cardinality;
	private DiscreteDistribution cpt;
	
	private DiscreteMessage local_lambda;
	private DiscreteMessage local_pi;
	
	private DiscreteMessage marginalDistribution = null;
	
	ArrayList<DiscreteBNNode> ds_parents = new ArrayList<DiscreteBNNode>();
	ArrayList<DiscreteParentSubscriber> ds_children = new ArrayList<DiscreteParentSubscriber>();
	HashMap<BNNodeI, DiscreteMessage> incomingLambdaMessages = new HashMap<BNNodeI, DiscreteMessage>();
	HashMap<BNNodeI, DiscreteMessage> incomingPiMessages = new HashMap<BNNodeI, DiscreteMessage>();
}
