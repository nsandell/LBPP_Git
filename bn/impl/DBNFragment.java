package bn.impl;

import java.rmi.RemoteException;

import java.util.HashMap;

import java.util.Map.Entry;

import bn.BNException;
import bn.IDiscreteDynBayesNode;
import bn.IDynBayesNet;
import bn.impl.IDBNFragmentServer.IRemoteDBNFragment;
import bn.impl.IDBNFragmentServer.RemoteCallback;
import bn.messages.Message.MessageInterface;

public class DBNFragment extends DynamicBayesianNetwork implements IDBNFragmentServer.IRemoteDBNFragment
{
	public static enum FragmentSpot
	{
		Front,
		Middle,
		Back
	}
	
	public DBNFragment(int T, FragmentSpot spot)
	{
		super(T);
		this.fragSpot = spot;
		if(spot!=FragmentSpot.Back)
			this.fwdMessageInterface = new HashMap<String, HashMap<String,MessageInterface>>();
		if(spot!=FragmentSpot.Front)
			this.bwdMessageInterface = new HashMap<String, HashMap<String,MessageInterface>>();
	}
	
	@Override
	public IDiscreteDynBayesNode addDiscreteNode(String name, int card) throws BNException
	{
		if(this.nodes.get(name)!=null)
			throw new BNException("Node with name " + name + " already exists.");
		DBNFragmentNode.DiscreteDBNFNode nd = new DBNFragmentNode.DiscreteDBNFNode(this, name, card);
		this.nodes.put(name,nd);
		return nd;
	}
	
	public void run_parallel(int maxit, double conv, RemoteCallback cb) throws RemoteException
	{
		this.run_parallel(maxit, conv, new RemoteCallbackChain(cb, this));
	}

	private static class RemoteCallbackChain implements ParallelCallback
	{
		
		public RemoteCallbackChain(RemoteCallback rcb, IRemoteDBNFragment frag)
		{
			this.rcb = rcb;
			this.frag = frag;
		}

		@Override
		public void callback(IDynBayesNet net, int numIts, double err,
				double timeElapsed) {
			try {
				this.rcb.callback(this.frag, numIts, err, timeElapsed);
			} catch(RemoteException e) {
				System.err.println("Error calling back home : " + e.getMessage()); //TODO Figure out what to do in these situations.. needs to be robust.
			}
		}

		@Override
		public void error(IDynBayesNet net, String error) {
			try {
				this.rcb.error(this.frag, error);
			} catch(RemoteException e) {
				System.err.println("Error calling back home (for error " + error + ") : " + e.getMessage()); //TODO Figure out what to do in these situations.. needs to be robust.
			}
		}
		
		IRemoteDBNFragment frag;
		RemoteCallback rcb;
	}

	
	public void addDiscreteNodeRemote(String name, int cardinality) throws BNException
	{
		this.addDiscreteNode(name, cardinality);
	}
	
	public void syncBwdIntf(HashMap<String,HashMap<String,MessageInterface>> newBwd) throws BNException
	{
		for(Entry<String, HashMap<String,MessageInterface>> ent : newBwd.entrySet())
		{
			HashMap<String,MessageInterface> inner = this.bwdMessageInterface.get(ent.getKey());
			if(inner==null)
				throw new BNException("Failed to sync fragment interfaces...");
			for(Entry<String,MessageInterface> ent2 : ent.getValue().entrySet())
			{//TODO consider stripping out exceptions when it's working
				MessageInterface innerMost = inner.get(ent2.getKey());
				if(innerMost==null)
					throw new BNException("Failed to sync fragment interfaces...");
				//innerMost.pi.adopt(ent2.getValue().pi);  //TODO Gotta figure this shit out.
			}
		}
	}
	
	public void syncFwdIntf(HashMap<String,HashMap<String,MessageInterface>> newFwd) throws BNException
	{
		for(Entry<String, HashMap<String,MessageInterface>> ent : newFwd.entrySet())
		{
			HashMap<String,MessageInterface> inner = this.fwdMessageInterface.get(ent.getKey());
			if(inner==null)
				throw new BNException("Failed to sync fragment interfaces...");
			for(Entry<String,MessageInterface> ent2 : ent.getValue().entrySet())
			{//TODO consider stripping out exceptions when it's working
				MessageInterface innerMost = inner.get(ent2.getKey());
				if(innerMost==null)
					throw new BNException("Failed to sync fragment interfaces...");
				//innerMost.lambda.adopt(ent2.getValue().lambda);  //TODO Gotta figure this shit out
			}
		}
	}
	
	public HashMap<String,HashMap<String,MessageInterface>> getFwdInterface()
	{
		return this.fwdMessageInterface;
	}
	
	public HashMap<String,HashMap<String,MessageInterface>> getBwdInterface()
	{
		return this.bwdMessageInterface;
	}
	
	double run_one_it() throws BNException
	{
		double err = 0;
		for(String nodeName: nodes.keySet())
		{
			DBNNode node = nodes.get(nodeName);
			try{err = Math.max(err,node.updateMessages());}
			catch(BNException e){throw new BNException("Node " + nodeName + " threw an exception while updating : ",e);}
		}
		return err;
	}
	
	HashMap<String,HashMap<String,MessageInterface>> fwdMessageInterface;
	HashMap<String,HashMap<String,MessageInterface>> bwdMessageInterface;
	FragmentSpot fragSpot;
}
