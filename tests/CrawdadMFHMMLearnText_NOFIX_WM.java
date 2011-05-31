package tests;

import java.io.File;
import java.util.Scanner;
import java.util.Vector;

import cern.jet.random.Beta;
import cern.jet.random.engine.DRand;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.TrueOr;
import bn.distributions.DiscreteCPT.CPTSufficient2SliceStat;
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
import complex.featural.proposal_generators.LowUseDeleter;
import complex.featural.proposal_generators.SimilarityMerger;
import complex.featural.proposal_generators.UsageOverlapPuller;

public class CrawdadMFHMMLearnText_NOFIX_WM {
	
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

		@Override
		public double parameterLL() {
			return 0;
		}
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

		@Override
		public double parameterLL() {
			return 0;
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
			this.y.setAdvanceDistribution(new DiscreteCPT(new int[]{2,2},2,new double[][]{{.97, .03},{.9, .1},{.8, .2},{.5, .5}}));
			this.y.setInitialDistribution(new DiscreteCPT(new double[][]{{.9, .1},{.2, .8}}, 2));
			
			
			DiscreteCPT dist = (DiscreteCPT)this.y.getAdvanceDistribution();
			CPTSufficient2SliceStat prior = new CPTSufficient2SliceStat(dist);
			prior.exp_tr[0][0] = 20; prior.exp_tr[0][1] = 1;
			prior.exp_tr[1][0] = 10; prior.exp_tr[1][1] = 1;
			prior.exp_tr[2][0] = 3; prior.exp_tr[2][1] = 3;
			prior.exp_tr[3][0] = 1; prior.exp_tr[3][1] = 5;
			dist.prior = prior;
			/*dist.optimize(new CPTSufficient2SliceStat(dist));
			double p00 = dist.evaluate(new int[]{0,0}, 1);
			double p01 = dist.evaluate(new int[]{0,1}, 1);
			double p10 = dist.evaluate(new int[]{1,0}, 1);
			double p11 = dist.evaluate(new int[]{1,1}, 1);
			System.err.println("Want (.03,.1,.5,.7) got ("+p00+","+p10+","+p01+","+p11+")");*/
			//this.y.lockParameters();
			//this.yor.lockParameters();
		}
		
		DiscreteFiniteDistribution backupA;
		DiscreteFiniteDistribution backupPi;
		public void backupParameters() 
		{
			try {
				this.backupA = this.y.getAdvanceDistribution().copy();
				this.backupPi = this.y.getInitialDistribution().copy();
			} catch(BNException e) {
				System.err.println("Failure to back up parameters..");
			}
		}
		public void restoreParameters()
		{
			try {
				this.y.setAdvanceDistribution(this.backupA);
				this.y.setInitialDistribution(this.backupPi);
			}
			catch(BNException e) {
				System.err.println("Failure to restore parameters...");
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
				Vector<String> nodes = new  Vector<String>();
				nodes.add(yor.getName());
				nodes.add(y.getName());
				for(int i = 0; i < 10; i++)
				{
					yor.getNetwork().run(nodes, 5, 0);
					y.optimizeParameters();
				}
			} catch(BNException e) {
				System.err.println("Failure to optimize node " + y.getName());
			}
		}

		IFDiscDBNNode y, yor;

		@Override
		public double parameterLL() {
			try {
				DiscreteCPT dist = (DiscreteCPT)this.y.getAdvanceDistribution();
				double p00 = dist.evaluate(new int[]{0,0}, 1);
				double p01 = dist.evaluate(new int[]{0,1}, 1);
				double p10 = dist.evaluate(new int[]{1,0}, 1);
				double p11 = dist.evaluate(new int[]{1,1}, 1);

				double ll = 0;
				beta.setState(1,20);
				ll += Math.log(beta.pdf(p00));
				beta.setState(1,10);
				ll += Math.log(beta.pdf(p10));
				beta.setState(3, 3);
				ll += Math.log(beta.pdf(p01));
				beta.setState(5,1);
				ll += Math.log(beta.pdf(p11));
				return ll;
			} catch(BNException e) {
				System.err.println("Unable to evaluate distribution!");
				return Double.NaN;
			}
		}
		
		Beta beta = new Beta(1,1,new DRand());
	}
	
	public static class ParamGen implements MFHMMInitialParamGenerator
	{
		@Override
		public DiscreteCPT getInitialA() {
			try
			{
				//NOTE THIS WAS .8 .2 .8 .2 when got most resutls, also locked
				DiscreteCPT ret = new DiscreteCPT(new double[][]{{.9,.1},{.3,.7}}, 2);
				ret.prior = new CPTSufficient2SliceStat(ret);
				ret.prior.exp_tr[0][0] = 4;
				ret.prior.exp_tr[0][1] = 1;
				ret.prior.exp_tr[1][0] = 2;
				ret.prior.exp_tr[1][1] = 3;
				return ret;
			} catch(BNException e){return null;}
		}
		
		@Override
		public DiscreteCPTUC getInitialPi() {
			try
			{
				return new DiscreteCPTUC(new double[]{.7,.3});
			} catch(BNException e){return null;}
		}

		@Override
		public double getA_LL(DiscreteCPT A) {
			return 0;
		}

		@Override
		public double getPi_LL(DiscreteCPTUC pi) {
			return 0;
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
				YORFWrapper child = new YORFWrapper("Y"+i, net);
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
				YORFWrapper child = new YORFWrapper("Y"+i, net);
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
		gens.add(new CoherenceSplitter<IFeaturalChild,FHMMX>(.25, .0, .0, .25));
		gens.add(new SimilarityMerger<IFeaturalChild,FHMMX>(.25, .0, .25, .0));
		gens.add(new CoherenceAdder<IFeaturalChild,FHMMX>(.25, .0, .0, .25));
		gens.add(new CoherenceUniqueParenter<IFeaturalChild,FHMMX>(.25, .0, .0, .25));
		gens.add(new UsageOverlapPuller<IFeaturalChild, FHMMX>());
		gens.add(new LowUseDeleter<IFeaturalChild, FHMMX>());
		IBPMixture<IFeaturalChild,FHMMX> mix = new IBPMixture<IFeaturalChild,FHMMX>(gens,
				new double[] {.22,.22,.23,.23,.00,.1},
				new double[]{.3, .02, .68});
		cont.setLogger(System.out);

		IBPMModelOptions<IFeaturalChild,FHMMX> opts = new IBPMModelOptions<IFeaturalChild,FHMMX>(cont, ass);
		opts.maxIterations = 2000;
		opts.learn_conv = 1e-5;
		opts.max_learn_it = 5;
		opts.run_conv = 1e-5;
		opts.max_run_it = 8;
		opts.alpha = .05;
		opts.savePath = out;
		
		opts.max_finalize_iterations = 2;
		opts.finalize = true;
		
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
