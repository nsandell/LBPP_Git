package bn.distributions;

import java.io.PrintStream;
import java.util.Vector;
import bn.BNException;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;

/**
 * This distribution is deterministic - given a set of boolean parents,
 * the value produced by this distribution of the number of true parents.
 * @author Nils F. Sandell
 */
public class CountDistribution extends DiscreteDistribution
{
	private CountDistribution(){}
	private static CountDistribution singleton = null;
	public static CountDistribution getSingleton()
	{
		if(singleton==null)
			singleton = new CountDistribution();
		return singleton;
	}

	//Nothing to optimize...
	@Override
	public double optimize(SufficientStatistic obj) throws BNException {return 0;}

	@Override
	public void printDistribution(PrintStream pr) {
		pr.println("Counting distribution..");
	}

	@Override
	public String getDefinition() {
		return "Count()";
	}

	@Override
	public int sample(ValueSet<Integer> parentVals) throws BNException {
		int sum = 0;
		for(int i = 0; i < parentVals.length(); i++)
			sum += (parentVals.getValue(i)==1 ? 1 : 0);
		return sum;
	}

	@Override
	public DiscreteSufficientStatistic getSufficientStatisticObj() {
		return null;
	}

	@Override
	public DiscreteDistribution copy() throws BNException {
		return this;
	}

	@Override
	public double evaluate(int[] indices, int value) throws BNException {
		double sum = 0;
		for(int index : indices)
			if(index==1)
				sum++;
		if(value==sum)
			return 1;
		else return 0;
	}

	@Override
	public void validateConditionDimensions(int[] dimensions)
			throws BNException {
		for(int dim : dimensions)
			if(dim!=2)
				throw new BNException("Parent of a count distribution not of size 2!");
	}

	public void computeLocalPi(FiniteDiscreteMessage local_pi,
			MessageSet<FiniteDiscreteMessage> incoming_pis, Integer value)
			throws BNException {
		
		int L = incoming_pis.size();
		if(local_pi.getCardinality()!=L+1)
			local_pi.adjustCardinality(L+1);
		
		//Changing this method to treat any number < 1e-8 as 0 for numerical stability issue
		Vector<FiniteDiscreteMessage> incPis2 = new Vector<FiniteDiscreteMessage>();
		int numZero = 0;
		for(int i = 0; i < L; i++)
		{
			if(incoming_pis.get(i).getValue(0) < 1e-8)
				numZero++;
			else
				incPis2.add(incoming_pis.get(i));
		}
		if(numZero==L)
		{
			FiniteDiscreteMessage ret = new FiniteDiscreteMessage(L+1);
			ret.setValue(L, 1-1e-8);
			for(int i = 0; i < L; i++)
				ret.setValue(i, 1e-8/L);
			return;
		}
		if(numZero > 0)
		{
			FiniteDiscreteMessage local_pi_tmp = new FiniteDiscreteMessage(L-numZero);
			this.computeLocalPi(local_pi_tmp, incoming_pis, value);
			local_pi.empty();
			for(int i = numZero; i < local_pi_tmp.getCardinality(); i++)
				local_pi.setValue(i, local_pi_tmp.getValue(i-numZero));
			return;
		}
		local_pi.empty();
		double[] p = new double[L];
		double eta = 1;

		// If zero chance of being off this won't work.  We can just compute pn for the
		// the guys that could be off and shift everything over to the right appropriately.
		
		for(int i = 0; i < L; i++)
		{
			p[i] = incoming_pis.get(i).getValue(1)/incoming_pis.get(i).getValue(0);
			eta*= incoming_pis.get(i).getValue(0);
		}
		local_pi.setValue(0, eta);
		
		double[] buf = new double[L];
		for(int i = 0; i < L; i++)
			buf[i] = 1;
		
		for(int i = 0; i < L; i++)
		{
			buf[L-1] *= p[L-1-i];
			for(int j = L-2; j >= i; j--)
				buf[j] = p[j-i]*buf[j]+buf[j+1];
			local_pi.setValue(i+1, buf[i]*eta);
		}
		return; 
	}

}
