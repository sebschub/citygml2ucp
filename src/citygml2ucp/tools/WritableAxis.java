/**
 * 
 */
package citygml2ucp.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * A dimension with values of the values connected to it.
 * 
 * @author Sebastian Schubert
 * 
 */
public class WritableAxis extends WritableDimension {

	/**
	 * Name of the field variable in the NetCDF file
	 */
	public final String axisname;

	/**
	 * Which axis type: either "X", "Y", "Z", "T" or ""
	 */
	public final String axistype;
	/**
	 * Standard name of the field in the NetCDF file
	 */
	public final String standard_name;
	/**
	 * More descriptive name of the field in the NetCDF file
	 */
	public final String long_name;
	/**
	 * Unit of the field in NetCDF file
	 */
	public final String units;
	/**
	 * The values on the axis
	 */
	private final double[] values;

	/**
	 * Axis with periodic boundary condition
	 */
	private boolean isPBC = false;
	/**
	 * x + pbc = x
	 */
	private double pbc;

	/**
	 * constant grid spacing?
	 */
	private boolean isRegular = false;
	/**
	 * Grid spacing
	 */
	private double dv = 0.;

	private boolean reduceLength = false;
	
	/**
	 * Constructor for non-periodic axis with the name of the axis equal to the
	 * name of the dimension.
	 * 
	 * NetCDF can use the values instead the index numbers.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param size
	 *            Length, or UNLIMITED.length or UNKNOWN.length
	 * @param axistype
	 *            either "X", "Y", "Z", "T" or ""
	 * @param standard_name
	 *            Standard name
	 * @param long_name
	 *            Descriptive name
	 * @param units
	 *            Unit of the axis
	 * @param values
	 *            Axis values
	 */
	public WritableAxis(String name, int size, String axistype,
			String standard_name, String long_name, String units,
			double[] values) {
		this(name, size, name, axistype, standard_name, long_name, units,
				values);
	}

	/**
	 * Constructor for non-periodic axis.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param size
	 *            Length, or UNLIMITED.length or UNKNOWN.length
	 * @param axisname
	 *            Name for the NetCDF File
	 * @param axistype
	 *            either "X", "Y", "Z", "T" or ""
	 * @param standard_name
	 *            Standard name
	 * @param long_name
	 *            Descriptive name
	 * @param units
	 *            Unit of the axis
	 * @param values
	 *            Axis values
	 */
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

	/**
	 * Constructor for periodic axis with the name of the axis equal to the name
	 * of the dimension.
	 * 
	 * NetCDF can use the values instead the index numbers.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param size
	 *            Length, or UNLIMITED.length or UNKNOWN.length
	 * @param axistype
	 *            either "X", "Y", "Z", "T" or ""
	 * @param standard_name
	 *            Standard name
	 * @param long_name
	 *            Descriptive name
	 * @param units
	 *            Unit of the axis
	 * @param values
	 *            Axis values
	 * @param pbc
	 *            x+pbc = pbc
	 */
	public WritableAxis(String name, int size, String axistype,
			String standard_name, String long_name, String units,
			double[] values, double pbc) {
		this(name, size, name, axistype, standard_name, long_name, units,
				values, pbc);
	}

	/**
	 * Constructor for periodic axis.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param size
	 *            Length, or UNLIMITED.length or UNKNOWN.length
	 * @param axisname
	 *            Name for the NetCDF File
	 * @param axistype
	 *            either "X", "Y", "Z", "T" or ""
	 * @param standard_name
	 *            Standard name
	 * @param long_name
	 *            Descriptive name
	 * @param units
	 *            Unit of the axis
	 * @param values
	 *            Axis values
	 * @param pbc
	 *            x+pbc = pbc
	 */
	public WritableAxis(String name, int size, String axisname,
			String axistype, String standard_name, String long_name,
			String units, double[] values, double pbc) {
		this(name, size, axisname, axistype, standard_name, long_name, units,
				values);
		this.pbc = pbc;
		isPBC = true;
	}

	/**
	 * Constructor for axis with fixed grid spacing with the name of the axis
	 * equal to the name of the dimension.
	 * 
	 * NetCDF can use the values instead the index numbers.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param size
	 *            Length
	 * @param axistype
	 *            either "X", "Y", "Z", "T" or ""
	 * @param standard_name
	 *            Standard name
	 * @param long_name
	 *            Descriptive name
	 * @param units
	 *            Unit of the axis
	 * @param startvalue
	 *            First axis value
	 * @param dv
	 *            Grid spacing
	 */
	public WritableAxis(String name, int size, String axistype,
			String standard_name, String long_name, String units,
			double startvalue, double dv) {
		this(name, size, name, axistype, standard_name, long_name, units,
				startvalue, dv);
	}

	/**
	 * Constructor for axis with fixed grid spacing.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param size
	 *            Length
	 * @param axisname
	 *            Name for the NetCDF File
	 * @param axistype
	 *            either "X", "Y", "Z", "T" or ""
	 * @param standard_name
	 *            Standard name
	 * @param long_name
	 *            Descriptive name
	 * @param units
	 *            Unit of the axis
	 * @param startvalue
	 *            First axis value
	 * @param dv
	 *            Grid spacing
	 */
	public WritableAxis(String name, int size, String axisname,
			String axistype, String standard_name, String long_name,
			String units, double startvalue, double dv) {
		this(name, size, axisname, axistype, standard_name, long_name, units,
				generate_valuefield(size, startvalue, dv));
		isRegular = true;
		this.dv = dv;
	}

	/**
	 * Constructor for periodic axis with fixed grid spacing with the name of
	 * the axis equal to the name of the dimension.
	 * 
	 * NetCDF can use the values instead the index numbers.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param size
	 *            Length
	 * @param axistype
	 *            either "X", "Y", "Z", "T" or ""
	 * @param standard_name
	 *            Standard name
	 * @param long_name
	 *            Descriptive name
	 * @param units
	 *            Unit of the axis
	 * @param startvalue
	 *            First axis value
	 * @param dv
	 *            Grid spacing
	 * @param pbc
	 *            x+pbc=pbc
	 */
	public WritableAxis(String name, int size, String axistype,
			String standard_name, String long_name, String units,
			double startvalue, double dv, double pbc) {
		this(name, size, name, axistype, standard_name, long_name, units,
				startvalue, dv, pbc);
	}

	/**
	 * Constructor for periodic axis with fixed grid spacing.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param size
	 *            Length
	 * @param axisname
	 *            Name for the NetCDF File
	 * @param axistype
	 *            either "X", "Y", "Z", "T" or ""
	 * @param standard_name
	 *            Standard name
	 * @param long_name
	 *            Descriptive name
	 * @param units
	 *            Unit of the axis
	 * @param startvalue
	 *            First axis value
	 * @param dv
	 *            Grid spacing
	 * @param pbc
	 *            x+pbc=pbc
	 */
	public WritableAxis(String name, int size, String axisname,
			String axistype, String standard_name, String long_name,
			String units, double startvalue, double dv, double pbc) {
		this(name, size, axisname, axistype, standard_name, long_name, units,
				startvalue, dv);
		this.pbc = pbc;
		isPBC = true;
	}

	/**
	 * Generate values from a start value and a grid spacing.
	 * 
	 * @param size
	 *            Number of points
	 * @param startvalue
	 *            First point
	 * @param dv
	 *            Grid spacing
	 * @return Array of values
	 */
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

	@Override
	public DimensionsAndVariables addToNetCDFfile(NetcdfFileWriter ncfile) {
		DimensionsAndVariables dimensionsAndVariables = this.dimensionsAndVariablesAddedToNetcdf.get(ncfile);
		if (Objects.isNull(dimensionsAndVariables)) {
			Dimension dim = ncfile.addDimension(null, this.shortName, this.getLength());
			List<Dimension> ncdims = Arrays.asList(dim);
			
			Variable var = ncfile.addVariable(null, this.axisname, DataType.FLOAT, ncdims);
			if (!axistype.isEmpty()) {
				ncfile.addVariableAttribute(var, new Attribute("axis", axistype));
			}
			ncfile.addVariableAttribute(var, new Attribute("standard_name",
					standard_name));
			ncfile.addVariableAttribute(var, new Attribute("long_name", long_name));
			if (!units.isEmpty()) {
				ncfile.addVariableAttribute(var, new Attribute("units", units));
			}
			dimensionsAndVariables = new DimensionsAndVariables(ncdims, Arrays.asList(var));
			this.dimensionsAndVariablesAddedToNetcdf.put(ncfile, dimensionsAndVariables);
		}
		return dimensionsAndVariables;
	}

	@Override
	public void writeToNetCDFfile(NetcdfFileWriter ncfile) throws IOException, InvalidRangeException {
		DimensionsAndVariables dimensionsAndVariables = this.dimensionsAndVariablesAddedToNetcdf.get(ncfile);
		if (reduceLength) {
			double[] val = new double[this.getLength()];
			for (int i = 0; i < val.length; i++) {
				val[i] = values[i];
			}
			ncfile.write(dimensionsAndVariables.variable.get(0), Array.factory(val));
		} else {
			ncfile.write(dimensionsAndVariables.variable.get(0), Array.factory(values));
		}
	}

	/**
	 * Does the Axis have a constant grid spacing?
	 * 
	 * @return isRegular
	 */
	public boolean isRegular() {
		return isRegular;
	}

	/**
	 * Get the nearest index of a value for given monotonously growing array
	 * using binary search.
	 * 
	 * @param item
	 *            The value to find the index for
	 * @param values
	 *            The grid values
	 * @return Index of {@code values} nearest to {@code value}
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
			if (values[i2] - getDValue(i2, values) / 2. > item) {
				end = i2;
			} else {
				start = i2;
			}
		} while (!(end == start + 1));

		return start;
	}

	/**
	 * Find the index on the axis which corresponds to the axis value nearest to
	 * the given value.
	 * 
	 * @param value
	 *            Find index for that value
	 * @return Index for the input value
	 * @throws IllegalArgumentException
	 *             {@code value} is outside of the range of this axis
	 */
	public int getIndexOf(double value) {
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

			int expReturn = binarySearch(value, expandedValues);

			if (expReturn == 0) {
				returnValue = values.length - 1;
			} else if (expReturn == values.length + 1) {
				returnValue = 0;
			} else {
				returnValue = expReturn - 1;
			}
		} else {
			if (value < values[0] - getDValue(0, this.values) / 2.
					|| value > values[this.getLength() - 1]
							+ getDValue(getLength() - 1, this.values) / 2.) {
				throw new IllegalArgumentException("Value " + value
						+ " out of range of axis " + this.axisname + ".");
			}

			returnValue = binarySearch(value, this.values);
		}

		return returnValue;
	}

	/**
	 * Get the axis value for an index.
	 * 
	 * @param i
	 *            Index to get the value for
	 * @return Axis value
	 * @throws IllegalArgumentException
	 *             {@code i} is not in the range of the axis.
	 */
	public double getValue(int i) throws IllegalArgumentException {
		if (i < 0 || i >= values.length) {
			throw new IllegalArgumentException("Axisindex " + i
					+ " out of range of axis " + this.axisname + ".");
		}
		return values[i];
	}

	/**
	 * Get the grid spacing between a value of a given index and the next value.
	 * 
	 * @param i
	 *            Index of the first value
	 * @return Grid spacing
	 * @throws IllegalArgumentException
	 *             {@code i} is not in the range of the axis.
	 */
	public double getDValue(int i) {
		try {
			return getDValue(i, this.values);
		} catch (IllegalArgumentException exep) {
			throw new IllegalArgumentException("Axisindex " + i
					+ " out of range of axis " + this.axisname + ".");
		}
	}

	/**
	 * Get the grid spacing between a value of a given index and the former
	 * value for a given set of axis values.
	 * 
	 * @param i
	 *            Index of the first value
	 * @param values
	 *            Values to calculate grid spacing for
	 * @return Grid spacing
	 * @throws IllegalArgumentException
	 *             {@code i} is not in the range of the axis.
	 */
	private double getDValue(int i, double[] values) {
		if (i < 0 || i > values.length - 1)
			throw new IllegalArgumentException("Axisindex " + i
					+ " out of range of given values.");
		if (isRegular) {
			return dv;
		}
		if (i == 0) {
			return values[i + 1] - values[i];
		}
		return values[i] - values[i - 1];
	}

	@Override
	public void setLength(int n) {
		if (values != null) {
			if (n > values.length) {
				throw new IllegalArgumentException("New length of axis "
						+ this.axisname
						+ " has to be smaller than the initial value "
						+ values.length);
			}
			reduceLength = true;
		}
		super.setLength(n);
	}

}
