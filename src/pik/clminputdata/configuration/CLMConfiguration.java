package pik.clminputdata.configuration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import pik.clminputdata.tools.NetCDFData;
import pik.clminputdata.tools.WritableAxis;
import pik.clminputdata.tools.WritableDimension;
import pik.clminputdata.tools.WritableField;
import pik.clminputdata.tools.WritableRotatedPole;

import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
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
	 * radius of earth in km
	 */
	public final static double r = 6371.229;

	/**
	 * rotated pole
	 */
	public final WritableRotatedPole rotpol;

	protected WritableAxis meridionalAxis;

	protected WritableAxis zonalAxis;

	protected WritableDimension verticalDimension;

	/**
	 * The area of a grid cell, a function of latitude.
	 */
	protected WritableField area;

	/**
	 * the corresponding latitude and longitude on the non-rotated grid
	 */
	protected WritableField lat, lon;

	/**
	 * Initialize with default values of CLM
	 * 
	 * @throws InvalidRangeException
	 * @throws IllegalArgumentException
	 */
	public CLMConfiguration() throws IllegalArgumentException,
			InvalidRangeException {
		this(32.5, -170.0, 0.008, 0.008, -7.972, -1.252, 51, 51, 20);
	}

	/**
	 * Initialize with custom values, checked for validity
	 * 
	 * @throws IllegalArgumentException
	 *             One of the arguments is not in the correct range
	 * @throws InvalidRangeException
	 */
	public CLMConfiguration(double pollat, double pollon, double dlat,
			double dlon, double startlat_tot, double startlon_tot, int ie_tot,
			int je_tot, int ke_tot) throws IllegalArgumentException,
			InvalidRangeException {

		rotpol = new WritableRotatedPole(pollat, pollon);
		toWrite.add(rotpol);

		if (dlat < 0 || dlat > 90) {
			throw new IllegalArgumentException("dlat out of range");
		}

		if (dlon < 0 || dlon > 180) {
			throw new IllegalArgumentException("dlon out of range");
		}

		if (startlat_tot < -90 || startlat_tot > 90) {
			throw new IllegalArgumentException("startlat_tot out of range");
		}

		if (startlon_tot < -180 || startlon_tot > 180) {
			throw new IllegalArgumentException("startlon_tot out of range");
		}

		if (ie_tot < 0) {
			throw new IllegalArgumentException("ie_tot out of range");
		}

		if (je_tot < 0) {
			throw new IllegalArgumentException("je_tot out of range");
		}

		if (ke_tot < 0) {
			throw new IllegalArgumentException("ke_tot out of range");
		}

		zonalAxis = new WritableAxis("rlon", ie_tot, "rlon", "X",
				"grid_longitude", "rotated longitude", "degrees", startlon_tot,
				dlon);
		toWrite.add(zonalAxis);

		meridionalAxis = new WritableAxis("rlat", je_tot, "rlat", "Y",
				"grid_latitude", "rotated latitude", "degrees", startlat_tot,
				dlat);
		toWrite.add(meridionalAxis);

		verticalDimension = new WritableDimension("level", ke_tot);
		// toWrite.add(verticalDimension); //add in subclass if needed

		List<Dimension> dimlist = new ArrayList<Dimension>();
		dimlist.add(meridionalAxis);

		area = new WritableField("area", dimlist, "area_element",
				"size of area element", "km2", "");
		calculateArea();
		toWrite.add(area);

		dimlist.add(zonalAxis);

		lon = new WritableField("lon", dimlist, "longitude", "longitude",
				"degrees east", "");
		toWrite.add(lon);

		lat = new WritableField("lat", dimlist, "latitude", "latitude",
				"degrees north", "");
		toWrite.add(lat);

		calculateTrueLatLon();
	}

	/**
	 * Calculate the area of the grid cell and save it.
	 * 
	 * @throws InvalidRangeException
	 */
	protected void calculateArea() throws InvalidRangeException {
		Index ind = area.getIndex();
		double fac = 2. * r * r * Math.toRadians(getDlon())
				* Math.sin(Math.toRadians(getDlat() / 2.));
		for (int i = 0; i < meridionalAxis.getLength(); i++) {
			area.set(ind.set(i), fac * Math.cos(Math.toRadians(getRLat(i))));
		}
	}

	protected void calculateTrueLatLon() {
		Index ilat = lat.getIndex();
		Index ilon = lon.getIndex();
		for (int i = 0; i < zonalAxis.getLength(); i++) {
			for (int j = 0; j < meridionalAxis.getLength(); j++) {
				LatLonPoint llp = rotpol.projToLatLon(zonalAxis.values[i],
						meridionalAxis.values[j]);
				lat.set(ilat.set(j, i), llp.getLatitude());
				lon.set(ilon.set(j, i), llp.getLongitude());
			}
		}
	}

	protected void addGlobalAttributesToNetCDFfile(NetcdfFileWriteable ncfile) {
		ncfile.addGlobalAttribute("institution", "PIK");
		ncfile.addGlobalAttribute("Conventions", "CF-1.4");
		ncfile.addGlobalAttribute("conventionsURL",
				"http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.4");

		DateFormatter df = new DateFormatter();
		ncfile.addGlobalAttribute("creation_date", df
				.toDateTimeString(new Date()));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pik.clminputdata.configuration.NetCDFData#addVariablesToNetCDFfile(ucar
	 * .nc2.NetcdfFileWriteable)
	 */
	@Override
	public List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile) {
		addGlobalAttributesToNetCDFfile(ncfile);
		return super.addVariablesToNetCDFfile(ncfile);
	}

	/**
	 * @return 'Meridional' (rotated lat-direction) grid spacing (in degrees), 0
	 *         if non-regular axis
	 */
	public double getDlat() {
		return meridionalAxis.getDValue(0);
	}

	/**
	 * @return 'Zonal' (rotated lon-direction) grid spacing (in degrees), 0 if
	 *         non-regular
	 */
	public double getDlon() {
		return zonalAxis.getDValue(0);
	}

	/**
	 * @return Latitude of the lower left grid point of the total domain (in
	 *         degrees, north>0, rotated coordinates)
	 */
	public double getStartlat_tot() {
		return meridionalAxis.values[0];
	}

	/**
	 * @return Longitude of the lower left grid point of the total domain (in
	 *         degrees, east > 0, rotated coordinates)
	 */
	public double getStartlon_tot() {
		return zonalAxis.values[0];
	}

	/**
	 * @return
	 */
	public int getIe_tot() {
		return zonalAxis.getLength();
	}

	/**
	 * @return
	 */
	public int getJe_tot() {
		return meridionalAxis.getLength();
	}

	/**
	 * @return Number of gridpoints of the total domain in vertical direction
	 */
	public int getKe_tot() {
		return verticalDimension.getLength();
	}

	/**
	 * @return Geographical longitude of the rotated north pole
	 */
	public double getPollon() {
		return rotpol.getNorthPole().x;
	}

	/**
	 * @return Geographical latitude of the rotated north pole
	 */
	public double getPollat() {
		return rotpol.getNorthPole().y;
	}

	/**
	 * @return
	 * @throws InvalidRangeException
	 */
	public double getRLat(int j) throws InvalidRangeException {
		if (j >= getJe_tot() || j < 0) {
			throw new InvalidRangeException("j out of range");
		}
		return meridionalAxis.values[j];
	}

	/**
	 * @return
	 * @throws InvalidRangeException
	 */
	public double getRLon(int i) throws InvalidRangeException {
		if (i >= getIe_tot() || i < 0) {
			throw new InvalidRangeException("i out of range");
		}
		return zonalAxis.values[i];
	}

	// CHECK!!!
	public int getRLonIndex(double lon) throws InvalidRangeException {
		// if (lon<getStartlon_tot() || lon>getRLon(getIe_tot()-1)) {
		// throw new InvalidRangeException("lon out of range");
		// }
		return zonalAxis.getIndexOf(lon);
	}

	public int getRLatIndex(double lat) throws InvalidRangeException {
		// if (lat<getStartlat_tot() || lat>getRLat(getJe_tot()-1)) {
		// throw new InvalidRangeException("lat out of range");
		// }
		return meridionalAxis.getIndexOf(lat);
	}

	// protected int indexSearch(int start, int end, double value, WritableAxis
	// axis) {
	// if (start == end) {
	// return start;
	// }
	//		
	// int i2 = (end-start)/2;
	// if (value<= axis.values[i2]) {
	// return indexSearch(start, i2, value, axis);
	// } else {
	// return indexSearch(i2, end, value, axis);
	// }
	// }

	/**
	 * @return
	 * @throws InvalidRangeException
	 */
	public double getArea(int rlati) throws InvalidRangeException {
		if (rlati >= getJe_tot() || rlati < 0) {
			throw new InvalidRangeException("rlati out of range");
		}
		Index ind = area.getIndex();
		return area.get(ind.set(rlati));
	}

	/**
	 * @return
	 * @throws InvalidRangeException
	 */
	public double getLat(int j, int i) throws InvalidRangeException {
		if (j >= getJe_tot() || j < 0) {
			throw new InvalidRangeException("j out of range");
		}
		if (i >= getIe_tot() || i < 0) {
			throw new InvalidRangeException("i out of range");
		}
		Index ind = lat.getIndex();
		return lat.get(ind.set(j, i));
	}

	/**
	 * @return
	 * @throws InvalidRangeException
	 */
	public double getLon(int j, int i) throws InvalidRangeException {
		if (j >= getJe_tot() || j < 0) {
			throw new InvalidRangeException("j out of range");
		}
		if (i >= getIe_tot() || i < 0) {
			throw new InvalidRangeException("i out of range");
		}
		Index ind = lon.getIndex();
		return lon.get(ind.set(j, i));
	}
}
