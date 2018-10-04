package citygml2ucp.tools;

import java.util.List;

import ucar.ma2.DataType;

/**
 * A field that can be written to a NetCDF file as double.
 * 
 * @author Sebastian Schubert
 * 
 */
public class WritableFieldDouble extends WritableField {

	public WritableFieldDouble(String name, List<WritableDimension> dimlist,
			String standard_name, String long_name, String units,
			String grid_mapping) {
		super(name, dimlist, standard_name, long_name, units, grid_mapping,
				DataType.DOUBLE);
	}

}