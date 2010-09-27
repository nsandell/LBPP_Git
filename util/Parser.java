package util;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.IOException;
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

	public static interface LineHandler
	{
		public int[] getGroups();
		public Pattern getRegEx();
		public String getPrompt();
		public void finish() throws ParserException;
		public LineHandler parseLine(String[] args) throws ParserException; // Return a line handler if expect more, else null
	}
	
	public static abstract class MethodWrapperHandler<EnvObj> implements LineHandler
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

		public LineHandler parseLine(String[] args) throws ParserException
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
				this.method.invoke(this.obj, objargs);
			} catch(IllegalArgumentException e) {
				throw new ParserException("Invalid type for the '"+ argumentNames[i] + "' argument.");
			} catch(InvocationTargetException e) {
				throw new ParserException(e.getCause().getMessage());
			} catch(IllegalAccessException e) {
				throw new ParserException("Failed to invoke method.");
			}
			return null;
		}
	
		String[] argumentNames;
		Object obj;
		Method method;
		HashMap<String,EnvObj> environmentObjects;
	}

	public Parser(BufferedReader input, BufferedWriter output, BufferedWriter error_output, boolean breakOnException, boolean printLineNoOnError)
	{
		this.input = input; this.output = output; this.breakOnException = breakOnException; this.printLineNoOnError = printLineNoOnError;this.error_output = error_output;
	}

	public void addHandler(LineHandler handler)
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
				{
					try {output.write(this.prompt); output.flush();}
					catch(IOException e){
						System.err.println("Error printing to output: " + e.toString());
						break;
					}
				}
				if(!readLine())
					break;
			}
			catch(Exception e) {
				if(error_output!=null)
				{
					try
					{
						if(printLineNoOnError)
							error_output.write("Error at line " + lineNumber + ": ");
						else
							error_output.write("Error: ");

						error_output.write(e.getMessage());
						error_output.write("\n");
						error_output.flush();
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
		try
		{
			if(output!=null)
				{this.output.write("\nExiting...\n\n");this.output.flush();}
		} catch(IOException e) {}
	}

	private boolean readLine() throws ParserException, IOException
	{
		String line = input.readLine();

		if(line==null)
			return false;
		if(line.matches("\\s*"))
			return true;
		if(this.commentStr!=null)
			line = line.split(this.commentStr)[0];
		
		LineHandler handler = null;
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
			for(LineHandler handlertmp : this.handlers)
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
		
		this.lastHandler = handler.parseLine(arguments);
		
		if(this.lastHandler!=null)
			this.prompt = this.lastHandler.getPrompt();
		else
			this.prompt = this.promptBackup;

		return true;
	}

	private LineHandler lastHandler = null;
	
	private ArrayList<LineHandler> handlers = new ArrayList<LineHandler>();
	
	private BufferedReader input;
	private BufferedWriter output;
	private BufferedWriter error_output;
	private String prompt = null;
	private String promptBackup;
	private String commentStr = null;
	private boolean breakOnException, printLineNoOnError;
}
