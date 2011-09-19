package pik.clminputdata.tools;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

/**
 * A field that can be written to a NetCDF file.
 * 
 * During calculations, a double field is always used to be able to inherit from
 * {@code ArrayDouble}, just {@code Array} cannot be used (better idea?).
 * 
 * @author Sebastian Schubert
 * 
 */
public class WritableField extends ArrayDouble implements NetCDFWritable {

	public final double missingValue=-999.;
	
	/**
	 * Name of the field variable in the NetCDF file
	 */
	protected final String name;
	/**
	 * Standard name of the field in the NetCDF file
	 */
	protected final String standard_name;
	/**
	 * More descriptive name of the field in the NetCDF file
	 */
	protected final String long_name;
	/**
	 * Unit of the field in NetCDF file
	 */
	protected final String units;
	/**
	 * Information about the mapping of the field, e.g. "rotated_pole"
	 */
	protected final String grid_mapping;

	/**
	 * Dimensions of the field
	 */
	protected final List<Dimension> dimlist;
	
	/**
	 * Output data type 
	 */
	protected final DataType outputType;
	
	private boolean doOutputPart = false;
	
	private int[] originPart;
	private int[] shapePart;
	
	/**
	 * Get the length of a list of dimensions.
	 * 
	 * @param diml
	 *            List of dimensions
	 * @return Array of lengths
	 */
	private static int[] getDimensions(List<Dimension> diml) {
		int[] dimint = new int[diml.size()];
		for (int i = 0; i < dimint.length; i++) {
			dimint[i] = diml.get(i).getLength();
		}
		return dimint;
	}

	public WritableField(String name, List<Dimension> dimlist,
			String standard_name, String long_name, String units,
			String grid_mapping, DataType outputType) {
		super(getDimensions(dimlist));
		this.name = name;
		this.standard_name = standard_name;
		this.long_name = long_name;
		this.units = units;
		this.grid_mapping = grid_mapping;
		this.dimlist = new LinkedList<Dimension>();
		this.dimlist.addAll(dimlist);
		this.outputType = outputType;
	}

	public void resetDim() {
		int[] origin = new int[dimlist.size()];
		int[] shape = new int[dimlist.size()];
		for (int i = 0; i < origin.length; i++) {
			origin[i] = 0;
			shape[i] = dimlist.get(i).getLength();
		}
		setOutputPart(origin, shape);
	}
	
	public void setOutputPart(int[] origin, int[] shape) {
		doOutputPart = true;
		this.originPart = origin;
		this.shapePart = shape;
	}
	
	public int[] getOriginPart() {
		return originPart;
	}

	public int[] getShapePart() {
		return shapePart;
	}

	/**
	 * @return Size of the field when saved to NetCDF
	 */
	public long getSavedSizeBytes() {
		return this.getSizeBytes();
	}
	
	/*
	 * (non-Javadoc) Here it is defined to write that field as float.
	 * 
	 * @see
	 * pik.clminputdata.configuration.NetCDFWritable#addVariablesToNetCDFfile
	 * (ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile) {
		Variable var = ncfile.addVariable(name, this.outputType, this.dimlist);
		ncfile.addVariableAttribute(var, new Attribute("standard_name",
				standard_name));
		ncfile.addVariableAttribute(var, new Attribute("long_name", long_name));
		ncfile.addVariableAttribute(var, new Attribute("_FillValue", missingValue));
		if (!units.isEmpty()) {
			ncfile.addVariableAttribute(var, new Attribute("units", units));
		}
		if (!grid_mapping.isEmpty()) {
			ncfile.addVariableAttribute(var, new Attribute("grid_mapping",
					grid_mapping));
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pik.clminputdata.configuration.NetCDFWritable#writeVariablesToNetCDFfile
	 * (ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile)
			throws IOException, InvalidRangeException {
		if (doOutputPart) {
			ncfile.write(name, this.sectionNoReduce(originPart, shapePart, null));
		} else {
			ncfile.write(name, this);
		}
	}
}
