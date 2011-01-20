package complex.featural;

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import bn.IDynBayesNode;

import complex.featural.ModelController.LatentBackup;

public abstract class ProposalAction
{
	public double forward;
	public double backward;

	public abstract void perform(ModelController cont) throws FMMException;
	public abstract void undo(ModelController cont) throws FMMException;

	public static class SplitAction extends ProposalAction
	{
		public SplitAction(IDynBayesNode latent, HashSet<IDynBayesNode> movers)
		{
			this.latentFeature = latent;
			this.movers = movers;
		}

		public void perform(ModelController cont) throws FMMException
		{
			this.newFeature = cont.newLatentModel();
			for(IDynBayesNode child : this.movers)
				cont.connect(this.newFeature,child);
		}

		public void undo(ModelController cont) throws FMMException
		{
			for(IDynBayesNode child : this.movers)
				cont.connect(latentFeature,child);
			cont.killLatentModel(this.newFeature);
		}

		private IDynBayesNode latentFeature;
		private IDynBayesNode newFeature;
		private HashSet<IDynBayesNode> movers;
	}

	public static class MergeAction extends ProposalAction
	{

		public MergeAction(IDynBayesNode l1, IDynBayesNode l2)
		{
			this.latent1 = l1;
			this.latent2 = l2;
		}

		public void perform(ModelController cont) throws FMMException
		{
			this.latent2Backup = cont.backupAndRemoveLatentModel(latent2);
			this.newl1Children = new HashSet<IDynBayesNode>();
			HashSet<IDynBayesNode> l1Children = cont.getChildren(this.latent1);
			for(IDynBayesNode child : this.latent2Backup.children)
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
			for(IDynBayesNode child : this.newl1Children)
				cont.disconnect(this.latent1,child);
		}
		private IDynBayesNode latent1, latent2;
		private LatentBackup latent2Backup;
		private HashSet<IDynBayesNode> newl1Children;
	}

	public static class SwitchAction extends ProposalAction
	{
		public SwitchAction(IDynBayesNode from, IDynBayesNode to, Vector<IDynBayesNode> switches)
		{
			this.lfrom = from; this.lto = to; this.switches = switches;
		}

		public void perform(ModelController cont) throws FMMException
		{
			// It is assumed anything switching wasn't already in the to node.
			for(IDynBayesNode child : this.switches)
			{
				cont.disconnect(lfrom, child);
				cont.connect(lto, child);
			}
		}

		public void undo(ModelController cont) throws FMMException
		{
			for(IDynBayesNode child : this.switches)
			{
				cont.disconnect(lto, child);
				cont.connect(lfrom, child);
			}
		}

		private IDynBayesNode lfrom, lto;
		private Vector<IDynBayesNode> switches;
	}

	public static class DisconnectAction extends ProposalAction
	{
		public DisconnectAction(IDynBayesNode latent, Collection<IDynBayesNode> discons)
		{
			this.latent = latent;
			this.disconnects = discons;
		}

		public void perform(ModelController cont) throws FMMException
		{
			for(IDynBayesNode child : this.disconnects)
				cont.disconnect(this.latent, child);
		}

		public void undo(ModelController cont) throws FMMException
		{
			for(IDynBayesNode child : this.disconnects)
				cont.connect(this.latent, child);
		}

		private IDynBayesNode latent;
		private Collection<IDynBayesNode> disconnects;
	}

	public static class ConnectAction extends ProposalAction
	{
		public ConnectAction(IDynBayesNode latent, Collection<IDynBayesNode> cons)
		{
			this.latent = latent;
			this.connects = cons;
		}

		public void perform(ModelController cont) throws FMMException
		{
			for(IDynBayesNode child : this.connects)
				cont.connect(this.latent, child);
		}

		public void undo(ModelController cont) throws FMMException
		{
			for(IDynBayesNode child : this.connects)
				cont.disconnect(this.latent, child);
		}

		private IDynBayesNode latent;
		private Collection<IDynBayesNode> connects;
	}
}
