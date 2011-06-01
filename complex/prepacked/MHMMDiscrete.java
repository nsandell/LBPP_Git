package complex.prepacked;

import java.util.HashMap;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import complex.CMException;
import complex.mixture.IMixtureChild;
import complex.prepacked.MHMM.IMixtureChildFactory;

public class MHMMDiscrete
{
	public static class BDCFactory implements IMixtureChildFactory
	{
		@Override
		public IMixtureChild getChild(IDynamicBayesNet net, int nameIndex, int ns, int[] observations)
		{
			try {
				int T = net.getT();
				int cardinality = 1;
				for(int j = 0; j < T; j++)
					cardinality = Math.max(cardinality, observations[j]+1);
				IFDiscDBNNode nd = net.addDiscreteNode("Y"+nameIndex, cardinality);
				nd.setAdvanceDistribution(new DiscreteCPT(obsmat(ns, cardinality),cardinality));
				nd.setValue(observations, 0);
				return new BasicDiscreteChild(nd);
			} catch(BNException bne) {
				System.err.println("Error creating observation node Y"+nameIndex+": " + bne.toString());
				return null;
			}
		}
		
		private static double[][] obsmat(int ns, int no)
		{
			double[][] mat = new double[ns][no];
			for(int i = 0; i < ns; i++)
			{
				for(int j = 0; j < no; j++)
				{
					if(i%no==j)
						mat[i][j] = concentration;
					else
						mat[i][j] = (1-concentration)/(no-1);
				}
			}
			return mat;
		}
		
		static double concentration = .9;

		@Override
		public void setArg(String arg) throws CMException {
			try {
				concentration = Double.parseDouble(arg);
				if(concentration < 0 || concentration > 1)
					throw new CMException("Concentration parameter for discrete nodes must be in range [0,1]");
			} catch(NumberFormatException e) {
				throw new CMException("Error parsing floating point concentration parameter : " + arg);
			} 
		}
	}
	
	public static class BasicDiscreteChild extends IMixtureChild.MixtureSingleNodeChild
	{
		public BasicDiscreteChild(IFDiscDBNNode node)
		{
			super(node);
		}

		@Override
		public double parameterLL() {
			return 0;
		}
	}

	public static void main(String[] args) throws BNException, CMException
	{
		HashMap<String,IMixtureChildFactory> factories = new HashMap<String,IMixtureChildFactory>();
		factories.put("default",new BDCFactory());
		factories.put("basic", new BDCFactory());
		MHMM.mhmm_main(args, factories);
	}
}
