// UniqueVariables.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;


/**
 * A class that handles the unique variables.
 */
public class UniqueVariables {
	//~ Instance fields --------------------------------------------------------

	/*
	 * link list of hashes holding variables
	 */
	private LinkedList<HashMap<String,LvalExpression>> vars;

	/*
	 * Holds refrences to parameters of this function.
	 */
	private Vector<LvalExpression> parameters;

	//~ Constructors -----------------------------------------------------------

	/**
	 * Constructor
	 */
	UniqueVariables() {
		parameters     = new Vector<LvalExpression>();
		vars           = new LinkedList<HashMap<String,LvalExpression>>();
		vars.addFirst(new HashMap<String,LvalExpression>());
	}

	//~ Methods ----------------------------------------------------------------

	/**
	 * Adds a new scope (= new hash table in linked list)
	 */
	public void pushScope() {
		vars.addFirst(new HashMap<String,LvalExpression>());
	}

	/**
	 * Removes the current scope (= hash table in head of linked list)
	 * @return HashMap the current poped scope
	 */
	public HashMap<String,LvalExpression> popScope() {
		return vars.removeFirst();
	}

	/**
	 * Returns all vars in current scope
	 * @return Set all vars in current scope
	 */
	public Set<String> enumScope() {
		return vars.getFirst().keySet();
	}

	/*
	 * Adds a parameter or local var to this function. Note that the function adds the LvalExpression for this parameter.
	 * @param name the name of the new parameter/var
	 * @param type the type of the new parameter/var
	 * @param isParameter true is the new variable is paramenter.
	 * @param isOutput true if it output variable
	 */
	public void addVar(String name, Type type, boolean isParameter,
	                   boolean isOutput) {
		// create the lavlue of the given parameter type and name.
		Lvalue lvalue = new VarLvalue(new Variable(name, type), isOutput);

		Vector<Lvalue> derivedLvalues = lvalue.getDerivedLvalues();

		for (int i = 0; i < derivedLvalues.size(); i++)
			add(derivedLvalues.elementAt(i), isParameter);
	}

	/*
	 * Adds a new parameter or variable. If the given parameter/var name already exists
	 * an IllegalArgumentException will be thrown
	 * @param lval the Lvalue created for the var/parma.
	 * @param isParameter true is the new variable is paramenter.
	 */
	public void add(Lvalue lval, boolean isParameter) {
		String         name    = lval.getName();
		LvalExpression lvalExp = new LvalExpression(lval);

		// add to local vars - current scope
		vars.getFirst().put(name, lvalExp);

		// add to parameters - if needed
		if (isParameter &&
			    ! (lval.hasDerives() || name.matches(".*\\[\\$\\].*"))) {
			parameters.add(lvalExp);
		}
	}

	/**
	 * Returns the LvalExpression reference of a var.
	 * It searches the scopes from the current scope till the global scope.
	 * @return LvalExpression the reference of the variable
	 */
	public LvalExpression getVar(String name) {
		ListIterator<HashMap<String,LvalExpression>> iterator = vars.listIterator(0);

		while (iterator.hasNext()) {
			HashMap<String,LvalExpression> scope = iterator.next();

			if (scope.containsKey(name)) {
				return scope.get(name);
			}
		}

		return null;
	}

	/*
	 * Replace all variables and parameters LvalExpressions
	 * with single bit LvalExpression.
	 */
	public void multi2SingleBit() {
		Lvalue oldLvalue;
		String oldName;

		// hold old params and locals
		Vector<LvalExpression> oldParameters = parameters;
		HashMap<String,LvalExpression> oldVars = popScope();

		// create new params vector and loacls scope
		parameters = new Vector<LvalExpression>();
		pushScope();

		Set<String> set = oldVars.keySet();
		Iterator<String> It = set.iterator();

		while (It.hasNext()) {
			oldName = It.next();

			// get the old LvalExpression's Lvalue
			oldLvalue = oldVars.get(oldName).getLvalue();

			if (oldLvalue.hasDerives()) {
				continue;
			}

			boolean isParameter = oldParameters.contains(oldVars.get(oldName));

			// insert new single bit variables instead 
			// of the old multibit vars
			for (int i = 0; i < oldLvalue.size(); i++)
				add(new BitLvalue(oldLvalue, i), isParameter);
		}

		oldVars.clear();
		oldParameters.clear();
	}

	/*
	 * Returns the vector of the parametes.
	 */
	public Vector<LvalExpression> getParameters() {
		return parameters;
	}
}
