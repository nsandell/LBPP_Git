package tests;

import java.io.File;
import java.util.Scanner;
import java.util.Vector;
import bn.BNException;
import bn.IDynBayesNet;
import bn.IDynBayesNode;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.FlatNoisyOr;
import complex.featural.IBPMixture;
import complex.featural.IBPMixture.IBPMModelOptions;
import complex.featural.ProposalGenerator;
import complex.featural.controllers.MFHMMController;
import complex.featural.controllers.MFHMMController.IFHMMChild;
import complex.featural.controllers.MFHMMController.MFHMMInitialParamGenerator;
import complex.featural.proposal_generators.RandomAbsorbGenerator;
import complex.featural.proposal_generators.RandomExpungeGenerator;
import complex.featural.proposal_generators.RandomMergeGenerator;
import complex.featural.proposal_generators.RandomSplitGenerator;

public class MFHMMLearnText {
	
	public static class YWrapper implements MFHMMController.IFHMMChild
	{
		public YWrapper(IDynBayesNode ynd)
		{
			this.ynd = ynd;
		}
		
		public String getName()
		{
			return this.ynd.getName();
		}
		
		public IDynBayesNode hook()
		{
			return this.ynd;
		}
		
		IDynBayesNode ynd;
	}
	
	public static class ParamGen implements MFHMMInitialParamGenerator
	{
		@Override
		public DiscreteCPT getInitialA() {
			try
			{
				return new DiscreteCPT(new double[][]{{.7,.3},{.7,.3}}, 2);
			} catch(BNException e){return null;}
		}
		
		@Override
		public DiscreteCPTUC getInitialPi() {
			try
			{
				return new DiscreteCPTUC(new double[]{.7,.3});
			} catch(BNException e){return null;}
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		if(args.length!=2)
			throw new Exception("What give me paths!");
		String obs = args[0];
		String out = args[1];
		Vector<IFHMMChild> children = new Vector<MFHMMController.IFHMMChild>();
		
/*		Integer[][] o = new Integer[][]{{0,1,1,1,0,0,0,0,0,0},
								{0,1,1,1,0,0,0,0,0,0},
								{0,1,1,1,0,0,0,0,0,0},
								{0,1,1,1,0,0,1,1,1,0},
								{0,1,1,1,0,0,1,1,1,0},
								{0,1,1,1,0,0,1,1,1,0},
								{0,0,0,0,0,0,1,1,1,0},
								{0,0,0,0,0,0,1,1,1,0},
								{0,0,0,0,0,0,1,1,1,0}};
		for(int i = 0; i < 9; i++)
		{
			YWrapper child = new YWrapper(net.addDiscreteNode("Y"+i, 2));
			children.add(child);
			child.ynd.setDistribution(new FlatNoisyOr(.9));
			net.setEvidence(child.getName(), 0, o[i]);
		}*/
		
		Integer[][] o = loadData(obs);
		IDynBayesNet net = bn.impl.BayesNetworkFactory.getDynamicNetwork(o[0].length);
		for(int i = 0; i < o.length; i++)
		{
			YWrapper child = new YWrapper(net.addDiscreteNode("Y"+i, 2));
			children.add(child);
			child.ynd.setDistribution(new FlatNoisyOr(.7));
			net.setEvidence(child.getName(), 0, o[i]);
		}
		
		MFHMMController cont = new MFHMMController(net,children,new ParamGen(),2);
		IBPMixture mix = new IBPMixture(new ProposalGenerator[] {
											new RandomAbsorbGenerator(.25, .25),
											new RandomExpungeGenerator(.25, .25),
											new RandomMergeGenerator(.25, .25),
											new RandomSplitGenerator(.25, .25)},
										new double[] {.25, .25, .25, .25}, 
										new double[]{.2, .1, .7});
		cont.setLogger(System.out);
		
		IBPMModelOptions opts = new IBPMModelOptions(cont, IBPMModelOptions.randomAssignment(o.length, 10, .2));
		opts.maxIterations = 2000;
		opts.savePath = out;
		mix.learn(opts);
	}
	
	public static Integer[][] loadData(String file) throws Exception
	{
		Scanner scan = new Scanner(new File(file));
		int rows = scan.nextInt();
		int cols = scan.nextInt();
		Integer[][] dat = new Integer[rows][cols];
		for(int i = 0; i < rows; i++)
		{
			for(int j = 0; j < cols; j++)
			{
				if(!scan.hasNext())
					throw new BNException("Failed to read data, less elements than specified!");
				dat[i][j] = scan.nextInt();
			}
		}
		return dat;
	}
}
