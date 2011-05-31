package complex.featural;

import java.util.Collection;

import java.util.HashSet;
import java.util.Vector;

import complex.CMException;
import complex.IParentProcess;

public abstract class ProposalAction<ChildProcess extends IFeaturalChild,ParentProcess extends IParentProcess>
{
	
	public ProposalAction(boolean suggest_samples)
	{
		this.suggest_samples = suggest_samples;
	}
	
	protected boolean suggest_samples;
	
	public double forward;
	public double backward;

	public abstract void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException;
	public abstract void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException;
	
	public Vector<ChildProcess> getChangedChildren()
	{
		return this.changedChildren;
	}
	public Vector<ParentProcess> getChangedParents()
	{
		return this.changedParents;
	}
	protected Vector<ChildProcess> changedChildren = new Vector<ChildProcess>();
	protected Vector<ParentProcess> changedParents = new Vector<ParentProcess>();
	
	public static class SwitchProposerAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public SwitchProposerAction(ParentProcess from, ParentProcess to)
		{
			super(false);
			this.from = from;this.to = to;
		}
	
		ParentProcess from, to;
		Vector<ChildProcess> switched = new Vector<ChildProcess>();
		Vector<ChildProcess> pulled = new Vector<ChildProcess>();
		

		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			Vector<ChildProcess> maybeSwitches = new Vector<ChildProcess>(cont.getChildren(from));
			for(ChildProcess child : maybeSwitches)
			{
				System.out.print("\tChecking " + child.getName() + " for node switch from " + from.getName() + " to " + to.getName() + ":" );
				child.backupParameters();
				
				if(!cont.getParents(child).contains(to))
				{
					double oldll = cont.run(1, 0);
					cont.disconnect(from, child);
					cont.connect(to, child);
					child.optimize();
					double newll = cont.run(20, 1e-5);//TODO This is all wrong, need another paradigm
					if(newll > oldll)
					{
						System.out.println("Yes: " + newll + " from " + oldll);
						this.switched.add(child);
					}
					else
					{
						System.out.println("No: " + newll + " from " + oldll);
						cont.disconnect(to, child);
						cont.connect(from, child);
						child.restoreParameters();
					}
				}
				else
				{
					cont.disconnect(from, child);
					child.optimize();
					double oldll = cont.run(1, 0);
					double newll = cont.run(20, 1e-5);//TODO This is all wrong, need another paradigm
					if(newll > oldll)
					{
						System.out.println("Yes!");
						this.pulled.add(child);
					}
					else
					{
						System.out.println("No.");
						cont.connect(from, child);
						child.restoreParameters();
					}
				}
			}
		}
	
		public void undo(FeaturalModelController<ChildProcess, ParentProcess> cont) throws CMException
		{
			for(ChildProcess switchedguy : this.switched)
			{
				cont.disconnect(to,switchedguy);
				cont.connect(from,switchedguy);
			}
			for(ChildProcess pulledguy : this.pulled)
			{
				cont.connect(from, pulledguy);
			}
		}
	}

	public static class SplitAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public SplitAction(ParentProcess latent, HashSet<ChildProcess> movers,boolean suggest_samples)
		{
			super(suggest_samples);
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
			if(this.suggest_samples)
			{
				this.changedParents.add(newFeature);
				this.changedParents.add(latentFeature);
			}
		}

		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			cont.killLatentModel(this.newFeature);
			for(ChildProcess child : this.movers)
			{
				cont.connect(latentFeature,child);
			}
		}

		private ParentProcess latentFeature;
		private ParentProcess newFeature;
		private HashSet<ChildProcess> movers;
	}
	
	public static class DeleteAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public DeleteAction(ParentProcess l)
		{
			super(false);
			this.l = l;
		}
		
		ParentProcess l;
		Vector<ChildProcess> children = new Vector<ChildProcess>();
		
		public void perform(FeaturalModelController<ChildProcess, ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : cont.getChildren(this.l))
				this.children.add(child);
			for(ChildProcess child : this.children)
				cont.disconnect(this.l, child);
		}
		
		public void undo(FeaturalModelController<ChildProcess, ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.children)
				cont.connect(this.l,child);
		}
	}

	public static class MergeAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{

		public MergeAction(ParentProcess l1, ParentProcess l2,boolean suggest_samples)
		{
			super(suggest_samples);
			this.latent1 = l1;
			this.latent2 = l2;
		}

		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			HashSet<ChildProcess> l1Children = cont.getChildren(this.latent1);
			this.newl1Children = new HashSet<ChildProcess>();
			
			
			this.latent2Children = new Vector<ChildProcess>();
			for(ChildProcess child : cont.getChildren(this.latent2))
			{
				this.latent2Children.add(child);
				if(!l1Children.contains(child))
				{
					this.newl1Children.add(child);
					cont.connect(latent1, child);
				}
			}
			for(ChildProcess child : this.latent2Children)
				cont.disconnect(latent2, child);
		
		}

		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.newl1Children)
			{
				cont.disconnect(this.latent1, child);
			}
			for(ChildProcess child : this.latent2Children)
				cont.connect(this.latent2, child);
		}
		
		private ParentProcess latent1, latent2;
		//private LatentBackup<ChildProcess,ParentProcess> latent2Backup;
		private Vector<ChildProcess> latent2Children;
		private HashSet<ChildProcess> newl1Children;
	}

	public static class SwitchAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public SwitchAction(ParentProcess from, ParentProcess to, Vector<ChildProcess> switches)
		{
			super(false);
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
			super(false);
			this.latent = latent;
			this.disconnects = discons;
		}

		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.disconnects)
			{
				cont.disconnect(this.latent, child);
			}
		}

		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.disconnects)
			{
				cont.connect(this.latent, child);
			}
		}

		private ParentProcess latent;
		private Collection<ChildProcess> disconnects;
	}
	
	public static class MultiUniqueParentAddAction<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends ProposalAction<ChildProcess,ParentProcess>
	{
		public MultiUniqueParentAddAction(Collection<ChildProcess> cps,boolean suggest_samples)
		{
			super(suggest_samples);
			this.cps = cps;
		}

		@Override
		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException {
			this.uniqParent = cont.newLatentModel();
			for(ChildProcess cp : this.cps)
			{
				cont.connect(uniqParent,cp);
			}
			if(this.suggest_samples)
				this.changedParents.add(uniqParent);
			cont.resetMessages(); //Because of the nature of this action  this helps
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
		public UniqueParentAddAction(ChildProcess cp,boolean suggest_smples)
		{
			super(suggest_smples);
			this.cp = cp;
		}

		@Override
		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException {
			this.uniqParent = cont.newLatentModel();
			cont.connect(uniqParent,cp);
			if(this.suggest_samples)
				this.changedParents.add(uniqParent);
			cont.resetMessages();
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
			super(false);
			this.latent = latent;
			this.connects = cons;
		}

		public void perform(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.connects)
			{
				cont.connect(this.latent, child);
			}
		}

		public void undo(FeaturalModelController<ChildProcess,ParentProcess> cont) throws CMException
		{
			for(ChildProcess child : this.connects)
			{
				cont.disconnect(this.latent, child);
			}
		}

		private ParentProcess latent;
		private Collection<ChildProcess> connects;
	}
}
