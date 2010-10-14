package bn.distributions;

import java.io.PrintStream;
import java.util.Vector;

import util.MathUtil;

import bn.BNException;
import bn.messages.DiscreteMessage;

/**
 * Implements a 'scalar' noisy or CPD.  That is, a noisy or node
 * where all activiation (inhibition) effects from parents are
 * identical, described by space c (activation probability)
 * i.e. p(this=1 | one and only one parent=1)=c
 * Note this is only valid for binary nodes with binary parents
 * @author Nils F. Sandell
 */
public class ScalarNoisyOr extends DiscreteDistribution
{
	
	/**
	 * Create a scalar noisy or node with activation parameter c
	 * @param c Activation parameter (i.e. p(this=1 | one and only one parent=1)=c)
	 * @throws BNException If c < 0 or c > 1
	 */
	public ScalarNoisyOr(double c) throws BNException
	{
		super(2);
		if(c < 0 || c > 1) throw new BNException("Attempted to specify noisy or with invalid c ( " + c + ")");
		this.c = c;
		this.q = 1-c;
	}
	
	@Override
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
	
	@Override
	public int sample(ValueSet<Integer> parents)  throws BNException
	{
		int num1 = 0;
		for(int i= 0; i < parents.length(); i++)
			if(parents.getValue(i)==1)
				num1++;
		
		return (MathUtil.rand.nextDouble() < getProbability1(num1)) ? 1 : 0;
	}
	
	/**
	 * Get the probability this node is active given the number of active parents.
	 * @param numActiveParents the number of active parents.
	 * @return Probability this node is active.
	 */
	double getProbability1(int numActiveParents)
	{
		return 1-Math.pow(this.q, numActiveParents);
	}
	
	@Override
	public void validateConditionDimensions(int[] dims) throws BNException
	{
		for(int i =0; i < dims.length; i++)
			if(dims[i]!=2)
				throw new BNException("Noisy-Or depends on parent with cardinality that is not 2.");
	}
	
	@Override
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
	
	@Override
	public ScalarNoisyOr copy() throws BNException
	{
		return new ScalarNoisyOr(c);
	}
	
	@Override //TODO Validate this approach, I think it may be a heuristic
	public double optimize(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof ScalarNoisyOrSuffStat))
			throw new BNException("Attempted to optimize noisy-or distribution with non-noisy or statistics!");
		ScalarNoisyOrSuffStat stato = (ScalarNoisyOrSuffStat)stat;
		this.q = 0;
		double normfac = stato.n-stato.pns.get(0).px0;
		for(int i = 1; i < stato.pns.size(); i++)
		{
			double N0 = stato.pns.get(i).px0;
			double N = N0+stato.pns.get(i).px1;
			this.q += N/normfac*Math.pow(N0/N, 1/((double)i));
		}
		double change = Math.abs(this.c-(1-q));
		this.c = 1-q;
		return change;
	}
	
	public void printDistribution(PrintStream ps)
	{
		ps.println("Scalar Noisy Or: Activation Parameter = " + this.c);
	}
	
	/**
	 * Sufficient statistic for scalar noisy or class
	 * @author Nils F. Sandell
	 */
	public static class ScalarNoisyOrSuffStat implements DiscreteSufficientStatistic
	{
		public ScalarNoisyOrSuffStat(ScalarNoisyOr cpt)
		{
			this.cpt = cpt;
			this.reset();
		}
		
		@Override
		public void reset(){this.pns.clear();}
		
		@Override
		public ScalarNoisyOrSuffStat update(SufficientStatistic stat) throws BNException
		{
			if(!(stat instanceof ScalarNoisyOrSuffStat))
				throw new BNException("Attempted to update noisy-or statistic with non-noisy-or statistics.");
	
			ScalarNoisyOrSuffStat stato = (ScalarNoisyOrSuffStat)stat;
			for(int i = 0; i < stato.pns.size(); i++)
			{
				if(this.pns.size() <= i)
					this.pns.add(new PXGN());
				this.pns.get(i).px0 += stato.pns.get(i).px0;
				this.pns.get(i).px1 += stato.pns.get(i).px1;
			}
			this.n += stato.n;
			return this;
		}
		
		@Override
		public ScalarNoisyOrSuffStat update(DiscreteMessage lambda, Vector<DiscreteMessage> incomingPis)
		{
			double[] pn = this.computePN(incomingPis);
			double[][] curr = new double[incomingPis.size()+1][2];
	
			double total = 0;
			for(int i = 0; i < incomingPis.size()+1; i++)
			{
				double fac = Math.pow(this.cpt.q, i);
				curr[i][0] = pn[i]*fac*lambda.getValue(0);
				curr[i][1] = pn[i]*(1-fac)*lambda.getValue(1);
				total += (curr[i][0]+curr[i][1]);
				if(this.pns.size() <= i)
					this.pns.add(new PXGN());
			}
			for(int i = 0; i < incomingPis.size()+1; i++)
			{
				this.pns.get(i).px0 += curr[i][0]/total;
				this.pns.get(i).px1 += curr[i][1]/total;
			}
			this.n++;
			return this;
		}
	
		// Compute the probability of number of parents being active from pi messages in quadratic
		// rather than exponential time.
		private double[] computePN(Vector<DiscreteMessage> incomingPis)
		{
			int L = incomingPis.size();
			double[] dist = new double[L+1];
			double[] p = new double[L];
			double eta = 1;
			for(int i = 0; i < L; i++)
			{
				p[i] = incomingPis.get(i).getValue(1)/incomingPis.get(i).getValue(0);
				eta*= incomingPis.get(i).getValue(0);
			}
			dist[0] = eta;
			
			double[] buf = new double[L];
			for(int i = 0; i < L; i++)
				buf[i] = 1;
			
			for(int i = 0; i < L; i++)
			{
				buf[L-1] *= p[L-1-i];
				for(int j = L-2; j >= i; j--)
					buf[j] = p[j-i]*buf[j]+buf[j+1];
				dist[i+1] = buf[i]*eta;
			}
			return dist;
		}
		
		private static class PXGN
		{
			public double px0 = 0;
			public double px1 = 0;
		}
	
		int n;
		Vector<PXGN> pns = new Vector<PXGN>();
		ScalarNoisyOr cpt;
	}
	
	@Override
	public DiscreteSufficientStatistic getSufficientStatisticObj()
	{
		return new ScalarNoisyOrSuffStat(this);
	}
	
	@Override
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