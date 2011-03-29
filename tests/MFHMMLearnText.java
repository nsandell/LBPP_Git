package tests;

import java.io.File;
import java.util.Scanner;
import java.util.Vector;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.TrueOr;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.impl.dynbn.DynamicNetworkFactory;
import complex.IParentProcess;
import complex.featural.IBPMixture;
import complex.featural.IFeaturalChild;
import complex.featural.IBPMixture.IBPMModelOptions;
import complex.featural.ProposalGenerator;
import complex.featural.controllers.FHMMX;
import complex.featural.controllers.MFHMMController;
import complex.featural.controllers.MFHMMController.MFHMMInitialParamGenerator;
import complex.featural.proposal_generators.CoherenceAdder;
import complex.featural.proposal_generators.CoherenceSplitter;
import complex.featural.proposal_generators.CoherenceUniqueParenter;
import complex.featural.proposal_generators.RandomAbsorbGenerator;
import complex.featural.proposal_generators.RandomAdderGenerator;
import complex.featural.proposal_generators.RandomExpungeGenerator;
import complex.featural.proposal_generators.RandomMergeGenerator;
import complex.featural.proposal_generators.RandomSplitGenerator;
import complex.featural.proposal_generators.SimilarityMerger;

public class MFHMMLearnText {
	
	public static class YWrapper implements IFeaturalChild
	{
		public YWrapper(IFDiscDBNNode ynd)
		{
			this.ynd = ynd;
			this.ynd.lockParameters();
		}
		
		public String getName()
		{
			return this.ynd.getName();
		}
		
		public IFDiscDBNNode hook()
		{
			return this.ynd;
		}
		
		public double getDisagreement(int t)
		{
			return ynd.conditionalLL(t);
		}
		
		@Override
		public void backupParameters() {}

		@Override
		public void restoreParameters() {}

		@Override
		public void addParent(IParentProcess p) {}

		@Override
		public void killParent(IParentProcess p) {}

		@Override
		public void optimize() {}

		IFDiscDBNNode ynd;
	}
	
	public static class YORWrapper implements IFeaturalChild
	{
		public YORWrapper(String name, IDynamicBayesNet net) throws BNException
		{
			this.y = net.addDiscreteNode(name, 2);
			this.yor = net.addDiscreteNode(name+"_OR",2);
			net.addIntraEdge(this.yor, this.y);
			this.yor.setAdvanceDistribution(TrueOr.getInstance());
			this.y.setAdvanceDistribution(new DiscreteCPT(new double[][]{{.9,.1},{.1,.9}},2));
			this.backup = this.y.getAdvanceDistribution().copy();
		}
	
		DiscreteFiniteDistribution backup = null;
		public void backupParameters()
		{
			try {
				this.backup = this.y.getAdvanceDistribution().copy();
			} catch(BNException e) {
				System.err.println("Failure to backup parameters");
			}
		}
		public void restoreParameters()
		{
			try
			{
				if(this.backup!=null)
					this.y.setAdvanceDistribution(this.backup);
			} catch(BNException e) {
				System.err.println("Failure to backup parameters");
			}
		}
		
		public double getDisagreement(int t)
		{
			return this.y.conditionalLL(t);
		}
		
		public String getName()
		{
			return y.getName();
		}
		
		public IFDiscDBNNode hook()
		{
			return this.yor;
		}
		
		@Override
		public void addParent(IParentProcess p) {}

		@Override
		public void killParent(IParentProcess p) {}
			
		@Override
		public void optimize()
		{
			try {
				Vector<String> nodes = new Vector<String>();
				nodes.add(y.getName());
				nodes.add(yor.getName());
				y.getNetwork().run_parallel_block(nodes, 2, 0);
				y.optimizeParameters();
			} catch(BNException e) {
				System.err.println("Failure to potimize");
			}
		}

		IFDiscDBNNode y, yor;
	}
	
	public static class YORFWrapper implements IFeaturalChild
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
			this.y.lockParameters();
			this.yor.lockParameters();
		}
		
		public void backupParameters(){}
		public void restoreParameters(){}
		
		public double getDisagreement(int t)
		{
			return this.y.conditionalLL(t);
		}
		
		public String getName()
		{
			return y.getName();
		}
		
		public IFDiscDBNNode hook()
		{
			return this.yor;
		}
		
		@Override
		public void addParent(IParentProcess p) {}

		@Override
		public void killParent(IParentProcess p) {}
			
		@Override
		public void optimize() {}

		IFDiscDBNNode y, yor;
	}
	
	public static class ParamGen implements MFHMMInitialParamGenerator
	{
		@Override
		public DiscreteCPT getInitialA() {
			try
			{
				return new DiscreteCPT(new double[][]{{.8,.2},{.8,.2}}, 2);
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
		Vector<IFeaturalChild> children = new Vector<IFeaturalChild>();
		IDynamicBayesNet net;
		boolean[][] ass;
		String out = null;
		if(args.length!=3)
		{
			System.out.println("using test data set...");
			net = DynamicNetworkFactory.newDynamicBayesNet(20);
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
				YORWrapper child = new YORWrapper("Y"+i, net);
				child.y.setValue(o[i], 0);
				children.add(child);
			}
			ass = new boolean[9][1];
			for(int i = 0; i < 9; i++)
				ass[i][0] = true;
			ass = new boolean[][] {
				{true,false,false},
				{true,false,false},
				{true,false,false},
				{true,false,false},
				{true,false,false},
				{true,false,false},
				{true,true,false},
				{true,true,true},
				{true,true,true}
			};
			//ass = IBPMModelOptions.randomAssignment(o.length, 20, .2);
		}
		else
		{
			System.out.println("Loading external data...");
			String obs = args[0];
			out = args[1];
			String membership = args[2];

			int[][] o = loadData(obs);
			int[][] mi = loadData(membership);

			net = DynamicNetworkFactory.newDynamicBayesNet(o[0].length);
			for(int i = 0; i < o.length; i++)
			{
				YORWrapper child = new YORWrapper("Y"+i, net);
				child.y.setValue(o[i], 0);
				children.add(child);
			}
			ass = new boolean[o.length][mi[0].length];
			for(int i = 0; i < mi.length; i++)
				for(int j = 0; j < mi[0].length; j++)
					ass[i][j] = (mi[i][j]==1);
		}

		MFHMMController cont = new MFHMMController(net,children,new ParamGen(),2);
		Vector<ProposalGenerator<IFeaturalChild, FHMMX>> gens = new Vector<ProposalGenerator<IFeaturalChild,FHMMX>>();
		gens.add(new RandomAbsorbGenerator<IFeaturalChild,FHMMX>(.05, .05));
		gens.add(new RandomExpungeGenerator<IFeaturalChild,FHMMX>(.05, .05));
		gens.add(new RandomMergeGenerator<IFeaturalChild,FHMMX>(.1, .1));
		gens.add(new RandomSplitGenerator<IFeaturalChild,FHMMX>(.1, .1));
		gens.add(new RandomAdderGenerator<IFeaturalChild,FHMMX>(.1, .1));
		gens.add(new CoherenceSplitter<IFeaturalChild,FHMMX>(.35, .1, .1, .35));
		gens.add(new SimilarityMerger<IFeaturalChild,FHMMX>(.35, .1, .35, .1));
		gens.add(new CoherenceAdder<IFeaturalChild,FHMMX>(.35, .1, .1, .35));
		gens.add(new CoherenceUniqueParenter<IFeaturalChild,FHMMX>(.35, .1, .1, .35));
		IBPMixture<IFeaturalChild,FHMMX> mix = new IBPMixture<IFeaturalChild,FHMMX>(gens,
				//new double[] {.05,.05,.1,.1,.2,.15,.35},
				new double[] {0,0,.35,.35,.3,0,0,0,0},
				new double[]{.05, .05, .9});
		cont.setLogger(System.out);

		IBPMModelOptions<IFeaturalChild,FHMMX> opts = new IBPMModelOptions<IFeaturalChild,FHMMX>(cont, ass);
		opts.maxIterations = 50000; 
		opts.alpha = 1;
		opts.savePath = out;
		mix.learn(opts);
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
