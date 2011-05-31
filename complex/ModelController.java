package complex;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import bn.BNException;
import bn.dynamic.IDynamicBayesNet;

public abstract class ModelController
{
	
	public void validate() throws CMException
	{
		try {
			this.network.validate();
		} catch(BNException e) {throw new CMException(e.toString());}
	}
	
	public void printNetwork(boolean log)
	{
		if(log && this.logger!=null)
			this.network.print(this.logger);
		else if(this.tracer!=null)
			this.network.print(this.tracer);
	}
	
	public void printNetwork(String filename)
	{
		PrintStream outfile = null;
		try {
			outfile = new PrintStream(new File(filename));
			this.network.print(outfile);
			outfile.close();
		} catch(FileNotFoundException e) {
			System.err.println("Couldn't write to output file " + filename);
		}
	}

	public double run(int max_it, double conv)  throws CMException
	{
		try {
			this.network.run_parallel_block(max_it, conv);
			//this.network.run(max_it,conv);
			double ll = this.network.getLogLikelihood();
			if(Double.isNaN(ll))// || ll > 0) //TODO Note that we could have a density and LL could be > 0
			{
				System.err.println("Resetting messages, found NAN");
				this.network.resetMessages();
				this.network.run(max_it,conv);
				ll = this.network.getLogLikelihood();
				if(Double.isNaN(ll))// || ll > 0)
				{
					this.network.print(System.err);
					this.network.getLogLikelihood();
					throw new CMException("Model returns NaN/Greater than 0 log likelihood!");
				}
			}
			return ll;
		} catch(BNException e) {
			throw new CMException("Error running the model : " + e.toString());
		}
	}
	
	public double learn(int max_learn_it, double learn_conv, int max_run_it, double run_conv) throws CMException
	{
		try {
			this.network.optimize_parallel(max_learn_it, learn_conv, max_run_it, run_conv);
			//this.network.optimize(max_learn_it, learn_conv, max_run_it, run_conv);
			return this.run(max_run_it,run_conv);
		} catch(BNException e) {
			throw new CMException("Error optimizing the model : " + e.toString());
		}
	}

	public void log(String msg)
	{
		if(logger!=null)
			logger.println(msg);
	}
	
	public void setLogger(PrintStream log)
	{
		this.logger = log;
	}
	
	public void resetMessages()
	{
		this.network.resetMessages();
	}
	
	public void trace(String msg)
	{
		if(this.tracer!=null)
			this.tracer.println(msg);
	}
	
	public void setTrace(PrintStream tracer)
	{
		this.tracer = tracer;
	}
	
	public abstract double parameterPosteriorLL();
	
	protected PrintStream logger = null, tracer = null;
	protected IDynamicBayesNet network;
}
