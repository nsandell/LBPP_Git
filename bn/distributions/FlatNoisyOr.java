package bn.distributions;

import java.io.PrintStream;
import java.util.Vector;

import bn.BNException;
import bn.messages.DiscreteMessage;

public class FlatNoisyOr extends DiscreteDistribution {
	
	public FlatNoisyOr(double c) throws BNException
	{
		super(2);
		this.c = c;
	}
	
	@Override
	public double optimize(SufficientStatistic obj) throws BNException {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public void printDistribution(PrintStream pr) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public String getDefinition() {
		return "FlatNoisyOr()\n"+this.c+"\n";
	}



	@Override
	public int sample(ValueSet<Integer> parentVals) throws BNException {
		boolean parentActive = false;
		for(int i = 0; i < parentVals.length(); i++)
		{
			if(parentVals.getValue(i)==1)
			{
				parentActive = true;
				break;
			}
		}
		if(parentActive)
			return (Math.random() < this .c) ? 1 : 0;
		else
			return 0;
	}



	@Override
	public DiscreteSufficientStatistic getSufficientStatisticObj() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public DiscreteDistribution copy() throws BNException {
		return new FlatNoisyOr(this.c);
	}



	@Override
	public double evaluate(int[] indices, int value) throws BNException {
		for(int i =0; i< indices.length; i++)
			if(indices[i]==1)
				return value==1 ? this.c : 1-this.c;
		return value==1 ? 0 : 1;
	}



	@Override
	public void validateConditionDimensions(int[] dimensions)
			throws BNException {
		for(int i = 0; i < dimensions.length; i++)
			if(dimensions[i]!=2)
				throw new BNException("Failed to validate conditions for flat noisy or, there is a parent not of cardinality 2!");
	}



	@Override
	public void computeLocalPi(DiscreteMessage local_pi,
			Vector<DiscreteMessage> incoming_pis, Integer value)
			throws BNException {
		double pAllP0 = 1;
		for(DiscreteMessage incPi : incoming_pis)
			pAllP0 *= incPi.getValue(0)/(incPi.getValue(0)+incPi.getValue(1));
		local_pi.setValue(0, pAllP0+(1-pAllP0)*(1-c));
		local_pi.setValue(1, (1-pAllP0)*c);
	}



	@Override
	public void computeLambdas(Vector<DiscreteMessage> lambdas_out,
			Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda,
			Integer value) throws BNException {
		int numZeros = 0;
		double pAll0 = 1;
		double[] pieces = new double[incoming_pis.size()];
		for(int i = 0; i < pieces.length; i++)
		{
			pieces[i] =  incoming_pis.get(i).getValue(0)/(incoming_pis.get(i).getValue(0)+incoming_pis.get(i).getValue(1));
			if(pieces[i] > 0)
				pAll0 *= pieces[i];
			else if(numZeros==0)
				numZeros++;
			else
			{
				numZeros = 2;
				pAll0 = 0;
				break;
			}
		}
		double ll0 = local_lambda.getValue(0)/(local_lambda.getValue(0)+local_lambda.getValue(1));
		double ll1 = 1-ll0;
		double lo1 = ll0*(1-c)+ll1*c;
		for(int i= 0; i < pieces.length; i++)
		{
			if(numZeros==0)
			{
				double pAll0NotMe = pAll0/pieces[i];
				lambdas_out.get(i).setValue(0, ll0*(pAll0NotMe+(1-c)*(1-pAll0NotMe)) + ll1*c*(1-pAll0NotMe));
				lambdas_out.get(i).setValue(1,lo1);
			}
			if(pAll0==0 || numZeros==1 && pieces[i] > 0)
			{
				lambdas_out.get(i).setValue(0, .5);
				lambdas_out.get(i).setValue(1, .5);
			}
			else if(numZeros==1 && pieces[i]==0)
			{
				lambdas_out.get(i).setValue(0, ll0*(pAll0+(1-c)*pAll0)+ll1*c*(1-pAll0));
				lambdas_out.get(i).setValue(1,lo1);
			}
		}
	}

	@Override
	public double computeBethePotential(Vector<DiscreteMessage> incoming_pis,
			DiscreteMessage local_lambda, DiscreteMessage marginal,
			Integer value, int numChildren) throws BNException {
		double E = 0, H1 = 0, H2 = 0;


		double pi0 = 1;
		double HX = 0;
		for(int i = 0; i < incoming_pis.size(); i++)
		{
			double tmp1 = incoming_pis.get(i).getValue(0);
			double tmp2 = incoming_pis.get(i).getValue(1);
			tmp1 /= (tmp1+tmp2);
			tmp2 /= (tmp1+tmp2);
			
			pi0 *= tmp1;
			
			if(tmp1 > 0)
				HX -= tmp1*Math.log(tmp1);
			if(tmp2 > 0)
				HX -= tmp2*Math.log(tmp2);
		}
		double pi1 = 1 - pi0;
	
		/**
		 * Compute the marginal over this node and the number of active parents
		 * given the evidence below and above.
		 */
		double pi0y0gE = pi0*local_lambda.getValue(0);
		double pi1y0gE = pi1*local_lambda.getValue(0)*(1-c);
		double pi1y1gE = pi1*local_lambda.getValue(1)*c;
		double norm = (pi0y0gE+pi1y0gE+pi1y1gE);
		pi0y0gE = pi0y0gE/norm;
		pi1y0gE = pi1y0gE/norm;
		pi1y1gE = pi1y1gE/norm;
		
		if(pi0y0gE > 0)
			H1 += pi0y0gE*Math.log(pi0y0gE);
		if(pi1y0gE > 0)
			H1 += pi1y0gE*Math.log(pi1y0gE);
		if(pi1y1gE > 0)
			H1 += pi1y1gE*Math.log(pi1y1gE);
		/**
		 * Compute both the energy term 
		 * E = \sum_{parents,thisnode} marginal(parents,thisnode | evidence) log[p(thisnode|parents)]
		 * and the portion of the H1 entropy term that corresponds to the join entropy over this node
		 * and the number of active parents.
		 */	
		E = -(pi1y0gE*Math.log(1-this.c)+pi1y1gE*Math.log(this.c));
	
		double HI = 0;
		if(pi0 > 0 && pi0 < 1)
			HI = -(pi0*Math.log(pi0)+pi1*Math.log(pi1));
		if(pi0!=1)
			H1 += pi1y1gE/(1-pi0)*(HX-HI);
		
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
		
		return E+H1-H2;
	}
	
	double c;  //  If any parent active... P(this is active) = c
}
