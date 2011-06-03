package complex.featural.proposal_generators;

import java.util.Vector;

import util.MathUtil;
import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;
import complex.metrics.FinDiscMarginalDivergence;

public class SimilarityMerger implements ProposalGenerator {
	
	@Override
	public Proposal generate(FeaturalModelController cont)
	{
		Vector<IParentProcess> latents = cont.getLatentNodes();
		if(latents.size()<2)
			return null;
		double[][] divergences = new double[latents.size()][latents.size()];
		double[] rowsum = new double[latents.size()];
		double sum = 0;
		for(int i = 0; i < latents.size(); i++)
		{
			for(int j = 0; j < i; j++)
			{
				double val = 1.0/(1.0+FinDiscMarginalDivergence.meanSimDivergence(latents.get(i),latents.get(j),cont.getT())) + smoother;
				divergences[i][j] = val;
				rowsum[i] += val;
				sum += val;
			}
		}
		for(int i = 0; i < latents.size(); i++)
		{
			for(int j = 0; j < i; j++)
				divergences[i][j] /= rowsum[i];
			rowsum[i] /= sum;
		}
		
			
		int row = MathUtil.discreteSample(rowsum);
		int col = MathUtil.discreteSample(divergences[row]);
		
		System.out.println("Proposing the merge of latent processes " + latents.get(row).getName() + " and " + latents.get(col).getName() + " based on similarity");
		
		return new Proposal(new ProposalAction.MergeAction(latents.get(row),latents.get(col),true));
	}

	public static double smoother = 0;
	
	@Override
	public String name() {
		return "Marginal-Similarity Merger";
	}

}
