package bn.impl.nodengines;

import java.io.Serializable;

import bn.BNException;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;

public class FiniteDiscreteNode implements Serializable
{
	
	public static double updateMessages(DiscreteFiniteDistribution cpt, FiniteDiscreteMessage localLambda, FiniteDiscreteMessage localPi, FiniteDiscreteMessage marginal,
			MessageSet<FiniteDiscreteMessage> incPis, MessageSet<FiniteDiscreteMessage> outPis,
			MessageSet<FiniteDiscreteMessage> incLambdas, MessageSet<FiniteDiscreteMessage> outLambdas, Integer value, int cardinality) throws BNException
	{

		if(value==null)
			FiniteDiscreteNode.updateLocalLambda(localLambda,incLambdas,cardinality);
		else
			FiniteDiscreteNode.updateLocalLambda(localLambda, value);

		FiniteDiscreteNode.updateLocalPi(cpt, localPi, incPis);
		FiniteDiscreteNode.updateLambdas(cpt, outLambdas, incPis, localLambda, value);
		FiniteDiscreteNode.updatePis(incLambdas, outPis, localPi, cardinality, value);

		FiniteDiscreteMessage newmarg = localLambda.multiply(localPi);
		newmarg.normalize();

		double maxChange = 0;
		for(int i = 0; i < marginal.getCardinality(); i++)
			maxChange = Math.max(maxChange,Math.abs(marginal.getValue(i)-newmarg.getValue(i)));

		marginal.adopt(newmarg);
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

	public static void updateLocalPi(DiscreteFiniteDistribution cpt, FiniteDiscreteMessage local_pi, MessageSet<FiniteDiscreteMessage> incomingPiMessages) throws BNException
	{
		for(int i = 0; i < local_pi.getCardinality(); i++)
			local_pi.setValue(i, 0);
		cpt.computeLocalPi(local_pi, incomingPiMessages);	
	}
	
	public static void updateLambdas(DiscreteFiniteDistribution cpt, MessageSet<FiniteDiscreteMessage> outgoingLambdas, MessageSet<FiniteDiscreteMessage> incomingPis , FiniteDiscreteMessage localLambda, Integer value) throws BNException
	{
		for(FiniteDiscreteMessage lambda : outgoingLambdas)
			lambda.empty();
		cpt.computeLambdas(outgoingLambdas, incomingPis, localLambda, value);
		for(FiniteDiscreteMessage lambda : outgoingLambdas)
			lambda.normalize();
	}
	
	public static void updateOutgoingPi(MessageSet<FiniteDiscreteMessage> incomingLambdaMessages, MessageSet<FiniteDiscreteMessage> outgoingPiMessages, int updateIndex, FiniteDiscreteMessage localPi,int cardinality, Integer observation) throws BNException
	{
		if(observation!=null)
		{
			outgoingPiMessages.get(updateIndex).setDelta(observation, 1.0);
			return;
		}
		
		double[] lambda_prods = new double[cardinality];
		for(int i = 0; i < cardinality; i++)
			lambda_prods[i] = 1;
		
		for(int i = 0; i < cardinality; i++)
		{
			for(int j = 0; j < incomingLambdaMessages.size(); j++)
			{
				if(j==updateIndex)
					continue;
			 	
				lambda_prods[i] *= incomingLambdaMessages.get(j).getValue(i);
				if(lambda_prods[i]==0)
					break;
			}
		}
		
		FiniteDiscreteMessage pi_child = outgoingPiMessages.get(updateIndex);
		
		pi_child.empty();
		for(int i = 0; i < cardinality; i++)
			pi_child.setValue(i, lambda_prods[i]*localPi.getValue(i));
		pi_child.normalize();
	}
	
	public static void updatePis(MessageSet<FiniteDiscreteMessage> incomingLambdaMessages, MessageSet<FiniteDiscreteMessage> outgoingPiMessages, FiniteDiscreteMessage localPi,int cardinality, Integer observation) throws BNException
	{
		if(observation!=null)
			for(FiniteDiscreteMessage outpi : outgoingPiMessages)
				outpi.setDelta(observation, 1.0);
		else
		{
			Integer[] zeroNodes = new Integer[cardinality];
			double[] lambda_prods = new double[cardinality];
			for(int i = 0; i < cardinality; i++)
			{
				zeroNodes[i] = null;
				lambda_prods[i] = 1;
			}

			for(int i = 0; i < cardinality; i++)
			{
				for(int j = 0; j < incomingLambdaMessages.size(); j++)
				{
					FiniteDiscreteMessage dm = incomingLambdaMessages.get(j);
					if(dm.getValue(i)!=0)	
						lambda_prods[i] *= dm.getValue(i);
					else if(zeroNodes[i]==null)
						zeroNodes[i] = j;
					else
					{
						lambda_prods[i] = 0;
						break;
					}
				}
			}	

			for(int j = 0; j < incomingLambdaMessages.size(); j++)
			{
				FiniteDiscreteMessage from_child = incomingLambdaMessages.get(j);
				FiniteDiscreteMessage pi_child = outgoingPiMessages.get(j);
				pi_child.empty();
				for(int i = 0; i < cardinality; i++)
				{
					double lambda_prod_local = lambda_prods[i];
					if(lambda_prod_local > 0 && zeroNodes[i]==null)
						lambda_prod_local /= from_child.getValue(i);
					else if(lambda_prod_local > 0 && zeroNodes[i]!=j)
						lambda_prod_local = 0;
					pi_child.setValue(i, lambda_prod_local*localPi.getValue(i));
				}
				pi_child.normalize();
			}		
		}
	}
	
	public static final long serialVersionUID = 50L;
}
