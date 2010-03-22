/**
 * 
 */
package pik.clminputdata.tools;

import java.io.IOException;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

/**
 * @author Sebastian Schubert
 * 
 */
public class WritableAxis extends WritableDimension {

	public final String axisname;
	public final String axistype;
	public final String standard_name;
	public final String long_name;
	public final String units;
	public final double[] values;

	private boolean isPBC = false;
	private double pbc;

	private boolean isRegular = false;
	private double dv = 0.;

	public WritableAxis(String name, int size, String axisname,
			String axistype, String standard_name, String long_name,
			String units, double[] values) {
		super(name, size);
		if (axisname.isEmpty()) {
			throw new IllegalArgumentException(
					"axisname is not allowed to be empty");
		}
		this.axisname = axisname;

		if (standard_name.isEmpty()) {
			throw new IllegalArgumentException(
					"standard_name is not allowed to be empty");
		}
		this.standard_name = standard_name;

		if (long_name.isEmpty()) {
			throw new IllegalArgumentException(
					"long_name is not allowed to be empty");
		}
		this.long_name = long_name;

		this.units = units;

		if (axistype != "X" && axistype != "Y" && axistype != "Z"
				&& axistype != "T" && !axistype.isEmpty()) {
			throw new IllegalArgumentException(
					"Only \"X\", \"Y\", \"Z\", \"T\", \"\" allowed as axistype");
		}
		this.axistype = axistype;

		if (values.length != size) {
			throw new IllegalArgumentException("values not of right size");
		}
		for (int i = 1; i < values.length; i++) {
			if (values[i - 1] > values[i]) {
				throw new IllegalArgumentException(
						"values must be monotonously increasing");
			}
		}
		this.values = values;
	}

	public WritableAxis(String name, int size, String axisname,
			String axistype, String standard_name, String long_name,
			String units, double[] values, double pbc) {
		this(name, size, axisname, axistype, standard_name, long_name, units,
				values);
		this.pbc = pbc;
		isPBC = true;
	}

	public WritableAxis(String name, int size, String axisname,
			String axistype, String standard_name, String long_name,
			String units, double startvalue, double dv) {
		this(name, size, axisname, axistype, standard_name, long_name, units,
				generate_valuefield(size, startvalue, dv));
		isRegular = true;
		this.dv = dv;
	}

	public WritableAxis(String name, int size, String axisname,
			String axistype, String standard_name, String long_name,
			String units, double startvalue, double dv, double pbc) {
		this(name, size, axisname, axistype, standard_name, long_name, units,
				startvalue, dv);
		this.pbc = pbc;
		isPBC = true;
	}

	private static double[] generate_valuefield(int size, double startvalue,
			double dv) {
		double[] valuestemp = new double[size];
		double site = startvalue;
		for (int i = 0; i < size; i++) {
			valuestemp[i] = site;
			site += dv;
		}
		return valuestemp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pik.clminputdata.configuration.Coordinate1D#addVariablesToNetCDFfile(
	 * ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile) {

		List<Dimension> l = super.addVariablesToNetCDFfile(ncfile);

		Variable var = ncfile.addVariable(axisname, DataType.FLOAT, l);
		if (!axistype.isEmpty()) {
			ncfile.addVariableAttribute(var.getShortName(), "axis", axistype);
		}
		ncfile.addVariableAttribute(var.getShortName(), "standard_name",
				standard_name);
		ncfile.addVariableAttribute(var.getShortName(), "long_name", long_name);
		if (!units.isEmpty()) {
			ncfile.addVariableAttribute(var.getShortName(), "units", units);
		}
		return l;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pik.clminputdata.configuration.Coordinate1D#writeVariablesToNetCDFfile
	 * (ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile)
			throws IOException, InvalidRangeException {
		// super.writeVariablesToNetCDFfile(ncfile);
		ncfile.write(axisname, Array.factory(values));
	}

	/**
	 * @return the isRegular
	 */
	public boolean isRegular() {
		return isRegular;
	}

	/**
	 * searching using binary search
	 * 
	 * @param value
	 * @param values
	 * @return
	 */
	private int binarySearch(double item, double[] values) {
		// it would never reach last index with code below
		if (item >= values[values.length - 1]
				- getDValue(values.length - 1, values) / 2.) {
			return values.length - 1;
		}

		int start = 0;
		int end = values.length - 1;
		int i2 = 0;

		do {
			i2 = (start + end) / 2;
			// System.out.print(values[start] + "  ");
			// System.out.print(values[i2] - getDValue(i2) / 2. + "  ");
			// System.out.println(values[end]);
			// System.out.println("start: " + start + " i2: " + i2 + " end: "
			// + end);
			if (values[i2] - getDValue(i2, values) / 2. > item) {
				end = i2;
			} else {
				start = i2;
			}
		} while (!(end == start + 1));

		return start;
	}

	public int getIndexOf(double value) throws IllegalArgumentException {
		int returnValue;
		if (isPBC) {
			double[] expandedValues = new double[values.length + 2];

			// last entry in values as new entry for expandedValues
			expandedValues[0] = values[values.length - 1] - pbc;

			for (int i = 0; i < values.length; i++) {
				expandedValues[i + 1] = values[i];
			}

			// values.length + 2 - 1
			expandedValues[values.length + 1] = values[0] + pbc;

//			for (double d : expandedValues) {
//				System.out.println(d);
//			}
//			System.out.println("-------------");			
			int expReturn = binarySearch(value, expandedValues);

			if (expReturn == 0) {
				returnValue =  values.length-1;
			} else if (expReturn == values.length + 1) {
				returnValue =  0;
			} else {
				returnValue = expReturn - 1;
			}
		} else {
			if (value < values[0] - getDValue(0, this.values) / 2.
					|| value > values[this.getLength() - 1]
							+ getDValue(getLength() - 1, this.values) / 2.) {
				throw new IllegalArgumentException("value out of range");
			}

			returnValue = binarySearch(value, this.values);
		}
		
		return returnValue;
	}

	public double getValue(int i) throws IllegalArgumentException {
		return values[i];
	}
	
	public double getDValue(int i) throws IllegalArgumentException {
		return getDValue(i, this.values);
	}

	public double getDValue(int i, double[] values)
			throws IllegalArgumentException {
		if (i < 0 || i > values.length - 1)
			throw new IllegalArgumentException("index not in range");
		if (isRegular) {
			return dv;
		}
		if (i == values.length - 1) {
			return values[i] - values[i - 1];
		}
		return values[i + 1] - values[i];
	}

}
