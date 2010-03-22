package pik.clminputdata.tools;

import java.util.ArrayList;
import java.util.Properties;

/**
 * Properties with input routines for different variable types and a generalized
 * set routine
 * 
 * @author Sebastian Schubert
 * 
 */
public class PropertiesEnh extends Properties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * get a single integer
	 * 
	 * @param key
	 *            the string to look for
	 * @param defaultValue
	 *            if key not found, return this
	 * @return the int
	 */
	public int getInt(String key, int defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		}
		return Integer.parseInt(str.trim());
	}

	/**
	 * get a single double
	 * 
	 * @param key
	 *            the string to look for
	 * @param defaultValue
	 *            if key not found, return this
	 * @return the double
	 */
	public double getDouble(String key, double defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		}
		return Double.parseDouble(str.trim());
	}

	/**
	 * get an array of doubles
	 * 
	 * @param key
	 *            the string to look for
	 * @param defaultValue
	 *            if key not found, return this
	 * @return the double array
	 */
	public double[] getDoubleArray(String key, double[] defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		}
		str = str.trim();

		ArrayList<Double> temp = new ArrayList<Double>();

		int cpos = str.indexOf(',');
		if (cpos == -1) {
			temp.add(Double.parseDouble(str.trim()));
		} else {
			// get first element
			temp.add(Double.parseDouble(str.substring(0, cpos).trim()));
			int cpos2;
			do {
				cpos2 = str.indexOf(',', cpos + 1);
				if (cpos2 != -1) {
					temp.add(Double.parseDouble(str.substring(cpos + 1,
							cpos2).trim()));
					cpos = cpos2;
				} else {
					temp.add(Double.parseDouble(str.substring(cpos + 1,
							str.length() - 1).trim()));
					cpos = -2;
				}
			} while (cpos != -2);
		}

		double[] rarray = new double[temp.size()];
		for (int i = 0; i < rarray.length; i++) {
			rarray[i] = temp.get(i);
		}
		return rarray;
	}

	/**
	 * get an array of integers
	 * 
	 * @param key
	 *            the string to look for
	 * @param defaultValue
	 *            if key not found, return this
	 * @return the integer array
	 */
	public int[] getIntArray(String key, int[] defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		}
		str = str.trim();

		ArrayList<Integer> temp = new ArrayList<Integer>();

		int cpos = str.indexOf(',');
		if (cpos == -1) {
			temp.add(Integer.parseInt(str.trim()));
		} else {
			// get first element
			temp.add(Integer.parseInt(str.substring(0, cpos).trim()));
			int cpos2;
			do {
				cpos2 = str.indexOf(',', cpos + 1);
				if (cpos2 != -1) {
					temp.add(Integer.parseInt(str
							.substring(cpos + 1, cpos2).trim()));
					cpos = cpos2;
				} else {
					temp.add(Integer.parseInt(str.substring(cpos + 1,
							str.length() - 1).trim()));
					cpos = -2;
				}
			} while (cpos != -2);
		}

		int[] rarray = new int[temp.size()];
		for (int i = 0; i < rarray.length; i++) {
			rarray[i] = temp.get(i);
		}
		return rarray;
	}

	/**
	 * get a string
	 * 
	 * @param key
	 *            the string to look for
	 * @param defaultValue
	 *            if key not found, return this
	 * @return the string
	 */
	public String getString(String key, String defaultValue) {
		return getProperty(key, defaultValue).trim();
	}

	/**
	 * set a property
	 * 
	 * @param name
	 *            string of the property
	 * @param var
	 *            Object to put into property
	 */
	public void setProperty(String name, Object var) {
		setProperty(name, var.toString());
	}

}
