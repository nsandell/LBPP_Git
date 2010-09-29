package bn.distributions;

import java.util.Vector;

import bn.BNException;
import bn.messages.DiscreteMessage;

public class NoisyOr extends DiscreteDistribution
{
	public NoisyOr(double p) throws BNException
	{
		super(2); // We will consider this to mean (from 0 -> Infty)
		if(p < 0 || p > 1) throw new BNException("Attempted to specify noisy or with invalid p ( " + p + ")");
		this.p = 1-p;
	}
	
	public double evaluate(int[] indices, int value)
	{
		int numact = 0;
		for(int i = 0; i< indices.length; i++)
			numact += indices[i];
		if(value==1)
			return getProbability1(numact);
		else
			return 1-getProbability1(numact);
	}
	
	double getProbability1(int numActiveParents)
	{
		return 1-Math.pow(p, numActiveParents);
	}
	
	public void validateConditionDimensions(int[] dims) throws BNException
	{
		for(int i =0; i < dims.length; i++)
			if(dims[i]!=2)
				throw new BNException("Noisy-Or depends on parent with cardinality that is not 2.");
	}
	
	public double computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Vector<DiscreteMessage> parent_pis, Integer value) throws BNException
	{
		//TODO Replace this with more efficient.. look up Pearl's book
		int[] parentDim = new int[incoming_pis.size()];
		for(int i= 0; i < parentDim.length; i++)
			parentDim[i] = 2;
		boolean observed = value!=null;
		double ll = 0;
		int[] indices = initialIndices(parentDim);
		do
		{
			double tmp = 1;
			double observation_ll_tmp = 1;
			for(int j = 0; j < indices.length; j++)
			{
				tmp *= incoming_pis.get(j).getValue(indices[j]);
				if(observed)
					observation_ll_tmp *= parent_pis.get(j).getValue(indices[j]);
			}
			for(int i = 0; i < this.getCardinality(); i++)
				local_pi.setValue(i, local_pi.getValue(i)+tmp*this.evaluate(indices, i));
			if(observed)
				ll += observation_ll_tmp*this.evaluate(indices, value);
		}
		while((indices = DiscreteDistribution.incrementIndices(indices, parentDim))!=null);
		local_pi.normalize();
		return ll;
	}
	
	public void computeLambdas(Vector<DiscreteMessage> lambdas_out, Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda, Integer value) throws BNException
	{
		//Get round to this later
	}
	
	private double p;
}