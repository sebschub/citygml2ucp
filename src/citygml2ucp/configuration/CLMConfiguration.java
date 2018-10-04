package citygml2ucp.configuration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import citygml2ucp.tools.DimensionsAndVariables;
import citygml2ucp.tools.NetCDFData;
import citygml2ucp.tools.WritableAxis;
import citygml2ucp.tools.WritableDimension;
import citygml2ucp.tools.WritableField;
import citygml2ucp.tools.WritableFieldDouble;
import citygml2ucp.tools.WritableRotatedPole;
import ucar.ma2.Index;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * A class that sets up basic configuration properties for a CCLM run.
 * 
 * @author Sebastian Schubert
 * 
 */
public class CLMConfiguration extends NetCDFData {

	/**
	 * Radius of earth in km
	 */
	public final static double r = 6371.229;

	/**
	 * Rotated pole
	 */
	public final WritableRotatedPole rotpol;

	/**
	 * Latitude in rotated coordinate system
	 */
	protected final WritableAxis meridionalAxis;
	/**
	 * Longitude in rotated coordinate system
	 */
	protected final WritableAxis zonalAxis;

	//	/**
	//	 * Vertical dimension
	//	 */
	//	protected final WritableDimension verticalDimension;

	/**
	 * Area of a grid cell, a function of latitude
	 */
	protected final WritableField area;

	/**
	 * Corresponding latitude and longitude on the non-rotated grid
	 */
	protected final WritableField lat, lon;

	private List<String> confItems, confValues;
	
	/**
	 * Initialize with default values of CLM
	 * 
	 */
	public CLMConfiguration() {
		this(32.5, -170.0, 0.008, 0.008, -7.972, -1.252, 51, 51);
	}

	/**
	 * Initialize with custom values and additional setting fields, checked for
	 * validity
	 * 
	 * @throws IllegalArgumentException
	 *             One of the arguments is not in the correct range
	 */
	public CLMConfiguration(double pollat, double pollon, double dlat,
			double dlon, double startlat_tot, double startlon_tot, int ie_tot,
			int je_tot, List<String> confItems, List<String> confValues)
			throws IllegalArgumentException {
		this(pollat, pollon, dlat, dlon, startlat_tot, startlon_tot, ie_tot, je_tot);
		if (confItems.size() != confValues.size())
			throw new IllegalArgumentException(
					"Number of setting names and values are not equal");
		this.confItems = confItems;
		this.confValues = confValues;
	}
	
	/**
	 * Initialize with custom values, checked for validity
	 * 
	 * @throws IllegalArgumentException
	 *             One of the arguments is not in the correct range
	 */
	public CLMConfiguration(double pollat, double pollon, double dlat,
			double dlon, double startlat_tot, double startlon_tot, int ie_tot,
			int je_tot) throws IllegalArgumentException {

		rotpol = new WritableRotatedPole(pollat, pollon);
		addToWrite(rotpol);

		if (dlat < 0 || dlat > 90) {
			throw new IllegalArgumentException("dlat out of range.");
		}

		if (dlon < 0 || dlon > 180) {
			throw new IllegalArgumentException("dlon out of range.");
		}

		if (startlat_tot < -90 || startlat_tot > 90) {
			throw new IllegalArgumentException("startlat_tot out of range.");
		}

		if (startlon_tot < -180 || startlon_tot > 180) {
			throw new IllegalArgumentException("startlon_tot out of range.");
		}

		if (ie_tot < 0) {
			throw new IllegalArgumentException("ie_tot out of range.");
		}

		if (je_tot < 0) {
			throw new IllegalArgumentException("je_tot out of range.");
		}

		// if (ke_tot < 0) {
		// throw new IllegalArgumentException("ke_tot out of range.");
		// }

		zonalAxis = new WritableAxis("rlon", ie_tot, "rlon", "X",
				"grid_longitude", "rotated longitude", "degrees", startlon_tot,
				dlon);
		addToWrite(zonalAxis);

		meridionalAxis = new WritableAxis("rlat", je_tot, "rlat", "Y",
				"grid_latitude", "rotated latitude", "degrees", startlat_tot,
				dlat);
		addToWrite(meridionalAxis);

		// verticalDimension = new WritableDimension("level", ke_tot);
		// addToWrite(verticalDimension); //add in subclass if needed

		List<WritableDimension> dimlist = new ArrayList<>();
		dimlist.add(meridionalAxis);

		area = new WritableFieldDouble("area", dimlist, "area_element",
				"size of area element", "km2", "");
		calculateArea();
		addToWrite(area);

		dimlist.add(zonalAxis);

		lon = new WritableFieldDouble("lon", dimlist, "longitude", "longitude",
				"degrees east", "");
		addToWrite(lon);

		lat = new WritableFieldDouble("lat", dimlist, "latitude", "latitude",
				"degrees north", "");
		addToWrite(lat);

		calculateTrueLatLon();
	}

	/**
	 * Calculate the area of the grid cell and save it.
	 * 
	 */
	protected void calculateArea() {
		Index ind = area.getIndex();
		double fac = 2. * r * r * Math.toRadians(getDlon())
				* Math.sin(Math.toRadians(getDlat() / 2.));
		for (int i = 0; i < meridionalAxis.getLength(); i++) {
			area.set(ind.set(i), fac * Math.cos(Math.toRadians(getRLat(i))));
		}
	}

	/**
	 * Calculate the true non-rotated coordinates.
	 */
	protected void calculateTrueLatLon() {
		Index ilat = lat.getIndex();
		Index ilon = lon.getIndex();
		for (int i = 0; i < zonalAxis.getLength(); i++) {
			for (int j = 0; j < meridionalAxis.getLength(); j++) {
				LatLonPoint llp = rotpol.projToLatLon(zonalAxis.getValue(i),
						meridionalAxis.getValue(j));
				lat.set(ilat.set(j, i), llp.getLatitude());
				lon.set(ilon.set(j, i), llp.getLongitude());
			}
		}
	}

	/**
	 * Add additional parameters (institute, convention, date) to NetCDF file.
	 * 
	 * @param ncfile
	 *            NetCDF file
	 */
	protected void addGlobalAttributesToNetCDFfile(NetcdfFileWriter ncfile) {
		ncfile.addGroupAttribute(null, new Attribute("institution", "PIK"));
		ncfile.addGroupAttribute(null, new Attribute("Conventions", "CF-1.4"));
		ncfile.addGroupAttribute(null, new Attribute("conventionsURL",
				"http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.4"));

		DateFormatter df = new DateFormatter();
		ncfile.addGroupAttribute(null, new Attribute("creation_date", df
				.toDateTimeString(new Date())));

		if (confItems!=null) {
			for (int i = 0; i < confItems.size(); i++) {
				ncfile.addGroupAttribute(null, new Attribute(confItems.get(i), confValues.get(i)));
			}
		}
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * citygml2ucp.configuration.NetCDFData#addVariablesToNetCDFfile(ucar
	 * .nc2.NetcdfFileWriteable)
	 */
	@Override
	public DimensionsAndVariables addToNetCDFfile(NetcdfFileWriter ncfile) {
		addGlobalAttributesToNetCDFfile(ncfile);
		return super.addToNetCDFfile(ncfile);
	}

	/**
	 * Get 'Meridional' (rotated lat-direction) grid spacing (in degrees), 0 if
	 * non-regular axis
	 * 
	 * @return Grid spacing
	 */
	public double getDlat() {
		return meridionalAxis.getDValue(0);
	}

	/**
	 * Get 'Zonal' (rotated lon-direction) grid spacing (in degrees), 0 if
	 * non-regular
	 * 
	 * @return Grid spacing
	 */
	public double getDlon() {
		return zonalAxis.getDValue(0);
	}

	/**
	 * Get latitude of the lower left grid point of the total domain (in
	 * degrees, north>0, rotated coordinates)
	 * 
	 * @return Starting latitude
	 */
	public double getStartlat_tot() {
		return meridionalAxis.getValue(0);
	}

	/**
	 * Get longitude of the lower left grid point of the total domain (in
	 * degrees, east > 0, rotated coordinates)
	 * 
	 * @return Starting longitude
	 */
	public double getStartlon_tot() {
		return zonalAxis.getValue(0);
	}

	/**
	 * Get total number if grid points in zonal direction.
	 * 
	 * @return Number of grid points
	 */
	public int getIe_tot() {
		return zonalAxis.getLength();
	}

	/**
	 * Get total number if grid points in meridional direction.
	 * 
	 * @return Number of grid points
	 */
	public int getJe_tot() {
		return meridionalAxis.getLength();
	}

	//	/**
	//	 * Get total number if grid points in vertical direction.
	//	 * 
	//	 * @return Number of grid points
	//	 */
	//	public int getKe_tot() {
	//		return verticalDimension.getLength();
	//	}

	/**
	 * Get geographical longitude of the rotated north pole.
	 * 
	 * @return Longitude of rotated north pole
	 */
	public double getPollon() {
		return rotpol.getNorthPole().getX();
	}

	/**
	 * Geographical latitude of the rotated north pole
	 * 
	 * @return Latitude of rotated north pole
	 */
	public double getPollat() {
		return rotpol.getNorthPole().getY();
	}

	/**
	 * Get latitude of an index in rotated system.
	 * 
	 * @param j
	 *            Index
	 * @return Latitude
	 */
	public double getRLat(int j) {
		return meridionalAxis.getValue(j);
	}

	/**
	 * Get longitude of an index in rotated system.
	 * 
	 * @param i
	 *            Index
	 * @return Longitude
	 */
	public double getRLon(int i) {
		return zonalAxis.getValue(i);
	}

	/**
	 * Get zonal index of an axis value.
	 * 
	 * @param lon
	 *            Axis value
	 * @return Index
	 */
	public int getRLonIndex(double lon) {
		return zonalAxis.getIndexOf(lon);
	}

	/**
	 * Get meridional index of an axis value.
	 * 
	 * @param lat
	 *            Axis value
	 * @return Index
	 */
	public int getRLatIndex(double lat) {
		return meridionalAxis.getIndexOf(lat);
	}

	/**
	 * Get the area of a grid cell.
	 * 
	 * @param rlati
	 *            in rotated system of the grid cell
	 * @return Area of the cell
	 * @throws IllegalArgumentException
	 *             Latitude not in range
	 */
	public double getArea(int rlati) throws IllegalArgumentException {
		if (rlati >= getJe_tot() || rlati < 0) {
			throw new IllegalArgumentException("rlati out of range.");
		}
		Index ind = area.getIndex();
		return area.get(ind.set(rlati));
	}

	/**
	 * Get the geographical latitude for indices.
	 * 
	 * @param j
	 *            Latitude index in rotated system
	 * @param i
	 *            Longitude index in rotated system
	 * @return Geographical latitude
	 * @throws IllegalArgumentException
	 *             Index not in range
	 */
	public double getLat(int j, int i) throws IllegalArgumentException {
		if (j >= getJe_tot() || j < 0) {
			throw new IllegalArgumentException("j out of range.");
		}
		if (i >= getIe_tot() || i < 0) {
			throw new IllegalArgumentException("i out of range.");
		}
		Index ind = lat.getIndex();
		return lat.get(ind.set(j, i));
	}

	/**
	 * Get the geographical longitude for indices.
	 * 
	 * @param j
	 *            Latitude index in rotated system
	 * @param i
	 *            Longitude index in rotated system
	 * @return Geographical longitude
	 * @throws IllegalArgumentException
	 *             Index not in range
	 */
	public double getLon(int j, int i) throws IllegalArgumentException {
		if (j >= getJe_tot() || j < 0) {
			throw new IllegalArgumentException("j out of range");
		}
		if (i >= getIe_tot() || i < 0) {
			throw new IllegalArgumentException("i out of range");
		}
		Index ind = lon.getIndex();
		return lon.get(ind.set(j, i));
	}
}