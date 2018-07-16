package octopus.utils;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/** 
@author yangdo  
This class has integrated with Groovy engine for running tests.
*/
public class OctopusScriptedTool extends OctopusToolBase {
//	private final String exprTemplate = loadStringFromRes(HemaScriptedTest.class, "ExprTemp.tmp");
	private final String exprTemplate = "\r\n"
			+ "import com.amazon.qa.octopus.tests.*\r\n"
			+ "\r\n"
			+ "class ExprTemp extends ExprTempRoot{\r\n"
			+ "\tpublic Object __evaluate(OctopusTest t){\r\n"
			+ "\t\t//steps\r\n"
			+ "\t\treturn \"expression\";\r\n"
			+ "\t}\r\n"
			+ "}";
	public ScriptErrorCode scriptError;
	public Binding defaultContext = new Binding();
	public GroovyShell defaultShell = new GroovyShell(defaultContext);

	/***
	 * This function is for 
	 * @param expr
	 * @param steps
	 * @return 
	 */
	protected String getEvalCode(String expr, String steps) {
		String packageName = getClass().getPackage().getName();
		String result = "package " + packageName + ";\r\n" +
				exprTemplate.replace("OctopusTest", getClass().getSimpleName());
		if (expr != null)
			result = result.replace("\"expression\"", expr);
		if (steps != null)
			result = result.replace("//steps", steps);
		return result;
	}

	public Object evalExprWithSteps(String expr, String steps) {
		scriptError = ScriptErrorCode.OK;
		@SuppressWarnings("resource")
		GroovyClassLoader loader = new GroovyClassLoader();
		Class<?> groovyClass = null;
		groovyClass = loader.parseClass(getEvalCode(expr, steps));
		try {
			Class<?> paramTypes[] = { this.getClass() };
			Method method = groovyClass.getMethod("__evaluate", paramTypes);
			Object[] params = { this };
			GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
			return method.invoke(groovyObject, params);
		} catch (NoSuchMethodException e) {
			scriptError = ScriptErrorCode.NoSuchMethod;
		} catch (SecurityException e) {
			scriptError = ScriptErrorCode.Security;
		} catch (IllegalAccessException e) {
			scriptError = ScriptErrorCode.IllegalAccess;
		} catch (IllegalArgumentException e) {
			scriptError = ScriptErrorCode.IllegalArgument;
		} catch (InvocationTargetException e) {
			scriptError = ScriptErrorCode.InvocationTarget;
		} catch (InstantiationException e) {
			scriptError = ScriptErrorCode.Instantiation;
		} finally {
//			try {
//				//loader.close();
//			} catch (IOException e) {
//				//e.printStackTrace();
//			}
		}
		return null;
	}

	/***
	 * This function is for executing normal expressions and scripts.
	 * Although there's no obvious limitation on the size of the scripts,
	 * we often tend to run scripts that's not very huge in size.
	 * @param snippet
	 * @return
	 */
	public Object runSnippet(String snippet) {
		return runSnippet(snippet, null);
	}

	/***
	 * Import static fields from a class
	 * @param clazz
	 */
	public void snippetImport(Class<?> clazz) {
		SnippetRunner.snippetImport(defaultContext, clazz);
	}

	/***
	 * This function is for executing normal expressions and scripts.
	 * Although there's no obvious limitation on the size of the scripts,
	 * we often tend to use this function to run scripts that's not very 
	 * huge in size.
	 * @param snippet
	 * @param params
	 * @return
	 */
	public Object runSnippet(String snippet, Map<String, ?> params) {
		return SnippetRunner.runSnippet(defaultShell, this, snippet, params);
	}

	public Object evalExpr(String expr) {
		return evalExprWithSteps(expr, null);
	}

	public void runScript(String script) {
		evalExprWithSteps(null, script);
	}
}
