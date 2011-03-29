package complex.featural;

import java.util.HashMap;

import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import complex.CMException;
import complex.IParentProcess;

import complex.featural.ProposalAction.UniqueParentAddAction;
import complex.featural.ProposalGenerator.Proposal;

import util.MathUtil;

public class IBPMixture<ChildProcess extends IFeaturalChild,ParentProcess extends IParentProcess> {
	
	public IBPMixture(Vector<ProposalGenerator<ChildProcess,ParentProcess>> generators, double[] p_gens, double[] p) throws CMException
	{
		if(generators.size()!=p_gens.length)
			throw new CMException("Provided different number of generators from generator probabilities");
		double sum = 0;
		for(double dub : p_gens)
			sum+=dub;
		if(Math.abs(sum-1) > 1e-12)
			throw new CMException("Provided generator probability distribution not summing to one!");
		if(Math.abs(p[0]+p[1]+p[2]-1) > 1e-12)
			throw new CMException("Provided move vs vertical vs horizontal distribution not summing to one!");
		
		this.accepted_genprops = new int[generators.size()];
		this.generators = generators;
		this.generator_probs = p_gens;
		this.main_probs = p;
	}
	
	public static class IBPMModelOptions<ChildProcess extends IFeaturalChild,ParentProcess extends IParentProcess>
	{
		public IBPMModelOptions(FeaturalModelController<ChildProcess,ParentProcess> controller, boolean[][] initAssign)
		{
			this.controller = controller;
			this.initialAssignments = initAssign;
		}
		
		public static boolean[][] randomAssignment(int obs, int lat, double p)
		{
			boolean[][] assign = new boolean[obs][lat];
			
			for(int j = 0; j < obs; j++)
			{
				boolean hasAssignment = false;
				for(int i = 0; i < lat; i++)
				{
					if(MathUtil.rand.nextDouble() < p)
					{
						assign[j][i] = true;
						hasAssignment = true;
					}
					else
					{
						assign[j][i] = false;
					}
				}
				if(!hasAssignment)
				{
					int feature = MathUtil.rand.nextInt(lat);
					assign[j][feature] = true;
				}
			}
			return assign;
		}
		
		public String savePath = null;
		public int numSave = 0;
		public double mll = Double.NEGATIVE_INFINITY;
	
		public int max_run_it = 30, max_learn_it = 5;
		public double run_conv = 1e-6, learn_conv = 1e-6;
		
		public FeaturalModelController<ChildProcess,ParentProcess> controller;

		public double alpha = 1;						// Indian Buffet
		public boolean[][] initialAssignments;			// Feature matrix initialization
		public int maxIterations = Integer.MAX_VALUE;   // maximum possible number of iterations
	}
	
	private static class ShutdownThread extends Thread
	{
		
		public void run() {
			System.out.println("Learning process manually terminated..");
			mix.keepGoing = false;
			try {
				mix.workThr.join();
			} catch( InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		public ShutdownThread(IBPMixture<?,?> mix)
		{
			this.mix = mix;
		}
		IBPMixture<?,?> mix;
	}
	
	private volatile Thread workThr;
	private volatile boolean keepGoing = true;
	public void learn(IBPMModelOptions<ChildProcess,ParentProcess> opts) throws CMException
	{
		this.keepGoing = true;
		this.workThr = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));
		FeaturalModelController<ChildProcess,ParentProcess> cont = opts.controller;
		int M = opts.initialAssignments[0].length;	// The number of latent processes
		
		Vector<ChildProcess> obs = cont.getObservedNodes();
		Vector<ParentProcess> lats = cont.getLatentNodes();
	
		for(int i = 0; i < M; i++)
		{
			ParentProcess latent = cont.newLatentModel();
			int counter = 0;
			for(ChildProcess child : cont.getObservedNodes())
			{
				if(opts.initialAssignments[counter][i])
					cont.connect(latent, child);
				counter++;
			}
		}
		
		double ll = cont.learn(opts.max_learn_it, opts.learn_conv, opts.max_run_it,opts.run_conv) + structureLL(cont,opts);
		double bestLL = ll;
		cont.saveInfo(opts.savePath + "/initial_iteration",ll);
		cont.log("Learning run started: Initial Log Likelihood = " + ll + "   (" + this.structureLL(cont, opts) + " structural).");
		
		for(int iteration = 0; iteration < opts.maxIterations && keepGoing; iteration++)
		{
			int choice = MathUtil.discreteSample(this.main_probs);
			if(choice==0)
			{
				choice = MathUtil.rand.nextInt(obs.size());
				ChildProcess hsn = obs.get(choice);
				cont.log("Starting horizontal sample run for node : " + hsn.getName());
				
				HashSet<ParentProcess> parents = cont.getParents(hsn);
				
				for(ParentProcess latNd : lats)
				{
					if(parents.contains(latNd))
						ll = this.attemptDisconnect(latNd, hsn, cont, opts, ll);
					else
						ll = this.attemptConnect(latNd, hsn, cont, opts, ll);
				}
				
				//Attempt to add unique parent
				if(MathUtil.rand.nextDouble() < 1)
				{	
					cont.log("Attempting to add unique parent to node " + hsn.getName());
					UniqueParentAddAction<ChildProcess,ParentProcess> act = new UniqueParentAddAction<ChildProcess,ParentProcess>(hsn);
					act.perform(cont);
					//double newLL = cont.run(opts.max_run_it, opts.run_conv) + structureLL(cont,opts);
					double newLL = cont.learn(opts.max_learn_it,opts.learn_conv,opts.max_run_it, opts.run_conv) + structureLL(cont,opts);
					if(accept(newLL,this.main_probs[0],ll,this.main_probs[0]+this.main_probs[1],cont))
						ll = newLL;
					else
						act.undo(cont);
				}
			}
			else if(choice==1 && lats.size() > 0)
			{
				choice = MathUtil.rand.nextInt(lats.size());
				ParentProcess vsn = lats.get(choice);
				cont.log("Starting vertical sample run for node : " + vsn.getName());

				HashSet<ChildProcess> children = cont.getChildren(vsn);

				for(ChildProcess obsNode : obs)
				{
					if(children.contains(obsNode))
						ll = this.attemptDisconnect(vsn, obsNode, cont, opts, ll);
					else
						ll = this.attemptConnect(vsn, obsNode, cont, opts, ll);
				}
			}
			else
			{
				choice = MathUtil.discreteSample(this.generator_probs);
				Proposal<ChildProcess,ParentProcess> proposal = generators.get(choice).generate(cont);
				
				
				if(proposal==null)
					continue;
				
				proposal.action().perform(cont);
				double newSLL = structureLL(cont,opts);
				double newLL = cont.learn(opts.max_learn_it, opts.learn_conv, opts.max_run_it, opts.run_conv) + newSLL;
				
				if(accept(newLL,proposal.forwardP(),ll,proposal.backwardP(),cont))
				{
					this.accepted_genprops[choice]++;
					ll = newLL;
				}
				else
					proposal.action().undo(cont);
			}
			
			for(int i = 0; i < lats.size(); i++)
			{
				if(cont.getChildren(lats.get(i)).size()==0)
					cont.killLatentModel(lats.get(i));
			}
			
			ll = cont.learn(opts.max_learn_it, opts.learn_conv, opts.max_run_it, opts.run_conv) + structureLL(cont, opts);
	
			if(opts.savePath!=null)
			{
				if(ll > bestLL)
				{
					bestLL = ll;
					cont.saveInfo(opts.savePath + "/iteration"+iteration,ll);
				}
			}
		}
		
		System.out.println("Accepted moves:");
		for(int i = 0; i < this.generators.size(); i++)
		{
			System.out.println("\t"+this.generators.get(i).name()+ " : " + this.accepted_genprops[i]);
		}
		System.out.println("Final LL : " + ll + "   (" + this.structureLL(cont, opts) + " structural).");

		for(int i = 0; i < obs.size(); i++)
		{
			for(int j = 0; j < lats.size(); j++)
			{
				if(cont.getChildren(lats.get(j)).contains(obs.get(i)))
					System.out.print("1\t");
				else
					System.out.print("0\t");
			}
			System.out.println();
		}
	}
	
	private double attemptDisconnect(ParentProcess latent, ChildProcess observed, FeaturalModelController<ChildProcess,ParentProcess> cont, IBPMModelOptions<ChildProcess,ParentProcess> opts, double ll) throws CMException
	{
		if(!cont.getChildren(latent).contains(observed))
			throw new CMException("Attempted to disconnect a latent node from an observed that was not its child!");
		cont.log("Attempting to disconnect observation " + observed.getName() + " from latent sequence " + latent.getName());
		
		observed.backupParameters();
		cont.disconnect(latent, observed);
		observed.optimize();
		
		double newLL = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont,opts);
		if(accept(newLL,this.main_probs[1]+this.main_probs[0],ll,this.main_probs[1]+this.main_probs[0],cont))
			ll = newLL;
		else
		{
			cont.connect(latent, observed);
			observed.restoreParameters();
		}
		
		return ll;
	}
	
	private double attemptConnect(ParentProcess latent, ChildProcess observed, FeaturalModelController<ChildProcess,ParentProcess> cont, IBPMModelOptions<ChildProcess,ParentProcess> opts, double ll) throws CMException
	{
		if(cont.getChildren(latent).contains(opts))
			throw new CMException("Attempted to connect parent " + latent.getName() + " to child " + observed.getName() + " when they already are connected.");
		cont.log("Attempting to connect observation " + observed.getName() + " to latent sequence " + latent.getName());
		
		observed.backupParameters();
		cont.connect(latent, observed);
		observed.optimize();
		
		double newLL = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont,opts);
		if(accept(newLL,this.main_probs[1]+this.main_probs[0],ll,this.main_probs[1]+this.main_probs[0],cont))
			ll = newLL;
		else
		{
			cont.disconnect(latent, observed);
			observed.restoreParameters();
		}
		
		return ll;
	}
	
	private boolean accept(double newLL, double pf, double oldLL, double pb, FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
	{
		boolean ret = (MathUtil.rand.nextDouble() < Math.exp(newLL+Math.log(pb)-oldLL-Math.log(pf)));
		if(ret)
			cont.log("Proposal accepted, new Log Likelihood : " + newLL);
		else
			cont.log("Proposal rejected.");
		return ret;
	}

	double cachedLNF = 0;
	double cachedAHN = 0;
	double cachedAHN_alpha = -1;
	int cachedAHN_id = -1;
	
	private double structureLL(FeaturalModelController<ChildProcess,ParentProcess> cont, IBPMModelOptions<ChildProcess,ParentProcess> opts)
	{
		int N = cont.observables.size();
		int K = cont.latents.size();
		
		if(cont.observables.size()!=cachedAHN_id || opts.alpha!=cachedAHN_alpha)
		{
			cachedLNF = 0;
			cachedAHN_alpha = opts.alpha;
			cachedAHN_id = cont.observables.size();
			cachedAHN = 0;
			
			for(double i = 1; i <= N; i++)
				cachedAHN += 1.0/i;
			for(int i = 2; i <= N; i++)
				cachedLNF += Math.log(i);
			cachedAHN *= cachedAHN_alpha;
		}
		
		double lladj = K*Math.log(opts.alpha);
		
		HashMap<HashSet<ChildProcess>, Integer> KNs = new HashMap<HashSet<ChildProcess>, Integer>();
		for(int i = 0; i < K; i++)
		{
			HashSet<ChildProcess> set = cont.getChildren(cont.getLatentNodes().get(i));
			boolean dupe = false;
			for(HashSet<ChildProcess> otherSet : KNs.keySet())
			{
				if(otherSet.containsAll(set) && set.containsAll(otherSet))
				{
					KNs.put(otherSet, KNs.get(otherSet)+1);
					dupe = true;
					break;
				}
			}
			if(!dupe)
				KNs.put(set, 1);
		}
		for(Map.Entry<HashSet<ChildProcess>, Integer> ent : KNs.entrySet())
			lladj -= (ent.getValue() > 1 ? Math.log(ent.getValue()) : 0);
		
		lladj -= cachedAHN;
		lladj -= K*cachedLNF;
		
		for(int i = 0; i < K; i++)
		{
			int mk = cont.getChildren(cont.latents.get(i)).size();
			
			for(int j = 2; j <= cont.observables.size()-mk; j++)
				lladj += Math.log(j);
			for(int j = 2; j <= mk-1; j++)
				lladj += Math.log(j);
		}
		if(lladj > 0)
		{
			this.structureLL(cont, opts);
		}
		return lladj;
	}
	
	private Vector<ProposalGenerator<ChildProcess,ParentProcess>> generators;
	private double[] generator_probs;
	private int[] accepted_genprops;
	private double[] main_probs;
}
