package bn.distributions;

import java.util.Vector;

import bn.BNException;
import bn.messages.DiscreteMessage;

public class ScalarNoisyOr extends DiscreteDistribution
{
	
	public ScalarNoisyOr(double c) throws BNException
	{
		super(2);
		if(c < 0 || c > 1) throw new BNException("Attempted to specify noisy or with invalid c ( " + c + ")");
		this.c = c;
		this.q = 1-c;
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
		return 1-Math.pow(this.q, numActiveParents);
	}
	
	public void validateConditionDimensions(int[] dims) throws BNException
	{
		for(int i =0; i < dims.length; i++)
			if(dims[i]!=2)
				throw new BNException("Noisy-Or depends on parent with cardinality that is not 2.");
	}
	
	public double computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Vector<DiscreteMessage> parent_pis, Integer value)
	{
		double localProduct = 1;
		for(int i = 0; i < incoming_pis.size(); i++)
			localProduct *= (1-incoming_pis.get(i).getValue(1)*this.c);
		local_pi.setValue(0, localProduct);
		local_pi.setValue(1, 1-localProduct);
		if(value!=null)
			return local_pi.getValue(value);
		else
			return 0;
	}
	
	public ScalarNoisyOr copy() throws BNException
	{
		return new ScalarNoisyOr(c);
	}
	
	public void optimize(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof ScalarNoisyOrSuffStat))
			throw new BNException("Attempted to optimize noisy-or distribution with non-noisy or statistics!");
		ScalarNoisyOrSuffStat stato = (ScalarNoisyOrSuffStat)stat;
		this.q = stato.e0/(stato.e0+stato.e1);
		this.c = 1-q;
	}
	
	public static class ScalarNoisyOrSuffStat implements DiscreteSufficientStatistic
	{
		public ScalarNoisyOrSuffStat(double c)
		{
			this.c = c;
			this.q = 1-c;
			this.reset();
		}
		
		public void reset(){this.e0 = 0; this.e1 = 0;}
		
		public ScalarNoisyOrSuffStat update(SufficientStatistic stat) throws BNException
		{
			if(!(stat instanceof ScalarNoisyOrSuffStat))
				throw new BNException("Attempted to update noisy-or statistic with non-noisy-or statistics.");
			
			ScalarNoisyOrSuffStat stato = (ScalarNoisyOrSuffStat)stat;
			this.e0 += stato.e0;
			this.e1 += stato.e1;
			return this;
		}
		
		public ScalarNoisyOrSuffStat update(DiscreteMessage lambda, DiscreteMessage pi, Vector<DiscreteMessage> incomingPis)
		{
			double[] pn = this.computePN(incomingPis);
			for(int i = 0; i < incomingPis.size(); i++)
			{
				double p0 = pn[i]*Math.pow(this.q, i);
				this.e0 += p0;
				this.e1 += (1-p0);
			}
			return this;
		}
	
		// Compute the probability of number of parents being active from pi messages in quadratic
		// rather than exponential time.
		private double[] computePN(Vector<DiscreteMessage> incomingPis)
		{
			int L = incomingPis.size();
			double[] dist = new double[L+1];
			double eta = 1;
			for(int i = 0; i < L; i++)
				eta*= incomingPis.get(i).getValue(0);
			dist[0] = eta;
			
			double[] buf = new double[L];
			for(int i = 0; i < L; i++)
				buf[i] = 1;
			
			for(int i = 0; i < L; i++)
			{
				buf[L-1] *= incomingPis.get(L-1-i).getValue(1);
				for(int j = L-2; j >= 0; j--)
					buf[j] = incomingPis.get(j-i).getValue(1)*buf[j]+buf[j+1];
				dist[i+1] = buf[i]*eta;
			}
			return dist;
		}
		
		double e0, e1;
		double c, q;
	}
	
	public DiscreteSufficientStatistic getSufficientStatisticObj()
	{
		return null;
	}
	
	public void computeLambdas(Vector<DiscreteMessage> lambdas_out, Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda, Integer value) throws BNException
	{
		double localProd = 1;
		for(int i = 0; i < lambdas_out.size(); i++)
			localProd *= (1-this.c*incoming_pis.get(i).getValue(1));
		localProd *= (local_lambda.getValue(1)-local_lambda.getValue(0));
		for(int i = 0; i < lambdas_out.size(); i++)  //TODO what to do when observed???  is this okay because local_lambda(~value)=0? i think so..
		{
			lambdas_out.get(i).setValue(0, local_lambda.getValue(1) - localProd);
			lambdas_out.get(i).setValue(1, local_lambda.getValue(1) - this.q*localProd);
		}
	}
	
	private double c;
	private double q;
}