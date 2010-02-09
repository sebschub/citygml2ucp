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

public class WritableField extends ArrayDouble implements NetCDFWritable {

	protected String name, standard_name, long_name, units, grid_mapping;

	protected List<Dimension> dimlist;

	private static int[] getDimensions(List<Dimension> diml) {
		int[] dimint = new int[diml.size()];
		for (int i = 0; i < dimint.length; i++) {
//			if (diml.get(i).isUnlimited()) {
//				dimint[i] = 1;
//			} else {
				dimint[i] = diml.get(i).getLength();
//			}
		}
		return dimint;
	}

	public WritableField(String name, List<Dimension> dimlist,
			String standard_name, String long_name, String units,
			String grid_mapping) {
		super(getDimensions(dimlist));
		this.name = name;
		this.standard_name = standard_name;
		this.long_name = long_name;
		this.units = units;
		this.grid_mapping = grid_mapping;
		this.dimlist = new LinkedList<Dimension>();
		this.dimlist.addAll(dimlist);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pik.clminputdata.configuration.NetCDFWritable#addVariablesToNetCDFfile
	 * (ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile) {
		Variable var = ncfile.addVariable(name, DataType.FLOAT, this.dimlist);
		ncfile.addVariableAttribute(var, new Attribute("standard_name",
				standard_name));
		ncfile.addVariableAttribute(var, new Attribute("long_name", long_name));
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
		ncfile.write(name, this);
	}
}
