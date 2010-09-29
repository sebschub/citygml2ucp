package pik.clminputdata.tools;

import java.util.Properties;

/**
 * Properties with input routines for different variable types and a generalized
 * set routine.
 * 
 * @author Sebastian Schubert
 * 
 */
public class PropertiesEnh extends Properties {


	/**
	 * SUID generated by eclipse 
	 */
	private static final long serialVersionUID = -1595761753037640465L;

	/**
	 * Get a single boolean from properties.
	 * 
	 * @param key
	 *            String to look for
	 * @param defaultValue
	 *            If key not found, return this
	 * @return The boolean
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		}
		return Boolean.parseBoolean(str.trim());
	}
	
	/**
	 * Get a single integer from properties.
	 * 
	 * @param key
	 *            String to look for
	 * @param defaultValue
	 *            If key not found, return this
	 * @return The int
	 */
	public int getInt(String key, int defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		}
		return Integer.parseInt(str.trim());
	}

	/**
	 * Get a single double from properties.
	 * 
	 * @param key
	 *            String to look for
	 * @param defaultValue
	 *            If key not found, return this
	 * @return The double
	 */
	public double getDouble(String key, double defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		}
		return Double.parseDouble(str.trim());
	}

	/**
	 * Get an array of doubles from properties.
	 * 
	 * @param key
	 *            String to look for
	 * @param defaultValue
	 *            If key not found, return this
	 * @return The double array
	 */
	public double[] getDoubleArray(String key, double[] defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		}
		str = str.trim();
		String[] temp = str.split(",");
		
		double[] rarray = new double[temp.length];
		for (int i = 0; i < rarray.length; i++) {
			rarray[i] = Double.valueOf(temp[i]);
		}
		return rarray;
	}

	/**
	 * Get an array of integers from properties.
	 * 
	 * @param key
	 *            String to look for
	 * @param defaultValue
	 *            If key not found, return this
	 * @return The integer array
	 */
	public int[] getIntArray(String key, int[] defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		}
		str = str.trim();
		String[] temp = str.split(",");

		int[] rarray = new int[temp.length];
		for (int i = 0; i < rarray.length; i++) {
			rarray[i] = Integer.valueOf(temp[i]);
		}
		return rarray;
	}

	/**
	 * Get a string from properties.
	 * 
	 * @param key
	 *            String to look for
	 * @param defaultValue
	 *            If key not found, return this
	 * @return The string
	 */
	public String getString(String key, String defaultValue) {
		return getProperty(key, defaultValue).trim();
	}

	/**
	 * Set a property.
	 * 
	 * @param name
	 *            String of the property
	 * @param var
	 *            Object to put into property
	 */
	public void setProperty(String name, Object var) {
		setProperty(name, var.toString());
	}

}
