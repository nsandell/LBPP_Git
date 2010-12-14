package bn.impl;

import java.rmi.Remote;

import java.rmi.RemoteException;
import java.util.HashMap;

import bn.BNException;
import bn.IBayesNet.RunResults;
import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.messages.Message;
import bn.messages.Message.MessageInterface;

public interface IDBNFragmentServer extends Remote
{
	public interface IRemoteDBNFragment extends Remote
	{
		public void removeNode(String name) throws BNException, RemoteException;
		public void resetMessages() throws RemoteException;
		public int numNodes() throws RemoteException;
		public void setDistribution(String nodeName, Distribution dist) throws BNException, RemoteException;
		public Iterable<String>	getNodeNames() throws RemoteException;
		public void sample() throws BNException, RemoteException;
		public void sample(String name) throws BNException, RemoteException;
		public void clearAllEvidence() throws RemoteException;
		public void collectSufficientStatistics(Iterable<String> nodeNames, HashMap<String,SufficientStatistic> stats) throws BNException, RemoteException;
		public void optimize(Iterable<String> nodenames, HashMap<String,SufficientStatistic> stats) throws BNException, RemoteException;
		public RunResults optimize(int learnIt, double learnErr, int runIt, double runErr) throws BNException, RemoteException;
		public void validate() throws BNException, RemoteException;
		public RunResults run(int maxit, double convergence) throws BNException, RemoteException;
		public void clearEvidence(String node) throws BNException, RemoteException;
		public void syncFwdIntf(HashMap<String,HashMap<String,MessageInterface>> newFwd) throws BNException, RemoteException;
		public void syncBwdIntf(HashMap<String,HashMap<String,MessageInterface>> newBwd) throws BNException, RemoteException;
		
		
		public HashMap<String,HashMap<String,MessageInterface>> getFwdInterface() throws RemoteException;
		public HashMap<String,HashMap<String,MessageInterface>> getBwdInterface() throws RemoteException;

		void addInterEdge(String from, String to) throws BNException, RemoteException;
		void addIntraEdge(String from, String to) throws BNException, RemoteException;
		public Message getMarginal(String nodename, int t) throws BNException, RemoteException;
		void removeInterEdge(String from, String to) throws BNException, RemoteException;
		void removeIntraEdge(String from, String to) throws BNException, RemoteException;
		boolean existsInterEdge(String fromName, String toName) throws BNException, RemoteException;
		boolean existsIntraEdge(String fromName, String toName) throws BNException, RemoteException;
		void addDiscreteNodeRemote(String name, int cardinality) throws BNException, RemoteException;
		int getT() throws RemoteException;
		void setInitialDistribution(String nodeName, Distribution dist) throws BNException, RemoteException;
		public RunResults run_parallel_block(int maxit, double conv) throws BNException, RemoteException;
		public void run_parallel(int maxit, double conv, RemoteCallback cb) throws RemoteException;
		void setEvidence(String nodeName, int t0, Object[] evidence) throws BNException, RemoteException;
		void setEvidence(String nodeName, int t, Object evidence) throws BNException, RemoteException;
	}

	IRemoteDBNFragment newDBNFragment(String refID, int T, DBNFragment.FragmentSpot spot) throws RemoteException;
	void removeDBNFragment(String refID) throws BNException, RemoteException;
	void clearFragments() throws RemoteException;
	
	public static interface RemoteCallback extends Remote
	{
		public void callback(IRemoteDBNFragment frag, int numIts, double err, double timeElapsed) throws RemoteException;
		public void error(IRemoteDBNFragment frag, String error) throws RemoteException;
	}
}
