package complex.featural;

import java.util.Collection;

import java.util.HashSet;
import java.util.Vector;

import complex.CMException;
import complex.IParentProcess;

public abstract class ProposalAction
{
	
	public ProposalAction(boolean suggest_samples)
	{
		this.suggest_samples = suggest_samples;
	}
	
	public abstract void perform(FeaturalModelController cont) throws CMException;
	public abstract void undo(FeaturalModelController cont) throws CMException;
	
	public Vector<IFeaturalChild> getChangedChildren()
	{
		return this.changedChildren;
	}
	public Vector<IParentProcess> getChangedParents()
	{
		return this.changedParents;
	}
	
	public static class SwitchProposerAction extends ProposalAction
	{
		public SwitchProposerAction(IParentProcess from, IParentProcess to)
		{
			super(false);
			this.from = from;this.to = to;
		}
	
		IParentProcess from, to;
		Vector<IFeaturalChild> switched = new Vector<IFeaturalChild>();
		Vector<IFeaturalChild> pulled = new Vector<IFeaturalChild>();
		

		public void perform(FeaturalModelController cont) throws CMException
		{
			Vector<IFeaturalChild> maybeSwitches = new Vector<IFeaturalChild>(cont.getChildren(from));
			for(IFeaturalChild child : maybeSwitches)
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
	
		public void undo(FeaturalModelController cont) throws CMException
		{
			for(IFeaturalChild switchedguy : this.switched)
			{
				cont.disconnect(to,switchedguy);
				cont.connect(from,switchedguy);
			}
			for(IFeaturalChild pulledguy : this.pulled)
			{
				cont.connect(from, pulledguy);
			}
		}
	}

	public static class SplitAction extends ProposalAction
	{
		public SplitAction(IParentProcess latent, HashSet<IFeaturalChild> movers,boolean suggest_samples)
		{
			super(suggest_samples);
			this.latentFeature = latent;
			this.movers = movers;
		}

		public void perform(FeaturalModelController cont) throws CMException
		{
			this.newFeature = cont.newLatentModel();
			for(IFeaturalChild child : this.movers)
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

		public void undo(FeaturalModelController cont) throws CMException
		{
			cont.killLatentModel(this.newFeature);
			for(IFeaturalChild child : this.movers)
			{
				cont.connect(latentFeature,child);
			}
		}

		private IParentProcess latentFeature;
		private IParentProcess newFeature;
		private HashSet<IFeaturalChild> movers;
	}
	
	public static class DeleteAction extends ProposalAction
	{
		public DeleteAction(IParentProcess l)
		{
			super(false);
			this.l = l;
		}
		
		IParentProcess l;
		Vector<IFeaturalChild> children = new Vector<IFeaturalChild>();
		
		public void perform(FeaturalModelController cont) throws CMException
		{
			for(IFeaturalChild child : cont.getChildren(this.l))
				this.children.add(child);
			for(IFeaturalChild child : this.children)
				cont.disconnect(this.l, child);
		}
		
		public void undo(FeaturalModelController cont) throws CMException
		{
			for(IFeaturalChild child : this.children)
				cont.connect(this.l,child);
		}
	}

	public static class MergeAction extends ProposalAction
	{

		public MergeAction(IParentProcess l1, IParentProcess l2,boolean suggest_samples)
		{
			super(suggest_samples);
			this.latent1 = l1;
			this.latent2 = l2;
		}

		public void perform(FeaturalModelController cont) throws CMException
		{
			HashSet<IFeaturalChild> l1Children = cont.getChildren(this.latent1);
			this.newl1Children = new HashSet<IFeaturalChild>();
			
			
			this.latent2Children = new Vector<IFeaturalChild>();
			for(IFeaturalChild child : cont.getChildren(this.latent2))
			{
				this.latent2Children.add(child);
				if(!l1Children.contains(child))
				{
					this.newl1Children.add(child);
					cont.connect(latent1, child);
				}
			}
			for(IFeaturalChild child : this.latent2Children)
				cont.disconnect(latent2, child);
		
		}

		public void undo(FeaturalModelController cont) throws CMException
		{
			for(IFeaturalChild child : this.newl1Children)
				cont.disconnect(this.latent1, child);
			for(IFeaturalChild child : this.latent2Children)
				cont.connect(this.latent2, child);
		}
		
		private IParentProcess latent1, latent2;
		private Vector<IFeaturalChild> latent2Children;
		private HashSet<IFeaturalChild> newl1Children;
	}

	public static class SwitchAction extends ProposalAction
	{
		public SwitchAction(IParentProcess from, IParentProcess to, Vector<IFeaturalChild> switches)
		{
			super(false);
			this.lfrom = from; this.lto = to; this.switches = switches;
		}

		public void perform(FeaturalModelController cont) throws CMException
		{
			// It is assumed anything switching wasn't already in the to node.
			for(IFeaturalChild child : this.switches)
			{
				cont.disconnect(lfrom, child);
				cont.connect(lto, child);
			}
		}

		public void undo(FeaturalModelController cont) throws CMException
		{
			for(IFeaturalChild child : this.switches)
			{
				cont.disconnect(lto, child);
				cont.connect(lfrom, child);
			}
		}

		private IParentProcess lfrom, lto;
		private Vector<IFeaturalChild> switches;
	}

	public static class DisconnectAction extends ProposalAction
	{
		public DisconnectAction(IParentProcess latent, Collection<IFeaturalChild> discons)
		{
			super(false);
			this.latent = latent;
			this.disconnects = discons;
		}

		public void perform(FeaturalModelController cont) throws CMException
		{
			for(IFeaturalChild child : this.disconnects)
				cont.disconnect(this.latent, child);
		}

		public void undo(FeaturalModelController cont) throws CMException
		{
			for(IFeaturalChild child : this.disconnects)
				cont.connect(this.latent, child);
		}

		private IParentProcess latent;
		private Collection<IFeaturalChild> disconnects;
	}
	
	public static class MultiUniqueParentAddAction extends ProposalAction
	{
		public MultiUniqueParentAddAction(Collection<IFeaturalChild> cps,boolean suggest_samples)
		{
			super(suggest_samples);
			this.cps = cps;
		}

		@Override
		public void perform(FeaturalModelController cont) throws CMException {
			this.uniqParent = cont.newLatentModel();
			for(IFeaturalChild cp : this.cps)
			{
				cont.connect(uniqParent,cp);
			}
			if(this.suggest_samples)
				this.changedParents.add(uniqParent);
			cont.resetMessages(); //Because of the nature of this action  this helps
		}

		@Override
		public void undo(FeaturalModelController cont) throws CMException {
			cont.killLatentModel(this.uniqParent);
		}
		
		IParentProcess uniqParent;
		Collection<IFeaturalChild> cps;
	}
	
	public static class UniqueParentAddAction extends ProposalAction
	{
		public UniqueParentAddAction(IFeaturalChild cp,boolean suggest_smples)
		{
			super(suggest_smples);
			this.cp = cp;
		}

		@Override
		public void perform(FeaturalModelController cont) throws CMException {
			this.uniqParent = cont.newLatentModel();
			cont.connect(uniqParent,cp);
			if(this.suggest_samples)
				this.changedParents.add(uniqParent);
			cont.resetMessages();
		}

		@Override
		public void undo(FeaturalModelController cont) throws CMException {
			cont.killLatentModel(this.uniqParent);
		}
		
		IParentProcess uniqParent;
		IFeaturalChild cp;
	}

	public static class ConnectAction extends ProposalAction
	{
		public ConnectAction(IParentProcess latent, Collection<IFeaturalChild> cons)
		{
			super(false);
			this.latent = latent;
			this.connects = cons;
		}

		public void perform(FeaturalModelController cont) throws CMException
		{
			for(IFeaturalChild child : this.connects)
				cont.connect(this.latent, child);
		}

		public void undo(FeaturalModelController cont) throws CMException
		{
			for(IFeaturalChild child : this.connects)
				cont.disconnect(this.latent, child);
		}

		private IParentProcess latent;
		private Collection<IFeaturalChild> connects;
	}
	
	public double forward;
	public double backward;
	protected boolean suggest_samples;
	protected Vector<IFeaturalChild> changedChildren = new Vector<IFeaturalChild>();
	protected Vector<IParentProcess> changedParents = new Vector<IParentProcess>();
}
