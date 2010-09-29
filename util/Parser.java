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

public class Parser {
	

	public static class ParserException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public ParserException(String message){super(message);}
	}

	public static interface ParserFunction
	{
		public int[] getGroups();
		public Pattern getRegEx();
		public String getPrompt();
		public void finish() throws ParserException;
		public ParserFunction parseLine(String[] args, PrintStream output) throws ParserException; // Return a line handler if expect more, else null
	}
	
	public static abstract class MethodWrapperHandler<EnvObj> implements ParserFunction
	{
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
		
		protected void handleReturn(){}

		public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
		{
			Object[] objargs = new Object[args.length];
			Class<?>[] parametersTypes = this.method.getParameterTypes();
			if(parametersTypes.length!=args.length)
				throw new ParserException("Method takes more arguments than specified...");
			int i = 0;
			try
			{
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
				if(this.method.getReturnType()!=void.class)
				{
					retObj = this.method.invoke(this.obj, objargs);
					this.handleReturn();
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

	public Parser(BufferedReader input, PrintStream output, PrintStream error_output, boolean breakOnException, boolean printLineNoOnError)
	{
		this.input = input; this.output = output; this.breakOnException = breakOnException; this.printLineNoOnError = printLineNoOnError; this.error_output = error_output;
	}

	public void addHandler(ParserFunction handler)
	{
		this.handlers.add(handler);
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
	
	public boolean inputWaiting()
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
	
	private boolean readLine(BufferedReader fileInput) throws ParserException, IOException
	{
		String line = fileInput.readLine();
		
		String fileIn = null;
		BufferedReader newFileInput = null;
		int internal_lineno = 1;

		if(line==null)
			return false;
		if(line.matches("\\s*"))
			return true;
		if(this.commentStr!=null)
			line = line.split(this.commentStr)[0];

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
		
		try
		{
			if(newFileInput!=null)
				while(this.readLine(newFileInput));
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
