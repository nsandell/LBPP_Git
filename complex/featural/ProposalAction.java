package complex.featural;

import java.util.Collection;

import java.util.HashSet;
import java.util.Vector;

import complex.CMException;
import complex.IParentProcess;
import complex.featural.FeaturalModelController.LatentBackup;

public abstract class ProposalAction<ChildProcess extends IFeaturalChild,ParentProcess extends IParentProcess>
{
	public double forward;
	public double backward;

	public abstract void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException;
	public abstract void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException;

	public static class SplitAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public SplitAction(ParentProcess latent, HashSet<ChildProcess> movers)
		{
			this.latentFeature = latent;
			this.movers = movers;
		}

		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			this.newFeature = cont.newLatentModel();
			for(ChildProcess child : this.movers)
			{
				cont.disconnect(latentFeature, child);
				cont.connect(this.newFeature,child);
			}
		}

		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.movers)
				cont.connect(latentFeature,child);
			cont.killLatentModel(this.newFeature);
		}

		private ParentProcess latentFeature;
		private ParentProcess newFeature;
		private HashSet<ChildProcess> movers;
	}

	public static class MergeAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{

		public MergeAction(ParentProcess l1, ParentProcess l2)
		{
			this.latent1 = l1;
			this.latent2 = l2;
		}

		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			this.latent2Backup = cont.backupAndRemoveLatentModel(latent2);
			this.newl1Children = new HashSet<ChildProcess>();
			HashSet<ChildProcess> l1Children = cont.getChildren(this.latent1);
			for(ChildProcess child : this.latent2Backup.children)
			{
				if(!l1Children.contains(child))
				{
					this.newl1Children.add(child);
					cont.connect(latent1, child);
				}
			}
		}

		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			this.latent2 = cont.restoreBackup(this.latent2Backup);
			for(ChildProcess child : this.newl1Children)
				cont.disconnect(this.latent1,child);
		}
		private ParentProcess latent1, latent2;
		private LatentBackup<ChildProcess,ParentProcess> latent2Backup;
		private HashSet<ChildProcess> newl1Children;
	}

	public static class SwitchAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public SwitchAction(ParentProcess from, ParentProcess to, Vector<ChildProcess> switches)
		{
			this.lfrom = from; this.lto = to; this.switches = switches;
		}

		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			// It is assumed anything switching wasn't already in the to node.
			for(ChildProcess child : this.switches)
			{
				cont.disconnect(lfrom, child);
				cont.connect(lto, child);
			}
		}

		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.switches)
			{
				cont.disconnect(lto, child);
				cont.connect(lfrom, child);
			}
		}

		private ParentProcess lfrom, lto;
		private Vector<ChildProcess> switches;
	}

	public static class DisconnectAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public DisconnectAction(ParentProcess latent, Collection<ChildProcess> discons)
		{
			this.latent = latent;
			this.disconnects = discons;
		}

		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.disconnects)
				cont.disconnect(this.latent, child);
		}

		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.disconnects)
				cont.connect(this.latent, child);
		}

		private ParentProcess latent;
		private Collection<ChildProcess> disconnects;
	}
	
	public static class MultiUniqueParentAddAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public MultiUniqueParentAddAction(Collection<ChildProcess> cps)
		{
			this.cps = cps;
		}

		@Override
		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException {
			this.uniqParent = cont.newLatentModel();
			for(ChildProcess cp : this.cps)
				cont.connect(uniqParent,cp);
		}

		@Override
		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException {
			cont.killLatentModel(this.uniqParent);
		}
		
		ParentProcess uniqParent;
		Collection<ChildProcess> cps;
	}
	
	public static class UniqueParentAddAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess> 
	{
		public UniqueParentAddAction(ChildProcess cp)
		{
			this.cp = cp;
		}

		@Override
		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException {
			this.uniqParent = cont.newLatentModel();
			cont.connect(uniqParent,cp);
		}

		@Override
		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException {
			cont.killLatentModel(this.uniqParent);
		}
		
		ParentProcess uniqParent;
		ChildProcess cp;
	}

	public static class ConnectAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public ConnectAction(ParentProcess latent, Collection<ChildProcess> cons)
		{
			this.latent = latent;
			this.connects = cons;
		}

		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.connects)
				cont.connect(this.latent, child);
		}

		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.connects)
				cont.disconnect(this.latent, child);
		}

		private ParentProcess latent;
		private Collection<ChildProcess> connects;
	}
}
