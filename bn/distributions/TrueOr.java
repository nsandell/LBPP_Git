package bn.distributions;

import java.io.PrintStream;
import java.util.Vector;

import bn.BNException;
import bn.messages.DiscreteMessage;

public class TrueOr extends DiscreteDistribution {
	
	private TrueOr(){super(2);}
	
	private static TrueOr singleton = new TrueOr();
	public static TrueOr getInstance()
	{
		return singleton;
	}

	@Override
	public double optimize(SufficientStatistic obj) throws BNException {
		return 0;
	}

	@Override
	public void printDistribution(PrintStream pr) {
		pr.println("True or distribution.");
	}

	@Override
	public String getDefinition() {
		return "Or()\n";
	}

	@Override
	public int sample(ValueSet<Integer> parentVals) throws BNException {
		for(int i = 0; i < parentVals.length(); i++)
			if(parentVals.getValue(i)==1)
				return 1;
		return 0;
	}

	@Override
	public DiscreteSufficientStatistic getSufficientStatisticObj() {
		return null; //TODO I think it can be null but check.
	}

	@Override
	public DiscreteDistribution copy() throws BNException {
		return this;
	}

	@Override
	public double evaluate(int[] indices, int value) throws BNException {
		for(int i = 0; i < indices.length; i++)
			if(indices[i]==1)
				return value==1 ? 1.0 : 0.0;
		return value==1 ? 0.0 : 1.0;
	}

	@Override
	public void validateConditionDimensions(int[] dimensions)
			throws BNException {
		if(dimensions.length==0)
			throw new BNException("Or node has no parents!");
		for(int i = 0; i < dimensions.length; i++)
			if(dimensions[i]!=2)
				throw new BNException("TrueOr node needs only binary parents!");
	}

	@Override
	public void computeLocalPi(DiscreteMessage local_pi,
			Vector<DiscreteMessage> incoming_pis, Integer value)
			throws BNException {
		
		double probability_all0 = 1;
		for(DiscreteMessage parentPi : incoming_pis)
			probability_all0 *= parentPi.getValue(0)/(parentPi.getValue(0)+parentPi.getValue(1));
		local_pi.setValue(0, probability_all0);
		local_pi.setValue(1, 1-probability_all0);
	}

	@Override
	public void computeLambdas(Vector<DiscreteMessage> lambdas_out,
			Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda,
			Integer value) throws BNException {
		
		double localProd = 1; //like scalar noisy or with c = 1 and q = 0
		int numZeros = 0;
		double[] pieces = new double[lambdas_out.size()];
		for(int i = 0; i < lambdas_out.size(); i++)
		{
			pieces[i] = (1-incoming_pis.get(i).getValue(1)/(incoming_pis.get(i).getValue(0)+incoming_pis.get(i).getValue(1)));
			if(numZeros==1 && pieces[i]==0)
			{
				numZeros = 2;
				localProd = 0;
				break;
			}
			else if(pieces[i]==0)
				numZeros = 1;
			else
				localProd *= pieces[i];
		}
		double ll1 = local_lambda.getValue(1);
		localProd *= (ll1-local_lambda.getValue(0));
		for(int i = 0; i < lambdas_out.size(); i++)
		{
			if(numZeros==0)
			{
				lambdas_out.get(i).setValue(0, ll1 - localProd/pieces[i]);
				lambdas_out.get(i).setValue(1, ll1);
			}
			else if(numZeros==1 && pieces[i]==0)
			{
				lambdas_out.get(i).setValue(0, ll1 - localProd);
				lambdas_out.get(i).setValue(1, ll1);
			}
			else if(numZeros==2 || numZeros==1 && pieces[i]>0)
			{
				lambdas_out.get(i).setValue(0, ll1);
				lambdas_out.get(i).setValue(1, ll1);
			}
		}
	}

	@Override
	public double computeBethePotential(Vector<DiscreteMessage> incoming_pis,
			DiscreteMessage local_lambda, DiscreteMessage marginal,
			Integer value, int numChildren) throws BNException {
		double H1 = 0, H2 = 0;
		// E is always 0 here
		
		double pAll0 = 1;
		
		for(int i = 0; i < incoming_pis.size(); i++)
		{
			double tmp1 = incoming_pis.get(i).getValue(0);
			double tmp2 = incoming_pis.get(i).getValue(1);
			tmp1 /= (tmp1+tmp2);
			tmp2 /= (tmp1+tmp2);
			
			pAll0 *= tmp1;
			
			if(tmp1 > 0)
				H1 += tmp1*Math.log(tmp1);
			if(tmp2 > 0)
				H1 += tmp2*Math.log(tmp2);
		}
		
		if(pAll0==1 && value!=null && value==1)
			return Double.NaN;
		else if(pAll0==0 && value!=null && value==0)
			return Double.NaN;
		
		/**
		 * Compute H2, the negative marginal entropy of this node times factor q
		 * where q is the number of children (because this node must have parents).
		 */
		double ll0 = marginal.getValue(0);
		double ll1 = marginal.getValue(1);
		if(ll0 > 0 && ll1 > 0)
		{
			H2 = ll0*Math.log(ll0);
			H2 += ll1*Math.log(ll1);
			H2*=numChildren;
		}
		
		return H1-H2;
	}

}
