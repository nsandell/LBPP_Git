package util;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Class that wraps a bunch of java functionality in an interactive command
 * line environment.
 * @author Nils F. Sandell
 */
public class Parser {

	/**
	 * Exception for use within the parser and parser functions.  Should be thrown
	 * with command-line appropriate error messages.
	 * @author Nils F. Sandell
	 */
	public static class ParserException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public ParserException(String message){super(message);}
	}

	/**
	 * Interface for objects that wrap specific java functionality.
	 * @author Nils F. Sandell
	 */
	public static interface ParserFunction
	{
		/**
		 * Get the regex that should be mapped to this function.  Should be 
		 * set up so that any desired arguments are 'groups'.
		 * @return Regex pattern object.
		 */
		public Pattern getRegEx();
		
		/**
		 * Get the regex 'groups' that are desired to be passed as
		 * arguments to this function.
		 * @return Array of group indexes.
		 */
		public int[] getGroups();
		
		/**
		 * Get the prompt if this function demands additional input after
		 * initial call.
		 * @return String to be output as the prompt.
		 */
		public String getPrompt();
		
		/**
		 * Called when user enters the termination syntax (*s) if this function
		 * takes variable lines of input.
		 * @throws ParserException If termination to this point in input creates an error.
		 */
		public void finish() throws ParserException;
		
		/**
		 * A line of input has been entered by the user matching the regex specified for this function.
		 * @param args Strings corresponding to the regex groups this object desired.
		 * @param output A printstream for any notification text to be printed to.
		 * @return If this function needs more lines of input, it returns a parserfunction to handle 
		 * 		the additional lines (might be "this", or another ParserFunction).  If it needs no 
		 * 		more lines it should return null.
		 * @throws ParserException If there is an error processing the function arguments or running the
		 * 		function.  Exception text should be command-line error appropriate.
		 */
		public ParserFunction parseLine(String[] args, PrintStream output) throws ParserException; // Return a line handler if expect more, else null
	}
	
	/**
	 * ParserFunction class that uses reflection to wrap a method.  Cuts down on implementation of
	 * ParserFunctions that directly send command line arguments that are integers, doubles, etc
	 * to a function.
	 * @author Nils F. Sandell
	 *
	 * @param If this function takes an argument that names a non-primitive java object, this specifies
	 * 		the type of object that is taken.  Hence, for now these functions can only take one non-primitive
	 * 		object argument.
	 */
	public static abstract class MethodWrapperHandler<EnvObj> implements ParserFunction
	{
		/**
		 * Constructor.
		 * @param obj The object on which the method should be called.
		 * @param method The method that belongs to the object that we want to call.
		 * @param argumentNames The names of the arguments to this function (for error purposes.)
		 * @param environmentObjects Map that maps names to object instances for the non-primitive object arguments.
		 * @throws Exception If the arguments are not coherent.
		 */
		public MethodWrapperHandler(Object obj, Method method, String[] argumentNames, HashMap<String, EnvObj> environmentObjects) throws Exception
		{
			if(this.getGroups().length!=argumentNames.length || this.getGroups().length!=method.getParameterTypes().length)
				throw new Exception("Failed MethodWrapper instantiation for "+ method.getName() +" ..");
			this.obj = obj;
			this.method = method;
			this.environmentObjects = environmentObjects;
			this.argumentNames = argumentNames;
		}
		
		public void finish(){}
		
		/**
		 * This method should be implemented by subclass if we wish to do
		 * something with the result of the function call.
		 * @param str Stream for outputting any user-level text.
		 */
		protected void handleReturn(PrintStream str){}

		// Implements the wrapping of the method
		public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
		{
			Object[] objargs = new Object[args.length];
			Class<?>[] parametersTypes = this.method.getParameterTypes();
			if(parametersTypes.length!=args.length) // Make sure we get appropriate number of arguments.
				throw new ParserException("Method takes more arguments than specified...");
			int i = 0;
			try
			{
				// Iterate through the arguments converting them to the appropriate types for the method
				//  throwing an error if the argument is improper (String when expecting Int, etc)
				while(i < args.length)
				{
					if(String.class.equals(parametersTypes[i]))
						objargs[i] = args[i]; 
					else if(Integer.class.equals(parametersTypes[i]) || int.class.equals(parametersTypes[i]))
						objargs[i] = Integer.parseInt(args[i]); 
					else if(Double.class.equals(parametersTypes[i]) || double.class.equals(parametersTypes[i]))
						objargs[i] = Double.parseDouble(args[i]);
					else if(environmentObjects!=null)
					{
						EnvObj arg = this.environmentObjects.get(args[i]);
						if(arg!=null) 
							objargs[i] = arg;
						else if(arg==null)
							throw new ParserException("Object '" + args[i] + "' not recognized.");
					}
					i++;
				}
				// If this method returns something aside from void, store it and invoke a method for
				// handling that return type.
				if(this.method.getReturnType()!=void.class)
				{
					retObj = this.method.invoke(this.obj, objargs);
					this.handleReturn(str);
				}
				else this.method.invoke(this.obj, objargs);
			} catch(IllegalArgumentException e) {
				throw new ParserException("Invalid type for the '"+ argumentNames[i] + "' argument.");
			} catch(InvocationTargetException e) {
				throw new ParserException(e.getCause().getMessage());
			} catch(IllegalAccessException e) {
				throw new ParserException("Failed to invoke method.");
			}
			return null;
		}
	
		protected Object retObj;
		private String[] argumentNames;
		private Object obj;
		private Method method;
		private HashMap<String,EnvObj> environmentObjects;
	}

	/**
	 * Create a command line parser object.
	 * @param input The stream from which to grab input.
	 * @param output The stream to push output.  If this is null, will not print output.
	 * @param error_output The stream to push error output.  If this is null, will not print error output.
	 * @param breakOnException If true, breaks on receiving exceptions from functions.  Inappropriate if we are using
	 * 		as an interactive prompt, for example, as the user should be able to just correct what was wrong.  May be
	 * 		appropriate if loading a file.
	 * @param printLineNoOnError If true, will print a line number on an error.  More useful for file reading.
	 */
	public Parser(BufferedReader input, PrintStream output, PrintStream error_output, boolean breakOnException, boolean printLineNoOnError)
	{
		this.input = input; this.output = output; this.breakOnException = breakOnException; this.printLineNoOnError = printLineNoOnError; this.error_output = error_output;
	}

	/**
	 * Add a function for this command line to handle.
	 * @param handler Function we want to handle.
	 */
	public void addHandler(ParserFunction handler)
	{
		this.handlers.add(handler);
	}

	/**
	 * Set the prompt to be used to prompt user for input.
	 * @param prompt String to be printed as prompt.
	 */
	public void setPrompt(String prompt)
	{
		this.prompt = prompt;
		this.promptBackup = prompt;
	}

	/**
	 * Set a 'comment' regex, where text after an occurrence will be stripped
	 * and ignored before command processing.
	 * @param commentStr Regex denoting single line comment start.
	 */
	public void setCommentString(String commentStr)
	{
		this.commentStr = commentStr;
	}
	
	/**
	 * Checks to see if there is more input waiting, so we know whether or
	 * not to print a prompt.
	 * @return True if more input is waiting.
	 */
	private boolean inputWaiting()
	{
		try {
			this.input.mark(2048);
			int chara;
			while((chara=this.input.read())!=-1)
			{
				if(((char)chara)=='\n')
				{
					input.reset();
					return true;
				}
			}
			input.reset();
			return false;
		} catch(IOException e) {
			System.err.println("Error trying to read input file.");
			return false;
		}
	}

	/**
	 * Start the command line handler.
	 */
	public void go()
	{
		int lineNumber = 1;
		for(;;)
		{
			try {
				if(!(this.input.ready() && this.input.markSupported() && this.inputWaiting()) && this.prompt!=null)
					output.print(this.prompt); 
				if(!readLine(this.input))
					break;
			}
			catch(Exception e) {
				if(error_output!=null)
				{
					if(printLineNoOnError)
						error_output.print("Error at line " + lineNumber + ": ");
					else
						error_output.print("Error: ");

					error_output.println(e.getMessage());
					error_output.flush();
				}
				if(breakOnException)
					break;
			}
			lineNumber++;
		}
		if(output!=null){this.output.println("\nExiting...\n");}
	}
	
	/**
	 * Internal function for handling input
	 * @param fileInput If we are inside a file pipe, handle to the input file.
	 * @return False if the file pipe or standard input has ended.
	 * @throws ParserException If there is an error parsing a command
	 * @throws IOException If there is an error with an input/output pipe.
	 */
	private boolean readLine(BufferedReader fileInput) throws ParserException, IOException
	{
		String line = fileInput.readLine();
		
		String fileIn = null;
		BufferedReader newFileInput = null;
		int internal_lineno = 1;

		if(line==null)
			return false;
	

		if(line.contains(">>") || line.contains("<<"))
		{
			String command = null, fileOut = null;
			if(line.contains(">>") && line.contains("<<"))
			{
				String[] bits = line.split("<<");
				if(bits.length!=2)
					throw new ParserException("Redirect format error.");
				if(bits[0].contains(">>"))
				{
					String[] bits2 = bits[0].split(">>");
					if(bits2.length!=2 || bits[1].contains(">>"))
						throw new ParserException("Redirect format error.");
					command = bits2[0];
					fileOut = bits2[1];
					fileIn = bits[2];
				}
				else
				{
					String[] bits2 = bits[1].split(">>");
					command = bits[0];
					fileOut = bits2[0];
					fileIn = bits[1];
				}	
			}
			else if(line.contains(">>"))
			{
				String[] bits = line.split(">>");
				if(bits.length!=2)
					throw new ParserException("Redirect format error.");
				command = bits[0];
				fileOut = bits[1];
			}
			else if(line.contains("<<"))
			{
				String[] bits = line.split("<<");
				if(bits.length>2)
					throw new ParserException("Error, multiple file inputs on one line.");
				fileIn = bits[1];
				command = bits[0];
			}
			line = command;
			if(fileOut!=null)
			{
				fileOut = fileOut.trim();
				this.tmp_output = new PrintStream(fileOut);
			}
			if(fileIn!=null)
			{
				try
				{
					fileIn = fileIn.trim();
					newFileInput = new BufferedReader(new FileReader(fileIn));
					line += newFileInput.readLine();
				} catch(IOException e) {
					throw new ParserException("Failure getting input from file " + fileIn);
				} catch(Exception e) {
					throw new ParserException("File ("+fileIn+") Line " + internal_lineno);
				}
			}
		}

		if(this.tmp_output!=null && this.tmp_output.checkError())
		{
			this.tmp_output = null;
			throw new ParserException("Error writing to output stream " + this.tmp_output.toString());
		}
		
		ParserFunction handler = null;
		Matcher matcher = null;
		if(this.lastHandler!=null && line.matches("\\s*\\**\\s*"))
		{
			this.lastHandler.finish();
			this.lastHandler = null;
			this.prompt = this.promptBackup;
			return true;
		}
		
		if(this.commentStr!=null)
			line = line.split(this.commentStr)[0];

		if(!line.matches("\\s*"))
		{
			if(this.lastHandler!=null)
			{
				handler = this.lastHandler;
				matcher = handler.getRegEx().matcher(line);
				if(!matcher.find())
					throw new ParserException("Syntax error.");
			}
			else
			{
				for(ParserFunction handlertmp : this.handlers)
				{
					matcher = handlertmp.getRegEx().matcher(line);
					if(matcher.find())
					{
						handler = handlertmp;
						break;
					}
				}
				if(handler==null)
					throw new ParserException("Unrecognized command.");
			}

			int[] groups = handler.getGroups();
			String[] arguments = new String[groups.length];
			for(int i = 0; i < groups.length; i++)
				arguments[i] = matcher.group(groups[i]);

			if(this.tmp_output==null)
				this.lastHandler = handler.parseLine(arguments,this.output);
			else
				this.lastHandler = handler.parseLine(arguments,this.tmp_output);

			if(this.lastHandler!=null)
				this.prompt = this.lastHandler.getPrompt();
			else
				this.prompt = this.promptBackup;
		}

		try
		{
			if(newFileInput!=null)
			{
				while(this.readLine(newFileInput))
				{
					internal_lineno++;
				}
			}
			if(this.tmp_output!=null)
				this.tmp_output.close();
			this.tmp_output = null;
		}
		catch(Exception e) {
			this.lastHandler = null;
			this.prompt = this.promptBackup;
			throw new ParserException("File (" + fileIn + ") Line Number " + internal_lineno + ": " + e.getMessage());
		}

		return true;
	}

	private ParserFunction lastHandler = null;
	
	private ArrayList<ParserFunction> handlers = new ArrayList<ParserFunction>();

	private BufferedReader input;
	private PrintStream output;
	private PrintStream error_output;
	private PrintStream tmp_output;
	private String prompt = null;
	private String promptBackup;
	private String commentStr = null;
	private boolean breakOnException, printLineNoOnError;
}
