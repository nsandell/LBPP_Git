package complex.prepacked;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.dynbn.DynamicNetworkFactory;
import bn.messages.FiniteDiscreteMessage;

import complex.CMException;
import complex.mixture.FixedMixture;
import complex.mixture.FixedMixture.FMModelOptions;
import complex.mixture.controllers.MHMMController;
import complex.mixture.controllers.MHMMController.MHMMChild;
import complex.mixture.controllers.MHMMController.MHMMParameterPrior;

public class MHMMDiscrete
{
	
	private static class Priors implements MHMMParameterPrior
	{
		
		public Priors(int Ns) throws BNException
		{
			double[] piv = new double[Ns]; piv[0] = 1.0;
			double[][] Av = new double[Ns][Ns];
			for(int i = 0; i < Ns; i++)
			{
				for(int j = 0; j < Ns; j++)
				{
					if(i==j)
						Av[i][j] = .9;
					else
						Av[i][j] = .1/(Ns-1);
				}
			}
			this.pi = new DiscreteCPTUC(piv);
			this.A = new DiscreteCPT(Av, Ns);
		}

		@Override
		public double evaluate(DiscreteCPT A) {
			return 1;
		}

		@Override
		public double evaluate(DiscreteCPTUC pi) {
			return 1;
		}

		@Override
		public DiscreteCPT initialSampleA() {
			return  this.A;
		}

		@Override
		public DiscreteCPTUC initialSamplePi() {
			return this.pi;
		}

		@Override
		public DiscreteCPT posteriorSampleA(DiscreteCPT A) {
			return A;
		}

		@Override
		public DiscreteCPTUC posteriorSamplePi(DiscreteCPTUC pi) {
			return pi;
		}
		
		DiscreteCPT A;
		DiscreteCPTUC pi;
	}
	
	private static class MHMM_BasicChild implements MHMMChild
	{
		public MHMM_BasicChild(IFDiscDBNNode node)
		{
			this.node = node;
		}
		private IFDiscDBNNode node;
		
		@Override
		public String getName() {
			return node.getName();
		}

		@Override
		public double getDisagreement(int t) {
			return node.conditionalLL(t);
		}
		
		public Collection<String> constituentNodeNames()
		{
			Vector<String> names = new Vector<String>();
			names.add(node.getName());
			return names;
		}

		@Override
		public IDBNNode hook() {
			return node;
		}

		@Override
		public void optimize(Vector<FiniteDiscreteMessage> chainIncPis)
		{
			try {
				int N = chainIncPis.get(0).getCardinality();
				int M = this.node.getCardinality();
				double[][] stats = new double[N][M];
				int T = this.node.getNetwork().getT();
				for(int t = 0; t < T; t++)
					for(int i = 0; i < N; i++)
						stats[i][this.node.getValue(t)] += chainIncPis.get(t).getValue(i);
				for(int i = 0; i < N; i++)
				{
					double rowsum = 0;
					for(int j = 0; j < M; j++)
						rowsum += stats[i][j];
					for(int j = 0; j < M; j++)
						stats[i][j] /= rowsum;
				}
				this.node.setAdvanceDistribution(new DiscreteCPT(stats, M));
			} catch(BNException e) {
				System.err.println("Failed to optimize node " + this.getName() + " : " + e.toString());
			}
		}

		@Override
		public double evaluateP() {
			return 1;
		}

		@Override
		public void sampleInit() {}

		@Override
		public void samplePosterior() {}
		
	}

	public static void main(String[] args) throws BNException, CMException
	{
		Options options = new Options();
		options.addOption("v", "verbose", false, "Use verbose mode.");
		Option opt;
// Removed these options because in this mHMM should be able to converge exactly and quickly, enable if this file 
//		is copied for something like AR-mHMM
//		opt = new Option("bp-iterations","Maximum number of iterations to perform in belief propagation.");
//		opt.setArgs(1);opt.setArgName("#Iterations");
//		options.addOption(opt);
//		
//		opt = new Option("bp-convergence","Convergence threshold for belief propagation.");
//		opt.setArgs(1);opt.setArgName("threshold");
//		options.addOption(opt);
		
		opt = new Option("emiterations","Maximum number of iterations to perform in expectation-maximization.");
		opt.setArgs(1);opt.setArgName("#Iterations");
		options.addOption(opt);
		
		opt = new Option("emconvergence","Convergence threshold for expectation maximization.");
		opt.setArgs(1);opt.setArgName("threshold");
		options.addOption(opt);
		
		//TODO This option wouldn't be as necessary without the bug that causes oscillations so.. figure that out.
		opt = new Option("i","maxiterations",true,"Maximum number of times to run through the observables to change assignments.");
		opt.setArgs(1);opt.setArgName("#Iterations");
		options.addOption(opt);
		
		opt = new Option("output","o",true,"Model output file");
		opt.setArgs(1);opt.setArgName("file");
		options.addOption(opt);
		
		HelpFormatter formatter = new HelpFormatter();
		
		CommandLineParser clp = new GnuParser();
		
		String[] justArgs = new String[]{""};
		int[][] o;
		int N;
		int Ns;
		CommandLine line;
		try {
			line = clp.parse(options,args);
			justArgs = line.getArgs();
			
			if(justArgs.length!=3)
				throw new ParseException("Expect 3 arguments, observation file name, number of latent chains, and the size of latent chains state spaces.");
			
			o = loadData(justArgs[0]);
			if(o==null)
			{
				System.err.println("Error laoding observation file - provided dimensions are incorrect.");
				return;
			}
			N = Integer.parseInt(justArgs[1]);
			Ns = Integer.parseInt(justArgs[2]);
		}
		catch ( ParseException exp ) {
			System.err.println("Invalid options..");
			formatter.printHelp("mhmm [observation file] [number of latent chains] [cardinality of latent chains' state spaces]", options);
			return;
		} catch(FileNotFoundException e) {
			System.err.println("Observation file <"+justArgs[0]+"> not found.");
			return;
		} catch(NumberFormatException e) {
			System.err.println("Invalid value of N - " + justArgs[1] + " or Ns - " + justArgs[2]);
			return;
		}
		
		IDynamicBayesNet network = DynamicNetworkFactory.newDynamicBayesNet(o[0].length);
		Vector<MHMMChild> children = new Vector<MHMMChild>();
		int T = o[0].length;
		for(int i = 0; i < o.length; i++)
		{
			int cardinality = 1;
			for(int j = 0; j < T; j++)
				cardinality = Math.max(cardinality, o[i][j]+1);
			IFDiscDBNNode nd = network.addDiscreteNode("Y"+i, cardinality);
			children.add(new MHMM_BasicChild(nd));
			nd.setAdvanceDistribution(new DiscreteCPT(obsmat(Ns, cardinality),cardinality));
			nd.setValue(o[i], 0);
		}

		MHMMController cont = new MHMMController(network, children, new Priors(Ns), Ns);
		FMModelOptions opts = new FMModelOptions(cont, N);

		String cno = "";
		try
		{
			cno = "emiterations";
			if(line.hasOption("emiterations"))
				opts.maxLearnIterations = Integer.parseInt(line.getOptionValue("emiterations"));
			cno = "emconvergence";
			if(line.hasOption("emconvergence"))
				opts.learnConv = Double.parseDouble(line.getOptionValue("emconvergence"));
			cno = "maxiterations";
			if(line.hasOption("maxiterations"))
				opts.maxAssignmentIterations = Integer.parseInt(line.getOptionValue("maxiterations"));
		} catch(NumberFormatException nfe) {
			System.err.println("Invalid option "+cno+"="+line.getOptionValue(cno));
		}
		
		if(line.hasOption("verbose"))
			cont.setTrace(System.out);
		cont.setLogger(System.out);

		PrintStream outfile = null;
		try {
			if(line.hasOption("output"))
			{
				String outfileName = line.getOptionValue("output");
				outfile = new PrintStream(new File(outfileName));
			}
		} catch(FileNotFoundException e) {
			System.err.println("Couldn't write to output file " + line.getOptionValue("output"));
		}
		
		FixedMixture.learnFixedMixture(opts);
		
		if(outfile!=null)
		{
			network.print(outfile);
			outfile.flush();outfile.close();
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
					mat[i][j] = .9;
				else
					mat[i][j] = .1/(no-1);
			}
		}
		return mat;
	}
	
	private static int[][] loadData(String file) throws FileNotFoundException
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
					return null;
				dat[i][j] = scan.nextInt();
			}
		}
		return dat;
	}
}