package complex.metrics;

import bn.messages.FiniteDiscreteMessage;

public class FinDiscMarginalDivergence {
	
	public static interface FinDiscMarginalHolder
	{
		FiniteDiscreteMessage marginal(int t);
	}
	
	public static double meanSimDivergence(FinDiscMarginalHolder o1, FinDiscMarginalHolder o2, int T)
	{
		double dT = T;
		double meandiv = 0;
		
		for(int i = 0; i < T; i++)
		{
			for(int j = 0; j < T; j++)
			{
				double p1 = o1.marginal(i).getValue(j);
				double p2 = o2.marginal(i).getValue(j);
				if((p1==0 && p2==0)  || (p1==1 && p2==1))
					continue;
				if(p1==0 || p1==1 || p2==0 || p2==1)
					return Double.POSITIVE_INFINITY;
				meandiv += (p1*Math.log(p1/p2)+p2*Math.log(p2/p1))/dT;
			}
		}
		return meandiv;
	}
}
