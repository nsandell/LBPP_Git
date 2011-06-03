package complex.featural.proposal_generators;

import java.util.Vector;

import util.MathUtil;

import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;

public class CoherenceUniqueParenter implements ProposalGenerator {
	
	@Override
	public Proposal generate(FeaturalModelController cont) {
		
		Vector<IFeaturalChild> children = cont.getObservedNodes();
		double[] coherence = new double[children.size()];
		double sum = 0;
		for(int i = 0; i < coherence.length; i++) {
			for(int t  = 0; t < cont.getT(); t++)
				coherence[i] = children.get(i).getDisagreement(t);
			coherence[i] += smoothness;
				
			//Coherence.coherence(children,cont.getT());
			sum += coherence[i];
		}
		for(int i = 0; i < coherence.length; i++)
			coherence[i] /= sum;
		
		int selection = MathUtil.discreteSample(coherence);
		System.out.println("Attempting to give " + children.get(selection).getName() + " a unique parent based on incoherence.");
	
		return new Proposal(new ProposalAction.UniqueParentAddAction(children.get(selection),true));
	}

	@Override
	public String name() {
		return "Disagreement-based unique parenter";
	}

	public double smoothness = 0;
}
