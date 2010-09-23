package util;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

public class Parser {

	public static class ParserException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public ParserException(String message){super(message);}
	}

	public static interface LineHandler
	{
		public String getPrompt();
		public boolean parseLine(String line) throws ParserException; // Return true if you expect more lines...
	}

	public Parser(BufferedReader input, BufferedWriter output, boolean breakOnException, boolean printLineNoOnError)
	{
		this.input = input; this.output = output; this.breakOnException = breakOnException; this.printLineNoOnError = printLineNoOnError;
	}

	public void addHandler(String regexp, LineHandler handler)
	{
		this.handlers.put(regexp, handler);
	}

	public void setPrompt(String prompt)
	{
		this.prompt = prompt;
		this.promptBackup = prompt;
	}

	public void setCommentString(String commentStr)
	{
		this.commentStr = commentStr;
	}

	public void go()
	{
		int lineNumber = 1;
		for(;;)
		{
			try {
				if(this.prompt!=null)
				{
					try {output.write(this.prompt); output.flush();}
					catch(IOException e){
						System.err.println("Error printing to output terminal: " + e.toString());
						break;
					}
				}
				if(!readLine())
					break;
			}
			catch(Exception e) {
				if(output!=null)
				{
					try
					{
						if(printLineNoOnError)
							output.write("Error at line " + lineNumber + ": ");
						else
							output.write("Error: ");

						output.write(e.getMessage());
						output.write("\n");
						output.flush();
					} catch(IOException e2) {
						System.err.println("Error printing to output terminal: " + e.toString());
						break;
					}
				}
				if(breakOnException)
					break;
			}
			lineNumber++;
		}
	}

	private boolean readLine() throws ParserException, IOException
	{
		String line = input.readLine();

		if(line==null) return false;

		if(line.matches("\\s*"))
			return true;

		if(this.commentStr!=null)
			line = line.split(this.commentStr)[0];

		if(this.expectMore)
			this.expectMore = this.lastHandler.parseLine(line);
		else
		{
			boolean found = false;
			for(String regex : handlers.keySet())
			{
				if(line.matches(regex))
				{
					this.lastHandler = handlers.get(regex);
					this.expectMore = this.lastHandler.parseLine(line);
					found = true;
					break;
				}
			}
			if(!found)
				throw new ParserException("Unknown command.");
		}
		if(this.expectMore)
			this.prompt = this.lastHandler.getPrompt();
		else
			this.prompt = this.promptBackup;

		return true;
	}

	private boolean expectMore = false;
	private LineHandler lastHandler = null;

	private HashMap<String,LineHandler> handlers = new HashMap<String, Parser.LineHandler>();
	private BufferedReader input;
	private BufferedWriter output;
	private String prompt = null;
	private String promptBackup;
	private String commentStr = null;
	private boolean breakOnException, printLineNoOnError;
}