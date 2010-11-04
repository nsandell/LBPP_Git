package bn.impl;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import bn.BNException;
import bn.impl.DBNFragment.FragmentSpot;

public class DBNFragmentServer  extends UnicastRemoteObject implements IDBNFragmentServer
{
	private static final long serialVersionUID = 50L;

	public DBNFragmentServer() throws RemoteException {super();}
	
	static int port;
	static String hostid;
	
	public static void main(String[] args)
	{
		if(args.length!=2)
		{
			System.out.println("No host arguments provided, defaulting to locaalhost:1099");
			port = 1099;
			hostid = "localhost";
		}
		else
		{
			hostid = args[0];
			port = Integer.parseInt(args[1]);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null) 
		{
			System.setSecurityManager(new RMISecurityManager());
			System.out.println("Security manager installed.");
		}
		else
			System.out.println("Security manager already exists.");

		try  //special exception handler for registry creation
		{
			LocateRegistry.createRegistry(port); 
			System.out.println("java RMI registry created.");
		}
		catch (RemoteException e)
		{
			//do nothing, error means registry already exists
			System.out.println("java RMI registry already exists.");
		}

		try
		{
			//Instantiate RmiServer
			DBNFragmentServer obj = new DBNFragmentServer();
			// Bind this object instance to the name "RmiServer"
			Naming.rebind("//"+hostid+":"+port+"/DBNFragmentServer", obj);
			System.out.println("PeerServer bound in registry at " + hostid +  ":" + port);
		} 
		catch (Exception e) 
		{
			System.err.println("RMI server exception:");
			e.printStackTrace();
		}
	}
	
	@Override
	public IRemoteDBNFragment newDBNFragment(String refID, int T, FragmentSpot spot)
			throws RemoteException {
		IRemoteDBNFragment frag = (IRemoteDBNFragment) UnicastRemoteObject.exportObject((IRemoteDBNFragment)new DBNFragment(T,spot),port);
		this.currentFragments.put(refID, frag);
		return frag;
	}

	@Override
	public void removeDBNFragment(String refID) throws BNException,
			RemoteException {
		this.currentFragments.remove(refID);
	}

	@Override
	public void clearFragments() {
		this.currentFragments.clear();
	}
	
	public HashMap<String, IRemoteDBNFragment> currentFragments = new HashMap<String, IRemoteDBNFragment>();
}
