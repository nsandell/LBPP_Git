package bn.distributions;

import java.io.PrintStream;
import java.util.Vector;

import util.MathUtil;

import bn.BNException;
import bn.messages.DiscreteMessage;

/**
 * A probability vector - a CPT with non conditioning variables.
 * @author Nils F. Sandell
 */
public class DiscreteCPTUC extends DiscreteDistribution
{
	/**
	 * Create a probability vector
	 * @param distr They probability values.
	 * @throws BNException
	 */
	public DiscreteCPTUC(double[] distr) throws BNException
	{
		super(distr.length);
		this.dist = distr.clone();
		this.validate();
	}

	/**
	 * Validate this probability vector
	 * @throws BNException
	 */
	private final void validate() throws BNException
	{
		double sum = 0;
		for(int i = 0; i < dist.length; i++)
		{
			if(dist[i] < 0 || dist[i] > 1)
				throw new BNException("Attempted to create pdist with invalid entry (" + dist[i] + ")");
			sum += dist[i];
		}

		if(Math.abs(sum-1) > 1e-12)
			throw new BNException("Attempted to create unnormalized pdist.");
	}

	@Override
	public int sample(ValueSet<Integer> parents) throws BNException
	{
		double val = MathUtil.rand.nextDouble();
		double sum = 0;
		for(int i = 0; i < dist.length; i++)
		{
			sum += dist[i];
			if(val < sum)
				return i;
		}
		return dist.length-1;
	}
	
	@Override
	public DiscreteCPTUC copy() throws BNException
	{
		double[] newdist = new double[this.dist.length];
		for(int i = 0; i < newdist.length; i++)
			newdist[i] = this.dist[i];
		return new DiscreteCPTUC(newdist);
	}
	
	@Override
	public void print(PrintStream ps)
	{
		ps.println("PV("+this.getCardinality()+")");
		ps.print(dist[0]);
		for(int i = 1; i < this.dist.length; i++)
			ps.print(" " + dist[i]);
		ps.println();
	}

	/**
	 * Get a uniform distribution probability vector.
	 * @param The cardinality of the vector
	 * @return Uniform distribution
	 * @throws BNException Cardinality <= 0
	 */
	public static DiscreteCPTUC uniform(int cardinality) throws BNException
	{
		if(cardinality <= 0)
			throw new BNException("Attempted to get uniform distribution with cardinality <= 0");
		double[] uniform = new double[cardinality];
		double value = ((double)1)/((double)cardinality);
		for(int i = 0; i < cardinality; i++)
			uniform[i] = value;
		return new DiscreteCPTUC(uniform);
	}
	
	@Override
	public void validateConditionDimensions(int[] dims) throws BNException
	{
		if(dims.length!=0)
			throw new BNException("Probability vector should not have conditions..");
	}

	@Override
	public double evaluate(int[] indices, int value) throws BNException
	{
		if(indices.length!=0)
			throw new BNException("Passed conditions into unconditional distribution.");
		return dist[value];
	}
	
	@Override
	public void computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Integer value) throws BNException
	{
		for(int i = 0; i < local_pi.getCardinality(); i++)
			local_pi.setValue(i, dist[i]);
	}
	
	@Override
	public double optimize(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof UDSuffStat))
			throw new BNException("Attempted to optimized probability vector with non probability vector statistics.");
		double maxdiff = 0;
		UDSuffStat stato = (UDSuffStat)stat; 
		for(int i = 0; i < stato.expected_data.length; i++)
		{
			double newval = stato.expected_data[i]/stato.expected_sum;
			maxdiff = Math.max(Math.abs(newval-dist[i]), maxdiff);
			this.dist[i] = newval; 
		}
		return maxdiff;
	}
	
	public void printDistribution(PrintStream ps)
	{
		ps.print("Probability Vector: [");
		ps.print(dist[0]);
		for(int i = 1; i < this.getCardinality(); i++)
			ps.print(" " + dist[i]);
		ps.println("]");
	}
	
	@Override
	public UDSuffStat getSufficientStatisticObj()
	{
		return new UDSuffStat(this.getCardinality(),this);
	}
	
	/**
	 * Sufficient statistic for a probability vector
	 * @author Nils F. Sandell
	 */
	public static class UDSuffStat implements DiscreteSufficientStatistic
	{
		public UDSuffStat(int len, DiscreteCPTUC cpt)
		{
			this.expected_data = new double[len];
			this.current = new double[len];
			this.reset();
			this.cpt = cpt;
		}
		
		@Override
		public void reset()
		{
			for(int i = 0; i < this.expected_data.length; i++)
				expected_data[i] = 0;
			this.expected_sum = 0;
		}
		
	
		@Override
		public DiscreteSufficientStatistic update(SufficientStatistic stat) throws BNException
		{
			if(!(stat instanceof UDSuffStat))
				throw new BNException("Expected unconditioned sufficient statistic...");
			UDSuffStat udstat = (UDSuffStat)stat;
			if(udstat.expected_data.length!=this.expected_data.length)
				throw new BNException("Attempted to merge incompatible unconditioned discrete distribution sufficient statistics.");
			
			for(int i= 0; i < this.expected_data.length; i++)
				this.expected_data[i] += udstat.expected_data[i];
			return this;
		}

		@Override
		public DiscreteSufficientStatistic update(DiscreteMessage lambda,
				Vector<DiscreteMessage> parent_pis) throws BNException {
			double sum = 0;
			for(int i = 0; i < expected_data.length; i++)
			{
				current[i] = lambda.getValue(i)*this.cpt.dist[i];
				sum += lambda.getValue(i)*this.cpt.dist[i];
			}
			for(int i = 0; i < expected_data.length; i++)
			{
				this.expected_data[i] += current[i]/sum;
				this.expected_sum += current[i]/sum;
			}
			return this;
		}
		
		private DiscreteCPTUC cpt;
		private double[] expected_data;
		private double expected_sum;
		private double[] current;
	}
	
	@Override
	public double computeBethePotential(Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda,
										DiscreteMessage marginal, Integer value, int numChildren) throws BNException {
		double E = 0, H1 = 0, H2 = 0;
		if(value!=null)
			E = -Math.log(this.dist[value]);
		if(numChildren==0)
			return E;
		if(value==null)
		{
			for(int i = 0; i < this.getCardinality(); i++)
			{
				if(this.dist[i] > 0)
					E -= local_lambda.getValue(i)*Math.log(dist[i]);
				if(marginal.getValue(i) > 0)
					H2 += marginal.getValue(i)*Math.log(marginal.getValue(i));
			}
			H2*=(numChildren-1);
		}
		return E+H1-H2;
	}
	
	@Override //Should have no parents so this method has no functionality.
	public void computeLambdas(Vector<DiscreteMessage> lambdas_out, Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda, Integer value) throws BNException{}
	
	private static final long serialVersionUID = 50L;
	private final double[] dist;

}