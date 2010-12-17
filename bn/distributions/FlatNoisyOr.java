package bn.distributions;

import java.io.PrintStream;
import java.util.Vector;

import bn.BNException;
import bn.distributions.ScalarNoisyOr.ScalarNoisyOrSuffStat;
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

		/**
		 * Compute the probability of the number of parents active given the evidence
		 * "above" and including the parents.
		 */
		double[] pn = ScalarNoisyOrSuffStat.computePN(incoming_pis);
		double pnsum = 0;
		for(int i = 0; i < pn.length; i++)
			pnsum += pn[i];
		for(int i = 0;i < pn.length; i++)
			pn[i] /= pnsum;
	
		/**
		 * Compute the marginal over this node and the number of active parents
		 * given the evidence below and above.
		 */
		double[][] pf = new double[pn.length][2];
		double pfsum = 0;
		for(int i = 0; i < pn.length; i++)
		{
			double ptmp = i > 0 ? c : 1-c;//this.getProbability1(i);
			pf[i][0] = pn[i]*(1-ptmp)*local_lambda.getValue(0);
			pf[i][1] = pn[i]*(ptmp)*local_lambda.getValue(1);
			pfsum += pf[i][0] + pf[i][1];
		}
		
		/**
		 * Compute both the energy term 
		 * E = \sum_{parents,thisnode} marginal(parents,thisnode | evidence) log[p(thisnode|parents)]
		 * and the portion of the H1 entropy term that corresponds to the join entropy over this node
		 * and the number of active parents.
		 */
		for(int i = 0; i < pn.length; i++)
		{
			double p1 = i > 0  ? c : 1-c;//this.getProbability1(i);
			double p0 = 1-p1;
			double pf1 = pf[i][1]/pfsum;
			double pf0 = pf[i][0]/pfsum;

			if(p0 > 0)
				E -= pf0*Math.log(p0);
			if(p1 > 0)
				E -= pf1*Math.log(p1);

			if(pf0 > 0)
				H1 += pf0*Math.log(pf0);
			if(pf1 > 0)
				H1 += pf1*Math.log(pf1);
		}
	
		/**
		 * Find the number of parents who are certainly 0 or certainly 1.  From here on out
		 * we treat less than < 1e-8 as 0 and > (1-1e-8) as 1 for stability
		 */
		double tolerance = 1e-8;
		int num0Pi = 0; //Number of parents who are certainly 1
		int num1Pi = 0; //Number of parents who are certainly 0
		for(int i = 0; i < incoming_pis.size(); i++)
		{
			if(incoming_pis.get(i).getValue(0) < tolerance)
				num0Pi++;			
			else if(incoming_pis.get(i).getValue(1) < tolerance)
				num1Pi++;
		}
		int numPiUnk = incoming_pis.size()-num0Pi-num1Pi; //Number of uncertain parents
		
		/**
		 * Compute the eta and c constants, used to calculate the entropy of the parents conditioned
		 * on evidence and the number of active parents.  This ignores both parents certainly 0 and
		 * parents certainly 1, as these don't contribute to entropy.
		 */
		double eta = 1;
		double[] c = new double[numPiUnk];
		int idx = 0;
		for(int i = 0; i < incoming_pis.size(); i++)
		{
			double pi0 = incoming_pis.get(i).getValue(0); double pi1 = incoming_pis.get(i).getValue(1);
			if(pi0 < tolerance || pi1 < tolerance)
				continue;
			
			eta *= pi0/(pi0+pi1);
			c[idx] = pi1/pi0;
			idx++;
		}
		double logEta = Math.log(eta);
		
		/**
		 * Compute R and L constant sets
		 * R[i] = the sum of the products of all subsets of set c of size i (times i factorial)
		 * L[i] = the sum of the products of all subsets of set c (times a log of one element) of size i (times i-1 factorial)
		 * factorials[i] = i!
		 */
		//TODO This appears to numerical stability issues that can be fixed as addressed.  May need to consider using some 
		// log calculations if indeed that is even possible *sigh*
		double[] R = new double[numPiUnk+1];
		double[] L = new double[numPiUnk+1];
		computeLR(c, R, L);
	
		/**
		 * Compute the portion of H1 that corresponds to the conditional entropy of the parents
		 * given the number of parents active.  Note we only iterate over the possible number of
		 * parents active given any known parent values.
		 */
		for(int i = num0Pi; i < pn.length-num1Pi; i++)
		{
			double pi = (pf[i][0]+pf[i][1])/pfsum;
			if(pi > 0)
			{
				if(i-num0Pi > 0 && i < pn.length-1)
				{
					double lstar = eta/pn[i]*((logEta-Math.log(pn[i]))*R[i-num0Pi-1]+L[i-num0Pi-1]);
					H1 += pi*lstar;
				}
			}
		}
		
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
	
	private static final void computeLR(double[] c, double[] R, double[] L)
	{
		int l = c.length;
		if(l==0)
			return;
		double[] clc = new double[l];
		double[] tmpR = new double[l];
		double[] tmpL = new double[l];
		
		for(int i = 0; i < l; i++)
			clc[i] = c[i]*Math.log(c[i]);
		
		R[0] = c[0];
		L[0] = clc[0];
		for(int i = 1; i < l; i++)
		{
			R[0] += c[i];
			L[0] += clc[i];
			R[i] =0; L[i] = 0;
		}
		for(int i = 1; i < l; i++)
		{
			for(int j = 0; j < i; j++)
			{
				tmpR[j] = R[j];
				tmpL[j] = L[j];
			}
			//for(int j = 0; j < l-1; j++)
			for(int j = l-1; j > 0; j--)
			{
				tmpR[0] -= c[j];
				//if(tmpR[0] <= 0){tmpR[0] = 0;break;}
				tmpL[0] -= clc[j];
				for(int k = 1; k < i; k++)
				{
					tmpR[k] -= c[j]*tmpR[k-1];
					tmpL[k] -= clc[j]*tmpR[k-1]+c[j]*tmpL[k-1];
				}
				//double add_in = c[j]*tmpR[i-1];
				//if(add_in/R[i] < 1e-10)
				//	break;
				//System.out.println(c[j]*tmpR[i-1]);
				if(tmpR[i-1] <= 0)
					break;
				L[i] += clc[j]*tmpR[i-1]+c[j]*tmpL[i-1];
				R[i] += c[j]*tmpR[i-1];
			}
		}
	}

	double c;  //  If any parent active... P(this is active) = c
}
