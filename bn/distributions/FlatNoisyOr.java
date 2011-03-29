package bn.distributions;

import java.io.PrintStream;

import bn.BNException;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;

public class FlatNoisyOr extends DiscreteFiniteDistribution {
	
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
		pr.println("Flat Noisy Or: Activation Parameter = " + this.c);
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
	public int sample(ValueSet<Integer> parentVals, FiniteDiscreteMessage lambda) throws BNException {
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
		{
			double pact = this.c*lambda.getValue(1);
			pact = pact/(pact+(1-this.c)*lambda.getValue(0));
			return (Math.random() < pact) ? 1 : 0;
		}
		else
			return 0;
	}
	
	@Override
	public DiscreteSufficientStatistic getSufficientStatisticObj() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public FlatNoisyOr copy() throws BNException {
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
		if(dimensions.length==0)
			throw new BNException("Flat Noisy-Or node has no parents!");
		for(int i = 0; i < dimensions.length; i++)
			if(dimensions[i]!=2)
				throw new BNException("Failed to validate conditions for flat noisy or, there is a parent not of cardinality 2!");
	}



	@Override
	public void computeLocalPi(FiniteDiscreteMessage local_pi,
			MessageSet<FiniteDiscreteMessage> incoming_pis)
			throws BNException {
		double pAllP0 = 1;
		for(FiniteDiscreteMessage incPi : incoming_pis)
			pAllP0 *= incPi.getValue(0)/(incPi.getValue(0)+incPi.getValue(1));
		local_pi.setValue(0, pAllP0+(1-pAllP0)*(1-c));
		local_pi.setValue(1, (1-pAllP0)*c);
	}



	@Override
	public void computeLambdas(MessageSet<FiniteDiscreteMessage> lambdas_out,
			MessageSet<FiniteDiscreteMessage> incoming_pis, FiniteDiscreteMessage local_lambda,
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
			else if(pAll0==0 || numZeros==1 && pieces[i] > 0)
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
	public double computeBethePotential(MessageSet<FiniteDiscreteMessage> incoming_pis,
			FiniteDiscreteMessage local_lambda, FiniteDiscreteMessage marginal,
			Integer value, int numChildren) throws BNException {
		double E = 0, H1 = 0, H2 = 0;


		double p0 = 1;
		double HX = 0;
		for(int i = 0; i < incoming_pis.size(); i++)
		{
			double tmp1 = incoming_pis.get(i).getValue(0);
			double tmp2 = incoming_pis.get(i).getValue(1);
			tmp1 /= (tmp1+tmp2);
			tmp2 /= (tmp1+tmp2);
			
			p0 *= tmp1;
			
			if(tmp1 > 0)
				HX -= tmp1*Math.log(tmp1);
			if(tmp2 > 0)
				HX -= tmp2*Math.log(tmp2);
		}
		double pN0 = 1 - p0;
		double q = 1-c;
		
	
		/**
		 * Compute the marginal over this node and the number of active parents
		 * given the evidence below and above.
		 */
		double pi0y0gE = p0*local_lambda.getValue(0);
		double pi1y0gE = pN0*local_lambda.getValue(0)*q;
		double pi1y1gE = pN0*local_lambda.getValue(1)*c;
		double norm = (pi0y0gE+pi1y0gE+pi1y1gE);
		pi0y0gE = pi0y0gE/norm;
		pi1y0gE = pi1y0gE/norm;
		pi1y1gE = pi1y1gE/norm;
		E = -(pi1y0gE*Math.log(q)+pi1y1gE*Math.log(this.c));
		
		double C = (p0+pN0*q)*local_lambda.getValue(0) + pN0*this.c*local_lambda.getValue(1);
		double ll0 = local_lambda.getValue(0);
		double ll1 = local_lambda.getValue(1);
		
		if(p0==1 && ll0==0)
			return Double.NaN;
		
		double HXM = -HX;
		if(p0 > 0)
			HXM -= p0*Math.log(p0);
		if(p0*ll0 > 0)
			H1 += p0*ll0*Math.log(p0*ll0/C);
		if(q*ll0 > 0)
			H1 += q*ll0*(HXM+(1-p0)*Math.log(q*ll0/C));
		if(this.c*ll1 > 0)
			H1 += this.c*ll1*(HXM+(1-p0)*Math.log(this.c*ll1/C));
		
		H1 /= C;
		
		/**
		 * Compute H2, the negative marginal entropy of this node times factor q
		 * where q is the number of children (because this node must have parents).
		 */
		double m0 = marginal.getValue(0);
		double m1 = marginal.getValue(1);
		if(m0 > 0 && m1 > 0)
		{
			H2 = m0*Math.log(m0);
			H2 += m1*Math.log(m1);
			H2 *= numChildren;
		}
		
		return E+H1-H2;
	}
	
	double c;  //  If any parent active... P(this is active) = c
}
