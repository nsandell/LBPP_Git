package bn.impl;

import java.io.PrintStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

import bn.impl.DBNFragment.FragmentSpot;
import bn.impl.IDBNFragmentServer.IRemoteDBNFragment;
import bn.impl.IDBNFragmentServer.RemoteCallback;

import bn.BNException;
import bn.IBayesNode;
import bn.IDiscreteDynBayesNode;
import bn.IDynBayesNet;
import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.messages.Message;

public class CompositeDBN implements IDynBayesNet {

	public CompositeDBN(int T, String baseref, double[] breakdown, IDBNFragmentServer[] fragmentServers) throws BNException
	{
		double sum = 0;
		for(double ent : breakdown)
			sum += ent;
		if(Math.abs(1-sum) > 1e-10)
			throw new BNException("Unable to distribute DBN, breakdown percentages don't sum to 1.");
		if(breakdown.length != fragmentServers.length)
			throw new BNException("Unable to distribute DBN, breakdown vector differs in length from number of fragment servers.");
	
		int[] lengths = new int[breakdown.length];
		this.T = T;
		this.fragments = new IRemoteDBNFragment[breakdown.length*2-1];
		int runningSum = 0;
		for(int i = 0; i < breakdown.length-1; i++)
		{
			lengths[i] = (int)(breakdown[i]*(T-(lengths.length-1)));
			runningSum += lengths[i];
		}
		lengths[breakdown.length-1] = T - runningSum - (lengths.length-1);
		
		try
		{
			this.time_offsets = new int[this.fragments.length];
			this.time_offsets[0] = 0;
			this.fragments[0] = new DBNFragment(lengths[0],FragmentSpot.Front);
			fragmentServers[0].newDBNFragment(baseref+0, lengths[0], FragmentSpot.Front);
			this.localFragment = new DBNFragment(1,FragmentSpot.Middle);
			this.fragments[1] = this.localFragment;
			this.time_offsets[1] = lengths[0]; int cume = lengths[0]+1;
			for(int i = 1; i < lengths.length-1; i++)
			{
				this.fragments[2*i] = fragmentServers[i].newDBNFragment(baseref+"i", lengths[i], FragmentSpot.Middle); time_offsets[2*i] = cume; cume += lengths[i];
				this.fragments[2*i+1] = new DBNFragment(1,FragmentSpot.Middle); time_offsets[2*i+1] = cume; cume++;
			}
			this.fragments[2*(lengths.length-1)] = fragmentServers[lengths.length-1].newDBNFragment(baseref+lengths.length, lengths[lengths.length-1], FragmentSpot.Back);
			this.time_offsets[2*(lengths.length-1)] = cume;
			
		} catch(RemoteException e) {
			throw new BNException("Failure to create composite DBN, remote error : " + e.getMessage());
		}
	}
	
	
	public void print(){
		//TODO Implement
	}
	
	public void print(PrintStream ps){
		//TODO Implement
	}
	
	public String getDefinition(){return null;}//TODO ???
	
	@Override
	public double getLogLikelihood()
	{
		//TODO Implement this.
		return 0;
	}
	
	public double getBetheEnergy()
	{
		//TODO implement
		return 0;
	}
	
	@Override
	public double getHMMLLAppx()
	{
		return 0; //TODO Implement
	}
	
	private DBNFragment localFragment;

	
	@Override
	public void removeNode(String name) throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
			{
				fragments[i].removeNode(name);
			}
		} catch(RemoteException e) {
			//TODO Need to be able to access host information for each fragment for diagnostic purposes.
			throw new BNException("Remote invocation error while removing node : " + e.getMessage());
		}
		nodeNames_local.remove(name);
	}

	@Override
	public void removeNode(IBayesNode node) throws BNException {
		this.removeNode(node.getName());
	}

	@Override
	public void resetMessages() {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].resetMessages();
		} catch(RemoteException e) {
			System.err.println("Error resetting messages on a fragment : "  + e.getMessage());
		}
	}

	@Override
	public int numNodes() {
		return nodeNames_local.size();
	}

	@Override
	public void setDistribution(String nodeName, Distribution dist)
	throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].setDistribution(nodeName, dist);
		} catch(RemoteException e) {
			throw new BNException("RMI Error while setting distribution :" + e.getMessage());
		}
	}

	@Override
	public Iterable<String> getNodeNames()
	{
		return nodeNames_local;
	}

	@Override
	public IBayesNode getNode(String name) throws BNException
	{
		throw new BNException("Get node operation unsupported for composite DBN.");
	}

	@Override
	public void sample() throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].sample();
		} catch(RemoteException e) {
			throw new BNException("RMI Error while sampling : " + e.getMessage());
		}
	}
	
	@Override
	public void sample(String node) throws BNException
	{
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].sample(node);
		} catch(RemoteException e) {
			throw new BNException("RMI Error while sampling : " + e.getMessage());
		}
	}

	@Override
	public void clearAllEvidence() {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].clearAllEvidence();
		} catch(RemoteException e) {
			//TODO Consider adding throws clauses to all methods to avoid this crap
			System.err.println("RMI Error while clearing all evidence : " + e.getMessage());
		}
	}

	@Override
	public void collectSufficientStatistics(Iterable<String> nodeNames,
			HashMap<String, SufficientStatistic> stats) throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].collectSufficientStatistics(nodeNames, stats);
		} catch(RemoteException e) {
			throw new BNException("RMI Error while collecting sufficient statistics..");
		}
	}

	@Override
	public void optimize(Iterable<String> nodenames,
			HashMap<String, SufficientStatistic> stats) throws BNException {
		//TODO Figure out optimization.  Something like collect sufficient stats, and distribute them back.
		throw new BNException("Not yet implemented for Composite DBNs.");
	}

	@Override
	public bn.IBayesNet.RunResults optimize(int learnIt, double learnErr,
			int runIt, double runErr) throws BNException {
		//TODO Figure out optimization.  Something like collect sufficient stats, and distribute them back.
		throw new BNException("Not yet implemented for Composite DBNs.");
	}

	@Override
	public void validate() throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].validate();
		} catch(RemoteException e) {
			throw new BNException("RMI error while validting : " + e.getMessage());
		}
	}

	@Override
	public void printDistributionInfo(String name, PrintStream ps)
	throws BNException {
		// TODO not sure about this.. what would be most efficient.
		throw new BNException("Unimplemented for Composite DBNs.");
	}

	@Override
	public bn.IBayesNet.RunResults run(int maxit, double convergence)
	throws BNException {
		double err = Double.MAX_VALUE;
		int its = 0;
		double ts = System.currentTimeMillis();
		while(err > convergence)
		{
			err = 0;
			try {
			for(int i = 0; i < fragments.length; i+=2)
				err = Math.max(err,fragments[i].run(1, 0).error);
			for(int i = 1; i < fragments.length; i+=2)
			{
				this.fragments[i].syncBwdIntf(this.fragments[i-1].getFwdInterface());
				this.fragments[i].syncFwdIntf(this.fragments[i+1].getBwdInterface());
				this.fragments[i].run(1, 0);
			}
			for(int i = 1; i < fragments.length; i+=2)
			{
				this.fragments[i-1].syncFwdIntf(this.fragments[i].getBwdInterface());
				this.fragments[i+1].syncBwdIntf(this.fragments[i].getFwdInterface());
			}
			} catch(RemoteException e) {
				throw new BNException("Error running a fragment...");
			}
			its++;
		}
		return new RunResults(its,(ts-System.currentTimeMillis())/1000.0, err);
		//return this.run_parallel_block(maxit, convergence);
	}

	@Override
	public void clearEvidence(String node) throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].clearEvidence(node);
		} catch(RemoteException e) {
			throw new BNException("RMI error while clearing node evidence : " + e.getMessage());
		}
	}

	@Override
	public void addInterEdge(String from, String to) throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].addInterEdge(from, to);
		} catch(RemoteException e) {
			throw new BNException("RMI error while adding interedge : " + e.getMessage());
		}
	}

	@Override
	public void addIntraEdge(String from, String to) throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].addIntraEdge(from, to);
		} catch(RemoteException e) {
			throw new BNException("RMI error while adding intraedge : " + e.getMessage());
		}
	}

	@Override
	public void addInterEdge(IBayesNode from, IBayesNode to) throws BNException {
		this.addInterEdge(from.getName(), to.getName());
	}

	@Override
	public Message getMarginal(String nodename, int t) throws BNException {
		try {
			int idx = this.getFragmentIndex(t);
			return fragments[idx].getMarginal(nodename, t-this.time_offsets[idx]);
		} catch(RemoteException e) {
			throw new BNException("RMI error while getting marginal" + e.getMessage());
		}
	}

	@Override
	public void addIntraEdge(IBayesNode from, IBayesNode to) throws BNException {
		this.addIntraEdge(from.getName(), to.getName());
	}

	@Override
	public void removeInterEdge(String from, String to) throws BNException {
		try { 
			for(int i = 0; i < fragments.length; i++)
				fragments[i].removeInterEdge(from, to);
		} catch(RemoteException e) {
			throw new BNException("RMI Error while removing interedge : " + e.getMessage());
		}
	}

	@Override
	public void removeIntraEdge(String from, String to) throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].removeIntraEdge(from, to);
		} catch(RemoteException e) {
			throw new BNException("RMI error while removing intraedge : " + e.getMessage());
		}
	}

	@Override
	public void removeInterEdge(IBayesNode from, IBayesNode to)
	throws BNException {
		this.removeInterEdge(from.getName(), to.getName());
	}

	@Override
	public void removeIntraEdge(IBayesNode from, IBayesNode to)
	throws BNException {
		this.removeIntraEdge(from.getName(), to.getName());
	}

	@Override
	public boolean existsInterEdge(String fromName, String toName)
	throws BNException {
		try {
			return fragments[0].existsInterEdge(fromName, toName);
		} catch(RemoteException e) {
			throw new BNException("RMI error while testing edge existence : " + e.getMessage());
		}
	}

	@Override
	public boolean existsIntraEdge(String fromName, String toName)
	throws BNException {
		try {
			return fragments[0].existsIntraEdge(fromName, toName);
		} catch(RemoteException e) {
			throw new BNException("RMI error while testing edge existence : " + e.getMessage());
		}
	}

	@Override
	public IDiscreteDynBayesNode addDiscreteNode(String name, int cardinality)
	throws BNException {
		try {
			for(int i = 0; i < fragments.length; i++)
				fragments[i].addDiscreteNodeRemote(name, cardinality);
			return null; //TODO need some sort of composite node?
		} catch(RemoteException e) {
			throw new BNException("RMI error while adding discrete node : " + e.getMessage());
		}
	}

	@Override
	public int getT() {
		return this.T;
	}

	@Override
	public void setInitialDistribution(String nodeName, Distribution dist)
	throws BNException {
		try {
			fragments[0].setInitialDistribution(nodeName, dist);
		} catch(RemoteException e) {
			throw new BNException("RMI error while setting initial distribution : " + e.getMessage());
		}
	}

	@Override
	public void run_parallel(int maxIt, double conv, ParallelCallback callback) {
		try {
		new Thread(new FragmentParallelIterator(this, callback, maxIt, conv)).start();
		} catch(RemoteException e) {
			System.err.println("Remote exception while attempting to run parallel : " + e.getMessage());
			e.printStackTrace();
		}
	}
	

	private static class FragmentParallelIterator extends UnicastRemoteObject implements RemoteCallback, Runnable
	{

		public FragmentParallelIterator(CompositeDBN dbn, ParallelCallback finalCallback, int maxIt, double maxErr) throws RemoteException
		{
			this.cdbn = dbn;
			this.pcb = finalCallback;
			this.maxErr = maxErr;
			this.maxIt = maxIt;
		}
		
		private void initializeIt()
		{
			this.err = 0;
			this.numExpected = 1 + this.cdbn.fragments.length/2;
		}
		
		public void run()
		{
			this.time = System.currentTimeMillis();
			this.runStep();
		}

		private void runStep()
		{
			try  {
				this.initializeIt();
				for(int i = 0; i < this.cdbn.fragments.length; i+=2)
					this.cdbn.fragments[i].run_parallel(2, 0,(RemoteCallback)this);
			} catch(RemoteException re) {
				this.pcb.error(this.cdbn, "Remote error while running inference : " + re.getMessage());
			}
		}

		@Override
		public void callback(IRemoteDBNFragment net, int numIts, double err,
				double timeElapsed) {
			this.numExpected--;
			this.err = Math.max(err, this.err);
			if(numExpected==0)
			{
				if(errors!=null)
					this.callback();
				try
				{
					for(int i = 1; i < this.cdbn.fragments.length; i+= 2)
					{
						this.cdbn.fragments[i].syncBwdIntf(this.cdbn.fragments[i-1].getFwdInterface());
						this.cdbn.fragments[i].syncFwdIntf(this.cdbn.fragments[i+1].getBwdInterface());
					}
					for(int i = 1; i < this.cdbn.fragments.length; i+= 2)
						this.err = Math.max(this.err,this.cdbn.fragments[i].run(1, 0).error);
					for(int i = 1; i < this.cdbn.fragments.length; i+= 2)
					{
						this.cdbn.fragments[i-1].syncFwdIntf(this.cdbn.fragments[i].getBwdInterface());
						this.cdbn.fragments[i+1].syncBwdIntf(this.cdbn.fragments[i].getFwdInterface());
					}
				} catch(Exception e)
				{
					this.error(net, "Error while performing inference on borders : " + e.getMessage());
				}
				this.iterations++;
				if(this.iterations >= this.maxIt || this.err <= this.maxErr)
					this.callback();
				else
					this.runStep();
			}
		}

		private void callback()
		{
			if(errors!=null)
				this.pcb.error(this.cdbn, this.errors);
			else
				this.pcb.callback(this.cdbn, this.iterations, this.err, (System.currentTimeMillis()-this.time)/1000.0);
		}

		@Override
		public void error(IRemoteDBNFragment net, String error) {
			this.numExpected--;
			if(errors==null)
				errors = "Error: " + error;
			else
				errors += "\nError: " + error;
			if(numExpected==0)
				this.callback();
		}

		CompositeDBN cdbn;
		private String errors = null;
		private int numExpected;
		private int iterations = 0;
		private int maxIt;
		private double maxErr;
		private double time = 0;
		private double err = 0;
		private ParallelCallback pcb;
		
		private static final long serialVersionUID = 50L;
	}

	@Override
	public bn.IBayesNet.RunResults run_parallel_block(int maxit, double conv) throws BNException {
		DynamicBayesianNetwork.BlockCallback2 cb = new DynamicBayesianNetwork.BlockCallback2();
		synchronized (cb.blockLock) {
			this.run_parallel(maxit, conv, cb);
			try{cb.blockLock.wait();}catch(InterruptedException e){System.err.println("Interrupted..");}
		}
		if(cb.error!=null)
			throw new BNException(cb.error);
		return new RunResults(cb.numIts, cb.timeElapsed, cb.errorD);
	}

	@Override
	public bn.IBayesNet.RunResults optimize_parallel(int maxit, double conv,
			int infMaxIt, double infConv) throws BNException {
		//TODO figure out optimization
		throw new BNException("Unsupported on CompositeDBNs presently.");
	}

	@Override
	public void setEvidence(String nodeName, int t0, Object[] evidence)
	throws BNException {
		try {
			Object[][] subevidence;

			int first_index = this.getFragmentIndex(t0);
			int last_index = this.getFragmentIndex(t0+evidence.length-1);
			subevidence = new Object[last_index-first_index+1][];
			int ci = 0;
			for(int i = first_index; i <= last_index; i++)
			{
				int subt0 = 0;
				if(i==first_index)
				{
					subt0 = t0-this.time_offsets[i];
				}
				subevidence[i] = new Object[fragments[i].getT()];
				for(int t = 0; t < subevidence[i].length; t++)
				{
					subevidence[i][t] = evidence[ci];
					ci++;
				}
				fragments[i].setEvidence(nodeName, subt0, subevidence[i]);
			}
		} catch(RemoteException e) {
			throw new BNException("RMI error while setting evidence : " + e.getMessage());
		}
	}

	@Override
	public void setEvidence(String nodeName, int t, Object evidence)
	throws BNException {
		try {
			int idx = this.getFragmentIndex(t);
			fragments[idx].setEvidence(nodeName,t-time_offsets[idx],evidence);
		} catch(RemoteException e) {
			throw new BNException("RMI Erorr while setting evidence : " + e.getMessage());
		}
	}

	private int getFragmentIndex(int t) throws BNException
	{
		for(int i = 0; i < fragments.length-1; i++)
		{
			if(t < time_offsets[i+1])
				return i;
		}
		return fragments.length-1;
	}

	private ArrayList<String> nodeNames_local = new ArrayList<String>();
	private IRemoteDBNFragment[] fragments;
	private int[] time_offsets;
	private int T;
}
