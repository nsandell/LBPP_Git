package bn.impl.nodengines;

import java.io.Serializable;

import bn.BNException;
import bn.distributions.DiscreteDistribution;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;

public class FiniteDiscreteNode implements Serializable
{
	
	public static double updateMessages(DiscreteFiniteDistribution cpt, FiniteDiscreteMessage localLambda, FiniteDiscreteMessage localPi,
			MessageSet<FiniteDiscreteMessage> incPis, MessageSet<FiniteDiscreteMessage> outPis,
			MessageSet<FiniteDiscreteMessage> incLambdas, MessageSet<FiniteDiscreteMessage> outLambdas, Integer value, int cardinality) throws BNException
	{
		FiniteDiscreteMessage oldMarginal = localLambda.multiply(localPi);
		oldMarginal.normalize();

		if(value==null)
			FiniteDiscreteNode.updateLocalLambda(localLambda,incLambdas,cardinality);
		else
			FiniteDiscreteNode.updateLocalLambda(localLambda, value);

		FiniteDiscreteNode.updateLocalPi(cpt, localPi, incPis, value);
		FiniteDiscreteNode.updateLambdas(cpt, outLambdas, incPis, localLambda, value);
		FiniteDiscreteNode.updatePis(incLambdas, outPis, localPi, cardinality, value);

		FiniteDiscreteMessage newmarg = localLambda.multiply(localPi);
		newmarg.normalize();

		double maxChange = 0;
		for(int i = 0; i < oldMarginal.getCardinality(); i++)
			maxChange = Math.max(maxChange,Math.abs(oldMarginal.getValue(i)-newmarg.getValue(i)));
		return maxChange;
	}
	
	public static void updateLocalLambda(FiniteDiscreteMessage local_lambda, MessageSet<FiniteDiscreteMessage> incomingLambdaMessages, int cardinality)
	{
		for(int i = 0; i < cardinality; i++)
		{
			double tmp = 1;
			for(int j = 0; j < incomingLambdaMessages.size(); j++)
				tmp *= incomingLambdaMessages.get(j).getValue(i);
			local_lambda.setValue(i,tmp);
		}
		local_lambda.normalize();
	}
	
	public static void updateLocalLambda(FiniteDiscreteMessage local_lambda, Integer observation)
	{
		local_lambda.setDelta(observation, 1.0);
	}

	public static void updateLocalPi(DiscreteDistribution cpt, FiniteDiscreteMessage local_pi, MessageSet<FiniteDiscreteMessage> incomingPiMessages, Integer value) throws BNException
	{
		for(int i = 0; i < local_pi.getCardinality(); i++)
			local_pi.setValue(i, 0);
		cpt.computeLocalPi(local_pi, incomingPiMessages, value);	
	}
	
	
	public static void updateLambdas(DiscreteDistribution cpt, MessageSet<FiniteDiscreteMessage> outgoingLambdas, MessageSet<FiniteDiscreteMessage> incomingPis , FiniteDiscreteMessage localLambda, Integer value) throws BNException
	{
		for(FiniteDiscreteMessage lambda : outgoingLambdas)
			lambda.empty();
		cpt.computeLambdas(outgoingLambdas, incomingPis, localLambda, value);
	}

	public static void updatePis(MessageSet<FiniteDiscreteMessage> incomingLambdaMessages, MessageSet<FiniteDiscreteMessage> outgoingPiMessages, FiniteDiscreteMessage localPi,int cardinality, Integer observation) throws BNException
	{
		int imin = 0; int imax = cardinality;
		if(observation!=null){imin = observation; imax = observation+1;}
		
		Integer[] zeroNodes = new Integer[imax-imin];
		double[] lambda_prods = new double[imax-imin];
		for(int i = imin; i < imax; i++)
		{
			zeroNodes[i-imin] = null;
			lambda_prods[i-imin] = 1;
		}
		
		for(int i = imin; i < imax; i++)
		{
			for(int j = 0; j < incomingLambdaMessages.size(); j++)
			{
				FiniteDiscreteMessage dm = incomingLambdaMessages.get(j);
				if(dm.getValue(i)!=0)	
					lambda_prods[i-imin] *= dm.getValue(i);
				else if(zeroNodes[i-imin]==null)
					zeroNodes[i-imin] = j;
				else
				{
					lambda_prods[i-imin] = 0;
					break;
				}
			}
		}	

		for(int j = 0; j < incomingLambdaMessages.size(); j++)
		{
			FiniteDiscreteMessage from_child = incomingLambdaMessages.get(j);
			FiniteDiscreteMessage pi_child = outgoingPiMessages.get(j);
			pi_child.empty();
			for(int i = imin; i < imax; i++)
			{
				double lambda_prod_local = lambda_prods[i-imin];
				if(lambda_prod_local > 0 && zeroNodes[i-imin]==null)
					lambda_prod_local /= from_child.getValue(i);
				else if(lambda_prod_local > 0 && zeroNodes[i-imin]!=j)
					lambda_prod_local = 0;
				pi_child.setValue(i, lambda_prod_local*localPi.getValue(i));
			}
			pi_child.normalize();
		}
	}
	
	public static final long serialVersionUID = 50L;
}
