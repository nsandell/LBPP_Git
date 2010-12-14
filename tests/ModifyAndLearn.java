package tests;

import bn.BNException;
import bn.IBayesNode;
import bn.IDynBayesNet;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.ScalarNoisyOr;

public class ModifyAndLearn {
	public static void main(String[] args) throws BNException
	{
		
		double[][] A = new double[][]{{.9, .1},{.1, .9}};
		double[] pi = new double[]{.5,.5};
		IDynBayesNet net = bn.impl.BayesNetworkFactory.getDynamicNetwork(100);
		DiscreteCPT cpt = new DiscreteCPT(A, 2);
		DiscreteCPTUC pidist = new DiscreteCPTUC(pi);
		
		IBayesNode x = net.addDiscreteNode("X", 2);
		net.setInitialDistribution("X", pidist);
		net.setDistribution("X", cpt);
		net.addInterEdge(x, x);
		IBayesNode y1 = net.addDiscreteNode("Y1", 2);
		net.addIntraEdge(x,y1);
		net.setDistribution("Y1", new ScalarNoisyOr(.9));
		/*IDynBayesNode y2 = net.addDiscreteNode("Y2", 2);
		net.addIntraEdge(x,y2);
		net.setDistribution("Y2", new ScalarNoisyOr(.9));*/
	
		int[] evidence = new int[100];
		for(int i = 0;i < 100; i++)
			evidence[i] = i % 2;
		net.setEvidence("Y1",0 , evidence);
		//net.setDiscreteEvidence("Y2",0 , evidence);
		
		net.validate();
		net.removeIntraEdge(x, y1);
		net.validate();
		net.optimize_parallel(20, 1e-4, 20, 1e-4);
		net.optimize(1, 0, 1, 0);
		
		
		double ll = net.getLogLikelihood();
		ll+=3;
		
		IBayesNode x2 = net.addDiscreteNode("X2", 2);
		net.setInitialDistribution("X2", pidist);
		net.setDistribution("X2", cpt);
		net.addIntraEdge(x2, y1);
		
		net.optimize_parallel(20, 1e-4, 20, 1e-4);
	}
}