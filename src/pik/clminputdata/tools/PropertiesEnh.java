package pik.clminputdata.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

public class PropertiesEnh extends Properties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int getInt(String key, int defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		} else {
			return Integer.parseInt(str.trim());
		}
	}

	public double getDouble(String key, double defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		} else {
			return Double.parseDouble(str.trim());
		}
	}

	public double[] getDoubleArray(String key, double[] defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		} else {

			str = str.trim();

			ArrayList<Double> temp = new ArrayList<Double>();

			int cpos = str.indexOf(',');
			if (cpos == -1) {
				temp.add(Double.parseDouble(str.trim()));
			} else {
				// get first element
				temp.add(Double.parseDouble(str.substring(0,
						cpos).trim()));
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
	}

	public int[] getIntArray(String key, int[] defaultValue) {
		String str = getProperty(key);
		if (str == null) {
			return defaultValue;
		} else {

			str = str.trim();

			ArrayList<Integer> temp = new ArrayList<Integer>();

			int cpos = str.indexOf(',');
			if (cpos == -1) {
				temp.add(Integer.parseInt(str.trim()));
			} else {
				// get first element
				temp.add(Integer.parseInt(str.substring(0,
						cpos).trim()));
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
	}

	public String getString(String key, String defaultValue) {
		return getProperty(key, defaultValue).trim();
	}

	public void setProperty(String name, Object var) {
		setProperty(name, var.toString());
	}

}
