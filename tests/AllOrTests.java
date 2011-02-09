package tests;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.FlatNoisyOr;
import bn.distributions.ScalarNoisyOr;
import bn.distributions.TrueOr;
import bn.impl.staticbn.StaticNetworkFactory;
import bn.statc.IDiscreteBayesNode;
import bn.statc.IStaticBayesNet;

public class AllOrTests {
	
	public static void main(String[] args) throws BNException
	{
		if(args.length!=1)
		{
			System.err.println("Must specify or node type to to test, scalar, flat or true.");
			return;
		}
	
		IStaticBayesNet net = StaticNetworkFactory.getNetwork();
		IDiscreteBayesNode x1 = net.addDiscreteNode("X1", 2);
		IDiscreteBayesNode x2 = net.addDiscreteNode("X2", 2);
		IDiscreteBayesNode x3 = net.addDiscreteNode("X3", 2);
		IDiscreteBayesNode y = net.addDiscreteNode("Y", 2);
		IDiscreteBayesNode z1 = net.addDiscreteNode("Z1", 2);
		IDiscreteBayesNode z2 = net.addDiscreteNode("Z2", 2);
	
		DiscreteCPTUC pi1 = new DiscreteCPTUC(new double[]{.9, .1});
		DiscreteCPTUC pi2 = new DiscreteCPTUC(new double[]{.8, .2});
		DiscreteCPTUC pi3 = new DiscreteCPTUC(new double[]{.7, .3});
		x1.setDistribution(pi1);
		x2.setDistribution(pi2);
		x3.setDistribution(pi3);
		
		DiscreteCPT A1 = new DiscreteCPT(new double[][]{{.9, .1},{.2, .8}}, 2);
		DiscreteCPT A2 = new DiscreteCPT(new double[][]{{.7, .3},{.65, .35}}, 2);
		z1.setDistribution(A1);
		z2.setDistribution(A2);
		
		if(args[0].equals("scalar"))
			y.setDistribution(new ScalarNoisyOr(.9));
		if(args[0].equals("flat"))
			y.setDistribution(new FlatNoisyOr(.9));
		if(args[0].equals("true"))
			y.setDistribution(TrueOr.getInstance());
		
		boolean first = true;
		
		net.addEdge("X1", "Y");
		net.addEdge("X2", "Y");
		net.addEdge("X3", "Y");
		net.addEdge("Y","Z1");
		net.addEdge("Y","Z2");
		
		net.validate();
		
		for(int x1e = -1; x1e < 2; x1e++)
		{
			for(int x2e = -1; x2e < 2; x2e++)
			{
				for(int x3e = -1; x3e < 2; x3e++)
				{
					for(int ye = -1; ye < 2; ye++)
					{
						for(int z1e = -1; z1e < 2; z1e++)
						{
							for(int z2e = -1; z2e < 2; z2e++)
							{
								if(!first)
								{
									net.clearAllEvidence();
									net.resetMessages();
								}
								else
									first = false;
								if(x1e!=-1)
									x1.setValue(x1e);
								if(x2e!=-1)
									x2.setValue(x2e);
								if(x3e!=-1)
									x3.setValue(x3e);
								if(ye!=-1)
									y.setValue(ye);
								if(z1e!=-1)
									z1.setValue(z1e);
								if(z2e!=-1)
									z2.setValue(z2e);
								net.run(100, 0);
								System.out.println(net.getLogLikelihood());
							}
						}
					}
				}
			}
		}
	}

}
