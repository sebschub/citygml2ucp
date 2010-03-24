package pik.clminputdata.tools;

import java.util.List;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;

/**
 * A field that can be written to a NetCDF file as float.
 * 
 * @author Sebastian Schubert
 * 
 */
public class WritableFieldFloat extends WritableField {

	public WritableFieldFloat(String name, List<Dimension> dimlist,
			String standard_name, String long_name, String units,
			String grid_mapping) {
		super(name, dimlist, standard_name, long_name, units, grid_mapping,
				DataType.FLOAT);
	}

}