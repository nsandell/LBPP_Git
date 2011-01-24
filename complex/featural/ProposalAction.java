package complex.featural;

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import complex.featural.ModelController.LatentBackup;

public abstract class ProposalAction
{
	public double forward;
	public double backward;

	public abstract void perform(ModelController cont) throws FMMException;
	public abstract void undo(ModelController cont) throws FMMException;

	public static class SplitAction extends ProposalAction
	{
		public SplitAction(IParentProcess latent, HashSet<IChildProcess> movers)
		{
			this.latentFeature = latent;
			this.movers = movers;
		}

		public void perform(ModelController cont) throws FMMException
		{
			this.newFeature = cont.newLatentModel();
			for(IChildProcess child : this.movers)
				cont.connect(this.newFeature,child);
		}

		public void undo(ModelController cont) throws FMMException
		{
			for(IChildProcess child : this.movers)
				cont.connect(latentFeature,child);
			cont.killLatentModel(this.newFeature);
		}

		private IParentProcess latentFeature;
		private IParentProcess newFeature;
		private HashSet<IChildProcess> movers;
	}

	public static class MergeAction extends ProposalAction
	{

		public MergeAction(IParentProcess l1, IParentProcess l2)
		{
			this.latent1 = l1;
			this.latent2 = l2;
		}

		public void perform(ModelController cont) throws FMMException
		{
			this.latent2Backup = cont.backupAndRemoveLatentModel(latent2);
			this.newl1Children = new HashSet<IChildProcess>();
			HashSet<IChildProcess> l1Children = cont.getChildren(this.latent1);
			for(IChildProcess child : this.latent2Backup.children)
			{
				if(!l1Children.contains(child))
				{
					this.newl1Children.add(child);
					cont.connect(latent1, child);
				}
			}
		}

		public void undo(ModelController cont) throws FMMException
		{
			this.latent2 = cont.restoreBackup(this.latent2Backup);
			for(IChildProcess child : this.newl1Children)
				cont.disconnect(this.latent1,child);
		}
		private IParentProcess latent1, latent2;
		private LatentBackup latent2Backup;
		private HashSet<IChildProcess> newl1Children;
	}

	public static class SwitchAction extends ProposalAction
	{
		public SwitchAction(IParentProcess from, IParentProcess to, Vector<IChildProcess> switches)
		{
			this.lfrom = from; this.lto = to; this.switches = switches;
		}

		public void perform(ModelController cont) throws FMMException
		{
			// It is assumed anything switching wasn't already in the to node.
			for(IChildProcess child : this.switches)
			{
				cont.disconnect(lfrom, child);
				cont.connect(lto, child);
			}
		}

		public void undo(ModelController cont) throws FMMException
		{
			for(IChildProcess child : this.switches)
			{
				cont.disconnect(lto, child);
				cont.connect(lfrom, child);
			}
		}

		private IParentProcess lfrom, lto;
		private Vector<IChildProcess> switches;
	}

	public static class DisconnectAction extends ProposalAction
	{
		public DisconnectAction(IParentProcess latent, Collection<IChildProcess> discons)
		{
			this.latent = latent;
			this.disconnects = discons;
		}

		public void perform(ModelController cont) throws FMMException
		{
			for(IChildProcess child : this.disconnects)
				cont.disconnect(this.latent, child);
		}

		public void undo(ModelController cont) throws FMMException
		{
			for(IChildProcess child : this.disconnects)
				cont.connect(this.latent, child);
		}

		private IParentProcess latent;
		private Collection<IChildProcess> disconnects;
	}
	
	public static class UniqueParentAddAction extends ProposalAction
	{
		public UniqueParentAddAction(IChildProcess cp)
		{
			this.cp = cp;
		}

		@Override
		public void perform(ModelController cont) throws FMMException {
			this.uniqParent = cont.newLatentModel();
			cont.connect(uniqParent,cp);
		}

		@Override
		public void undo(ModelController cont) throws FMMException {
			cont.killLatentModel(this.uniqParent);
		}
		
		IParentProcess uniqParent;
		IChildProcess cp;
	}

	public static class ConnectAction extends ProposalAction
	{
		public ConnectAction(IParentProcess latent, Collection<IChildProcess> cons)
		{
			this.latent = latent;
			this.connects = cons;
		}

		public void perform(ModelController cont) throws FMMException
		{
			for(IChildProcess child : this.connects)
				cont.connect(this.latent, child);
		}

		public void undo(ModelController cont) throws FMMException
		{
			for(IChildProcess child : this.connects)
				cont.disconnect(this.latent, child);
		}

		private IParentProcess latent;
		private Collection<IChildProcess> connects;
	}
}
