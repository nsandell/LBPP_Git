package complex.prepacked;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
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
import bn.dynamic.IDynamicBayesNet;
import bn.impl.dynbn.DynamicNetworkFactory;

import complex.CMException;
import complex.latents.FiniteMarkovChain;
import complex.mixture.FixedMixture;
import complex.mixture.FixedMixture.FMModelOptions;
import complex.mixture.IMixtureChild;
import complex.mixture.controllers.MHMMController;

public class MHMM
{
	public static interface IMixtureChildFactory
	{
		public IMixtureChild getChild(IDynamicBayesNet net, int nameidx, int ns, int[] observations);
		public void setArg(String arg) throws CMException;
	}
	
	public static void mhmm_main(String[] args, HashMap<String, IMixtureChildFactory> childFactories) throws BNException, CMException
	{
		Options options = new Options();
		options.addOption("v", "verbose", false, "Use verbose mode.");
		Option opt;
		
		opt = new Option("emiterations","Maximum number of iterations to perform in expectation-maximization.");
		opt.setArgs(1);opt.setArgName("#Iterations");
		options.addOption(opt);
		
		opt = new Option("emconvergence","Convergence threshold for expectation maximization.");
		opt.setArgs(1);opt.setArgName("threshold");
		options.addOption(opt);
		
		opt = new Option("i","maxiterations",true,"Maximum number of times to run through the observables to change assignments.");
		opt.setArgs(1);opt.setArgName("#Iterations");
		options.addOption(opt);
		
		opt = new Option("output","o",true,"Model output file");
		opt.setArgs(1);opt.setArgName("file");
		options.addOption(opt);
		
		opt = new Option("modelTraceFilebase","m",true,"Base file name if we wish to have a model printed to file every iteration.");
		opt.setArgs(1);opt.setArgName("base file name");
		options.addOption(opt);
		
		opt = new Option("priorFile", "p",true,"File name that contains description of A and pi matrix priors.");
		opt.setArgs(1);opt.setArgName("prior file name");
		options.addOption(opt);
		
		opt = new Option("obsType", true, "Name of the type of observation, followed by '.' and an argument name, if necessary.");
		opt.setArgs(1);opt.setArgName("observation type");
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
				System.err.println("Error loading observation file - provided dimensions are incorrect.");
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
		
		String ctype = "default";
		String carg = null;
		if(line.hasOption("obsType"))
		{
			String[] bits = line.getOptionValue("obsType").split("\\.", 2);
			ctype = bits[0];
			if(bits.length==2)
				carg = bits[1];
		}
		
		if(childFactories.get(ctype)==null)
		{
			System.err.println("Error - unknown observation prior type '"+ctype+"'");
			System.err.println("Valid options are: ");
			for(String ctype2 : childFactories.keySet())
				System.err.println("\t" + ctype2);
			return;
		}
		else if(carg!=null)
			childFactories.get(ctype).setArg(carg);
		
		IDynamicBayesNet network = DynamicNetworkFactory.newDynamicBayesNet(o[0].length);
		Vector<IMixtureChild> children = new Vector<IMixtureChild>();
		for(int i = 0; i < o.length; i++)
		{
			IMixtureChild child = childFactories.get(ctype).getChild(network,i,Ns,o[i]);
			if(child==null)
				return;
			children.add(child);
		}
		
		FiniteMarkovChain.FiniteMCPrior prior = null;
		
		//TODO Allow for specification of priors...
		/*
		Priors priors;
		if(line.hasOption("priorFile"))
		{
			try {
				priors = new Priors(line.getOptionValue("priorFile"));
			} catch(IOException e){System.err.println("Cannot load MHMM priors from file " + line.getOptionValue("priorFile"));return;}
		}
		else
			priors = new Priors(Ns);
		*/
		
		// Default prior uniform with this starting point
		if(prior==null)
		{
			double[] piv = new double[Ns]; piv[0] = .9;
			for(int j = 1; j < piv.length; j++)
				piv[j] = (.1)/(piv.length-1);
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
			
			prior = new FiniteMarkovChain.UniformFiniteMCPrior(
					new DiscreteCPT(Av, Ns),new DiscreteCPTUC(piv),Ns);
		}
		
		MHMMController cont = new MHMMController(network, children, new FiniteMarkovChain.FiniteMarkovChainFactory(prior));
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
		
		if(line.hasOption("modelTraceFilebase"))
			opts.modelBaseName = line.getOptionValue("modelTraceFilebase");
		
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
