package tests;

import bn.BNException;
import bn.IBayesNode;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.ScalarNoisyOr;
import bn.dynamic.IDynFDiscNode;
import bn.dynamic.IDynNet;
import bn.impl.dynbn.DynamicNetworkFactory;

public class ModifyAndLearn {
	public static void main(String[] args) throws BNException
	{
		
		double[][] A = new double[][]{{.9, .1},{.1, .9}};
		double[] pi = new double[]{.5,.5};
		IDynNet net = DynamicNetworkFactory.newDynamicBayesNet(100);
		DiscreteCPT cpt = new DiscreteCPT(A, 2);
		DiscreteCPTUC pidist = new DiscreteCPTUC(pi);
		
		IBayesNode x = net.addDiscreteNode("X", 2);
		net.setInitialDistribution("X", pidist);
		net.setAdvanceDistribution("X", cpt);
		net.addInterEdge(x, x);
		IDynFDiscNode y1 = net.addDiscreteNode("Y1", 2);
		net.addIntraEdge(x,y1);
		net.setAdvanceDistribution("Y1", new ScalarNoisyOr(.9));
		/*IDynBayesNode y2 = net.addDiscreteNode("Y2", 2);
		net.addIntraEdge(x,y2);
		net.setDistribution("Y2", new ScalarNoisyOr(.9));*/
	
		int[] evidence = new int[100];
		for(int i = 0;i < 100; i++)
			evidence[i] = i % 2;
		y1.setValue(evidence, 0);
		
		net.validate();
		net.removeIntraEdge(x, y1);
		net.validate();
		net.optimize_parallel(20, 1e-4, 20, 1e-4);
		net.optimize(1, 0, 1, 0);
		
		
		double ll = net.getLogLikelihood();
		ll+=3;
		
		IDynFDiscNode x2 = net.addDiscreteNode("X2", 2);
		net.setInitialDistribution("X2", pidist);
		net.setAdvanceDistribution("X2", cpt);
		net.addIntraEdge(x2, y1);
		
		net.optimize_parallel(20, 1e-4, 20, 1e-4);
	}
}
