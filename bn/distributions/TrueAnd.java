package bn.distributions;

import java.io.PrintStream;

import bn.BNException;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.interfaces.MessageSet;
import bn.messages.FiniteDiscreteMessage;

public class TrueAnd extends DiscreteFiniteDistribution {
	
	private TrueAnd(){super(2);}
	
	private static TrueAnd singleton = new TrueAnd();
	public static TrueAnd getInstance()
	{
		return singleton;
	}

	@Override
	public double optimize(SufficientStatistic obj) throws BNException {
		return 0;
	}

	@Override
	public void printDistribution(PrintStream pr) {
		pr.println("True and distribution.");
	}

	@Override
	public String getDefinition() {
		return "And()\n";
	}

	@Override
	public int sample(ValueSet<Integer> parentVals) throws BNException {
		for(int i = 0; i < parentVals.length(); i++)
			if(parentVals.getValue(i)==0)
				return 0;
		return 1;
	}

	@Override
	public DiscreteSufficientStatistic getSufficientStatisticObj() {
		return Distribution.NullDiscreteSufficientStatistic.instance();
	}

	@Override
	public TrueAnd copy() throws BNException {
		return this;
	}

	@Override
	public double evaluate(int[] indices, int value) throws BNException {
		for(int i = 0; i < indices.length; i++)
			if(indices[i]==0)
				return value==0 ? 1.0 : 0.0;
		return value==0 ? 0.0 : 1.0;
	}

	@Override
	protected void validateConditionDimensions(int[] dimensions)
			throws BNException {
		if(dimensions.length==0)
			throw new BNException("Or node has no parents!");
		for(int i = 0; i < dimensions.length; i++)
			if(dimensions[i]!=2)
				throw new BNException("TrueOr node needs only binary parents!");
	}

	@Override
	public void computeLocalPi(FiniteDiscreteMessage local_pi,
			MessageSet<FiniteDiscreteMessage> incoming_pis, Integer value)
			throws BNException {
		
		//TODO Implement!
		/*double probability_all0 = 1;
		for(DiscreteMessage parentPi : incoming_pis)
			probability_all0 *= parentPi.getValue(0)/(parentPi.getValue(0)+parentPi.getValue(1));
		local_pi.setValue(0, probability_all0);
		local_pi.setValue(1, 1-probability_all0);*/
	}
	
	static void computeLambdasS(MessageSet<FiniteDiscreteMessage> lambdas_out,
			MessageSet<FiniteDiscreteMessage> incoming_pis, FiniteDiscreteMessage local_lambda,
			Integer value) throws BNException {
		//TODO Implement!
	}

	@Override
	public void computeLambdas(MessageSet<FiniteDiscreteMessage> lambdas_out,
			MessageSet<FiniteDiscreteMessage> incoming_pis, FiniteDiscreteMessage local_lambda,
			Integer value) throws BNException {
			computeLambdasS(lambdas_out, incoming_pis, local_lambda, value);
	}
	
	static double computeH1(MessageSet<FiniteDiscreteMessage> incoming_pis, FiniteDiscreteMessage local_lambda)
	{
		// H1 = -H(X,I | Ex, Ei) = -H(X | I, Ex, Ei) - H(I|Ex,Ei)
		// = pXNotAll0*(-H(X|Ex)+H(X!=All0|Ex))-H(I|Ex,Ei)
		
		double H1;
		double p1 = 1;
		double HX = 0;

		for(int i = 0; i < incoming_pis.size(); i++)
		{
			double tmp1 = incoming_pis.get(i).getValue(0);
			double tmp2 = incoming_pis.get(i).getValue(1);
			tmp1 /= (tmp1+tmp2);
			tmp2 /= (tmp1+tmp2);

			p1 *= tmp2;

			if(tmp1 > 0)
				HX -= tmp1*Math.log(tmp1);
			if(tmp2 > 0)
				HX -= tmp2*Math.log(tmp2);
		}

		double ll0 = local_lambda.getValue(0);
		double ll1 = local_lambda.getValue(1);
		double C = p1*ll1 + (1-p1)*ll0;
		if(ll1==1 && p1==0)
			return Double.NaN;
		else if(ll0==1 && p1==1)
			return Double.NaN;

		H1 = -HX;
		if(p1 > 0)
			H1 -= p1*Math.log(p1);
		if(ll1 > 0)
			H1 += (1-p1)*(Math.log(ll0/C));
		H1 *= ll0/C;
		if(p1*ll0 > 0)
			H1 += p1*ll1/C*Math.log(p1*ll1/C);
		
		return H1;
	}
	
	static double computeH2(FiniteDiscreteMessage marginal, int numChildren)
	{
		double H2 = 0;
		double m1 = marginal.getValue(1);
		double m0 = marginal.getValue(0);
		if(m1 > 0 && m0 > 0)
		{
			H2 = m0*Math.log(m0);
			H2 += m1*Math.log(m1);
			H2 *= numChildren;
		}
		return H2;
	}

	@Override
	public double computeBethePotential(MessageSet<FiniteDiscreteMessage> incoming_pis,
			FiniteDiscreteMessage local_lambda, FiniteDiscreteMessage marginal,
			Integer value, int numChildren) throws BNException {
		
		double H1 = computeH1(incoming_pis,local_lambda);
		double H2 = computeH2(marginal,numChildren);
		// E is always 0 here
		return H1-H2;
	}
}
