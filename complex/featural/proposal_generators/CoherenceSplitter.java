package complex.featural.proposal_generators;

import java.util.HashSet;
import java.util.Vector;

import util.MathUtil;

import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;
import complex.metrics.Coherence;

public class CoherenceSplitter implements ProposalGenerator {
	
	@Override
	public Proposal generate(FeaturalModelController cont) {
		Vector<IParentProcess> latents = cont.getLatentNodes();
		if(latents.size()==0)
			return null;
		double[] coherence = new double[latents.size()];
		double sum = 0;
		for(int i = 0; i < latents.size(); i++)
		{
			Vector<IFeaturalChild> children = new Vector<IFeaturalChild>(cont.getChildren(latents.get(i)));
			coherence[i] = smoothness + Coherence.coherence(children,cont.getT());
			sum += coherence[i];
		}
		for(int i = 0; i < coherence.length; i++)
			coherence[i] /= sum;
		
	
		int selection = MathUtil.discreteSample(coherence);
		if(cont.getChildren(latents.get(selection)).size()<2)
			return null;
		System.out.println("Attempting to split node " + latents.get(selection).getName() + " based on incoherence.");
		                                 
		Vector<IFeaturalChild> children = new Vector<IFeaturalChild>(cont.getChildren(latents.get(selection)));
		boolean[] membership = Coherence.partition(children, cont.getT());
		
		HashSet<IFeaturalChild> movers = new HashSet<IFeaturalChild>();
		for(int i = 0; i < membership.length; i++)
			if(membership[i])
				movers.add(children.get(i));
		
		return new Proposal(new ProposalAction.SplitAction(latents.get(selection),movers,true));
	}

	@Override
	public String name() {
		return "Coherence-based splitter";
	}

	public double smoothness = 0;
}
