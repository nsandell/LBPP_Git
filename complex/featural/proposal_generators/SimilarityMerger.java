package complex.featural.proposal_generators;

import java.util.Vector;

import util.MathUtil;

import complex.featural.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;
import complex.metrics.FinDiscMarginalDivergence;

public class SimilarityMerger implements ProposalGenerator {
	
	public SimilarityMerger(double smp, double rmp, double csp, double rsp)
	{
		this.smp = smp;
		this.rmp = rmp;
		this.csp = csp;
		this.rsp = rsp;
	}
	double smp, rmp, csp, rsp;

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
		
		double fp = this.smp*rowsum[row]*divergences[row][col] + this.rmp/latents.size()/(latents.size()-1);
		double bp = this.rsp/(latents.size()+1)/latents.size(); //TODO decide whether we want exact backwards probabilities
		
		return new Proposal(fp, bp, new ProposalAction.MergeAction(latents.get(row),latents.get(col)));
	}

	public static double smoother = 0;
	
	@Override
	public String name() {
		return "Marginal-Similarity Merger";
	}

}
