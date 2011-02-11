package tests;

import java.io.File;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.plaf.SliderUI;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.FlatNoisyOr;
import bn.distributions.TrueOr;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.impl.dynbn.DynamicNetworkFactory;
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
		public YWrapper(IFDiscDBNNode ynd)
		{
			this.ynd = ynd;
		}
		
		public String getName()
		{
			return this.ynd.getName();
		}
		
		public IFDiscDBNNode hook()
		{
			return this.ynd;
		}
		
		IFDiscDBNNode ynd;
	}
	
	public static class YORFWrapper implements MFHMMController.IFHMMChild
	{
		public YORFWrapper(String name, IDynamicBayesNet net) throws BNException
		{
			this.y = net.addDiscreteNode(name, 2);
			this.yor = net.addDiscreteNode(name+"_OR",2);
			net.addIntraEdge(this.yor, this.y);
			net.addInterEdge(this.y, this.y);
			this.yor.setAdvanceDistribution(TrueOr.getInstance());
			this.y.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2},2,new double[][]{{.97, .03},{.9, .1},{.5, .5},{.3, .7}}));
			this.y.setInitialDistribution(new DiscreteCPT(new double[][]{{.9, .1},{.2, .8}}, 2));
		}
		
		public String getName()
		{
			return y.getName();
		}
		
		public IFDiscDBNNode hook()
		{
			return this.yor;
		}
		
		IFDiscDBNNode y, yor;
	}
	
	public static class ParamGen implements MFHMMInitialParamGenerator
	{
		@Override
		public DiscreteCPT getInitialA() {
			try
			{
				return new DiscreteCPT(new double[][]{{.7,.3},{.3,.7}}, 2);
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
		
		/*
		IDynamicBayesNet net = DynamicNetworkFactory.newDynamicBayesNet(20);
		int[][] o = new int[][]{{0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,0,0,0,0},
								{0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,0,0,0,0},
								{0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,0,0,0,0},
								{0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0},
								{0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0},
								{0,1,1,1,0,0,1,1,1,0,0,1,1,1,0,0,1,1,1,0},
								{0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0},
								{0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0},
								{0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0}};
		for(int i = 0; i < 9; i++)
		{
			YORFWrapper child = new YORFWrapper("Y"+i, net);
			child.y.setValue(o[i], 0);
			children.add(child);
			//YWrapper child = new YWrapper(net.addDiscreteNode("Y"+i, 2));
			//children.add(child);
			//child.ynd.setAdvanceDistribution(new FlatNoisyOr(.9));
			//child.ynd.setValue(o[i], 0);
		}*/
		int[][] o = loadData(obs);
		IDynamicBayesNet net = DynamicNetworkFactory.newDynamicBayesNet(o[0].length);
		for(int i = 0; i < o.length; i++)
		{
			YORFWrapper child = new YORFWrapper("Y"+i, net);
			child.y.setValue(o[i], 0);
			children.add(child);
//			YWrapper child = new YWrapper(net.addDiscreteNode("Y"+i, 2));
//			children.add(child);
//			child.ynd.setAdvanceDistribution(new FlatNoisyOr(.7));
//			child.ynd.setValue(o[i], 0);
		}
		
		MFHMMController cont = new MFHMMController(net,children,new ParamGen(),2);
		IBPMixture mix = new IBPMixture(new ProposalGenerator[] {
											new RandomAbsorbGenerator(.25, .25),
											new RandomExpungeGenerator(.25, .25),
											new RandomMergeGenerator(.25, .25),
											new RandomSplitGenerator(.25, .25)},
										new double[] {.25, .25, .25, .25}, 
										new double[]{.35, .03, .62});
		cont.setLogger(System.out);
		
		
		//boolean [][]ass = new boolean[o.length][0];
		boolean[][] ass = new boolean[10][0];
		/*	new boolean[][] {{true, false},
										   {true, false},
										   {true, false},
										   {true, true},
										   {true, true},
										   {true, true},
										   {false, true},
										   {false, true},
										   {false, true}};*/
		IBPMModelOptions opts = new IBPMModelOptions(cont, ass);
		//IBPMModelOptions opts = new IBPMModelOptions(cont, IBPMModelOptions.randomAssignment(o.length, 10, .2));
		opts.maxIterations = 50000;
		opts.alpha = 1;
		opts.savePath = out;
		mix.learn(opts);
		//System.out.println(net.getDefinition());
	}
	
	public static int[][] loadData(String file) throws Exception
	{
		Scanner scan = new Scanner(new File(file));
		int rows = scan.nextInt();
		int cols = scan.nextInt();
		int[][] dat = new int[rows][cols];
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
