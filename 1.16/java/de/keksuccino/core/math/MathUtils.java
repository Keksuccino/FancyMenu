package de.keksuccino.core.math;

import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class MathUtils {

	public static boolean isIntegerOrDouble(String value) {
    	try {
    		if (value.contains(".")) {
    			Double.parseDouble(value);
    		} else {
    			Integer.parseInt(value);
    		}
    		return true;
    	} catch (Exception e) {}
    	return false;
    }
	
	public static boolean isInteger(String value) {
		try {
			Integer.parseInt(value);
    		return true;
    	} catch (Exception e) {}
    	return false;
	}
	
	public static boolean isDouble(String value) {
		try {
			Double.parseDouble(value);
    		return true;
    	} catch (Exception e) {}
    	return false;
	}
	
	public static boolean isLong(String value) {
		try {
			Long.parseLong(value);
    		return true;
    	} catch (Exception e) {}
    	return false;
	}
	
	public static boolean isFloat(String value) {
		try {
			Float.parseFloat(value);
    		return true;
    	} catch (Exception e) {}
    	return false;
	}
	
	public static int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			return min;
		}
		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}
	
	/**
	 * Returns the calculated number OR ZERO if an error happened during the calculation.
	 */
	public static double calculateFromString(String in) {
	    try {
	    	ScriptEngineManager mgr = new ScriptEngineManager(null);
		    ScriptEngine engine = mgr.getEngineByName("javascript");
		    if (engine == null) {
		    	System.out.println("CALCULATION FAILED: ENGINE IS NULL");
		    	return 0;
		    }
	    	if (in == null) {
	    		System.out.println("CALCULATION FAILED: STRING IS NULL");
	    		return 0;
	    	}
	    	Object o = engine.eval(in);
	    	if (o == null) {
	    		System.out.println("CALCULATION FAILED: OBJECT IS NULL");
	    		return 0;
	    	}
	    	if (o instanceof Integer) {
	    		return (double) ((int)o);
	    	}
	    	if (o instanceof Double) {
	    		return (double) o;
	    	}
		} catch (Exception e) {
			e.printStackTrace();
		}
	    System.out.println("CALCULATION FAILED: RETURNING ZERO");
	    return 0;
	}
	
	/**
	 * Returns true if the given string can be calculated using {@link MathUtils#calculateFromString}.
	 */
	public static boolean isCalculateableString(String in) {
		ScriptEngineManager mgr = new ScriptEngineManager(null);
	    ScriptEngine engine = mgr.getEngineByName("javascript");
	    try {
	    	if (engine == null) {
	    		return false;
	    	}
	    	if (in == null) {
	    		return false;
	    	}
	    	Object o = engine.eval(in);
	    	if (o == null) {
	    		return false;
	    	}
	    	if (o instanceof Integer) {
	    		return true;
	    	}
	    	if (o instanceof Double) {
	    		return true;
	    	}
		} catch (ScriptException e) {}
	    return false;
	}

}
