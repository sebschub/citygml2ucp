/**
 * 
 */
package pik.clminputdata.configuration;

import java.util.LinkedList;
import java.util.List;

import pik.clminputdata.tools.WritableAxis;
import pik.clminputdata.tools.WritableDimension;
import pik.clminputdata.tools.WritableField;
import pik.clminputdata.tools.WritableFieldInt;

import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;

/**
 * @author Sebastian Schubert
 * 
 */
public class UrbanCLMConfiguration extends CLMConfiguration {

	protected WritableAxis streetdir;

	/**
	 * number of urban classes
	 */
	// public final int nuclasses;
	protected WritableDimension nuclasses;

	/**
	 * number of height levels in urban scheme
	 */
	protected WritableFieldInt ke_urban;

	protected static int ke_urbanmax;

	/**
	 * maximum of ke_urban, used to initialize arrays (so space is wasted, but
	 * only netcdf 4 natively supports ragged arrays, java does, though)
	 */
	// public final int ke_urban_max;
	protected WritableAxis height;

	// /**
	// * height of urban levels
	// */
	// public final double[] height;
	// /**
	// * String used in NetCDF file for height
	// */
	// public final static String heightString = "height";

	/**
	 * probability to have a building
	 */
	protected WritableField buildProb;
	// /**
	// * String used in NetCDF file for buildprob
	// */
	// public final static String buildprobString = "BUILD_PROP";

	/**
	 * urban fraction
	 */
	protected WritableField buildingFrac;
	// /**
	// * String used in NetCDF file for urban_frac
	// */
	// public final static String urban_fracString = "FR_URBAN";

	/**
	 * street width
	 */
	protected WritableField streetWidth;

	private double streetSurfaceSum[][][][];

	public WritableField streetLength;

	public double maxHeight = -Double.MAX_VALUE;
	public double minHeight = Double.MAX_VALUE;

	// /**
	// * String used in NetCDF file for urban_frac
	// */
	// public final static String street_widthString = "STREET_WIDTH";

	private void initalizeUrbanFields(int nuclasses, double[] streetdir,
			int[] ke_urban, double[] height) throws InvalidRangeException {
		if (nuclasses < 1) {
			throw new IllegalArgumentException("nuclasses must be positive");
		}
		this.nuclasses = new WritableDimension("nuc", nuclasses);
		toWrite.add(this.nuclasses);

		List<Dimension> ldim2 = new LinkedList<Dimension>();
		ldim2.add(this.nuclasses);

		if (!(ke_urban.length == nuclasses)) {
			throw new IllegalArgumentException(
					"ke_urban must be of length nuclasses");
		}
		this.ke_urban = new WritableFieldInt("ke_urban", ldim2,
				"urban_heigth_levels", "number of urban height levels", "", "");
		Index ind = this.ke_urban.getIndex();
		for (int i = 0; i < nuclasses; i++) {
			this.ke_urban.set(ind, ke_urban[i]);
		}
		toWrite.add(this.ke_urban);

		// find max of ke_urban:
		int maxv = ke_urban[0];
		for (int i = 0; i < ke_urban.length; i++) {
			if (ke_urban[i] < 1) {
				throw new IllegalArgumentException(
						"all ke_urban elements must be positive.");
			}
			if (ke_urban[i] > maxv) {
				maxv = ke_urban[i];
			}
		}

		ke_urbanmax = maxv;

		// height
		if (height.length != ke_urbanmax) {
			throw new IllegalArgumentException(
					"height must be given for maximal number of urban level.");
		}
		if (height[0] != 0.f) {
			throw new IllegalArgumentException("height[0] must be 0.");
		}
		for (int i = 0; i < height.length - 1; i++) {
			if (height[i + 1] < height[i]) {
				throw new IllegalArgumentException(
						"height must increase with index.");
			}
		}
		this.height = new WritableAxis("level_urban", ke_urbanmax,
				"height_urban", "Z", "urban_height", "height above surface",
				"", height);
		toWrite.add(this.height);

		if (streetdir[0] < -90. || streetdir[streetdir.length - 1] > 90.)
			throw new IllegalArgumentException(
					"smalles street direction must be larger than -90 deg");
		for (int i = 0; i < streetdir.length - 1; i++) {
			if (streetdir[i + 1] < streetdir[i]) {
				throw new IllegalArgumentException(
						"streetdir must increase with index.");
			}
		}
		this.streetdir = new WritableAxis("street_dir", streetdir.length,
				"degree_street", "", "degree_street",
				"degree of street direction", "degrees", streetdir, 180.);
		toWrite.add(this.streetdir);

		List<Dimension> ldim = new LinkedList<Dimension>();
		ldim.add(this.meridionalAxis);
		ldim.add(this.zonalAxis);
		// ldim is now latdim, londim

		ldim.add(0, this.nuclasses);
		// ldim is now nucdim, latdim, londim

		// urban fraction
		this.buildingFrac = new WritableField("FR_BUILD", ldim,
				"building_fraction",
				"fraction of building surface in grid cell", "1",
				"rotated_pole");
		toWrite.add(this.buildingFrac);

		ldim.add(1, this.streetdir);
		// ldim is now nucdim, streetdir, latdim, londim

		this.streetLength = new WritableField("STREET_LENGTH", ldim.subList(1,
				3), "Street Length", "average street length", "km",
				"rotated_pole");
		toWrite.add(streetLength);
		calculateStreetLength();

		// street width
		this.streetWidth = new WritableField("STREET_WIDTH", ldim,
				"street_width", "street width in grid cell", "m",
				"rotated_pole");
		toWrite.add(this.streetWidth);

		ldim.add(2, this.height);
		// dims is now nucdim, streetdir, zdim, latdim, londim

		// building probability
		this.buildProb = new WritableField("BUILD_PROP", ldim,
				"building_probability",
				"probability to have a building at the height", "1",
				"rotated_pole");
		toWrite.add(this.buildProb);

		streetSurfaceSum = new double[getNuclasses()][getNstreedir()][getJe_tot()][getIe_tot()];

	}

	private void calculateStreetLength() throws InvalidRangeException {

		Index ind = streetLength.getIndex();

		double lp = r * toRadians(getDlat());

		for (int lat = 0; lat < getJe_tot(); lat++) {

			double ll = 2 * r * getDlon() / getDlat()
					* sin(0.5 * toRadians(getDlat()))
					* cos(toRadians(getRLat(lat)));

			for (int sd = 0; sd < getNstreedir(); sd++) {

				double a = toRadians(abs(getStreetDir(sd)));

				if (abs(a - Math.PI / 2) < 1e-13) {
					streetLength.set(ind.set(sd, lat), ll);
				} else if (tan(a) < ll / lp) {
					streetLength.set(ind.set(sd, lat), 1 / cos(a) * lp / ll
							* (ll - 0.5 * lp * tan(a)));
				} else {
					streetLength.set(ind.set(sd, lat), 1 / sin(a) * ll / lp
							* (lp - 0.5 * ll / tan(a)));
				}

			}
		}
	}

	public UrbanCLMConfiguration() throws IllegalArgumentException,
			InvalidRangeException {
		super();
		// nuclasses = 1;
		// ke_urban = new int[] { 10 };
		// ke_urban_max = 10;
		// height = new double[] { 0.f, 3.f, 7.f, 10.f, 13.f, 19.f, 25.f, 30.f,
		// 38.f, 45.f };
		initalizeUrbanFields(1, new double[] { -45., 0., 45., 90., },
				new int[] { 10 }, new double[] { 0.f, 3.f, 7.f, 10.f, 13.f,
						19.f, 25.f, 30.f, 38.f, 45.f });
	}

	public UrbanCLMConfiguration(double pollat, double pollon, double dlat,
			double dlon, double startlat_tot, double startlon_tot, int ie_tot,
			int je_tot, int ke_tot, int nuclasses, double[] streetdir,
			int[] ke_urban, double[] height) throws IllegalArgumentException,
			InvalidRangeException {
		super(pollat, pollon, dlat, dlon, startlat_tot, startlon_tot, ie_tot,
				je_tot, ke_tot);
		initalizeUrbanFields(nuclasses, streetdir, ke_urban, height);
	}

	public int getNuclasses() {
		return nuclasses.getLength();
	}

	public int getNstreedir() {
		return streetdir.getLength();
	}

	public void setBuildProb(int uc, int sd, int lev, LatLonPoint llp,
			double value) throws InvalidRangeException {
		ProjectionPoint pp = rotpol.latLonToProj(llp);
		setBuildProb(uc, sd, lev, getRLatIndex(pp.getY()), getRLonIndex(pp
				.getX()), value);
	}

	public void incBuildProb(int uc, int sd, int heighti, int rlati, int rloni,
			double value) throws InvalidRangeException {
		setBuildProb(uc, sd, heighti, rlati, rloni, getBuildProb(uc, sd,
				heighti, rlati, rloni)
				+ value);
	}

	public void setBuildProb(int uc, int sd, int lev, int rlati, int rloni,
			double value) {
		if (uc >= getNuclasses() || uc < 0) {
			throw new IllegalArgumentException("uc not in range");
		}
		if (rlati >= getJe_tot() || rlati < 0) {
			throw new IllegalArgumentException("lat not in range");
		}
		if (rloni >= getIe_tot() || rloni < 0) {
			throw new IllegalArgumentException("lon not in range");
		}
		if (lev >= getKe_urban(uc) || lev < 0) {
			throw new IllegalArgumentException("lev not in range");
		}

		Index ind = buildProb.getIndex();
		buildProb.set(ind.set(uc, sd, lev, rlati, rloni), value);
	}

	public double getBuildProb(int uc, int sd, int lev, LatLonPoint llp)
			throws InvalidRangeException {
		ProjectionPoint pp = rotpol.latLonToProj(llp);
		return getBuildProb(uc, sd, lev, getRLatIndex(pp.getY()),
				getRLonIndex(pp.getX()));
	}

	public double getBuildProb(int uc, int sd, int lev, int rlati, int rloni) {
		if (uc >= getNuclasses() || uc < 0) {
			throw new IllegalArgumentException("uc not in range");
		}
		if (rlati >= getJe_tot() || rlati < 0) {
			throw new IllegalArgumentException("lat not in range");
		}
		if (rloni >= getIe_tot() || rloni < 0) {
			throw new IllegalArgumentException("lon not in range");
		}
		if (lev >= getKe_urban(uc) || lev < 0) {
			throw new IllegalArgumentException("lev not in range");
		}

		Index ind = buildProb.getIndex();
		return buildProb.get(ind.set(uc, sd, lev, rlati, rloni));
	}

	public double getBuildingFrac(int uc, int irlat, int irlon)
			throws InvalidRangeException {
		Index ind = buildingFrac.getIndex();
		return buildingFrac.get(ind.set(uc, irlat, irlon));
	}

	public void incBuildingFrac(int uc, int irlat, int irlon, double incr)
			throws InvalidRangeException {
		Index ind = buildingFrac.getIndex();
		ind.set(uc, irlat, irlon);
		buildingFrac.set(ind, buildingFrac.get(ind) + incr);
	}

	public void normBuildingFrac()
			throws InvalidRangeException {
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					Index ind = buildingFrac.getIndex();
					ind.set(uc, lat, lon);
					
					buildingFrac.set(ind, buildingFrac.get(ind) / getArea(lat));
					
				}
			}
		}
	}

	public void normStreetWidth() throws InvalidRangeException {
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int dir = 0; dir < getNstreedir(); dir++) {
				for (int lat = 0; lat < getJe_tot(); lat++) {
					for (int lon = 0; lon < getIe_tot(); lon++) {
						if (streetSurfaceSum[uc][dir][lat][lon] > 0) {
							setStreetWidth(uc, dir, lat, lon, getStreetWidth(
									uc, dir, lat, lon)
									/ streetSurfaceSum[uc][dir][lat][lon]);
						}
					}
				}
			}
		}
	}

	public void incStreetSurfaceSum(int uc, int dir, int lat, int lon,
			double value) {
		if (uc >= getNuclasses() || uc < 0) {
			throw new IllegalArgumentException("uc not in range");
		}
		if (dir >= getNstreedir() || dir < 0) {
			throw new IllegalArgumentException("dir not in range");
		}
		if (lat >= getJe_tot() || lat < 0) {
			throw new IllegalArgumentException("lat not in range");
		}
		if (lon >= getIe_tot() || lon < 0) {
			throw new IllegalArgumentException("lon not in range");
		}
		streetSurfaceSum[uc][dir][lat][lon] += value;
	}

	// public void incBuildingWidth(int uc, int dir, int lat, int lon, double
	// value) {
	// if (uc >= getNuclasses() || uc < 0) {
	// throw new IllegalArgumentException("uc not in range");
	// }
	// if (dir >= getNstreedir() || dir < 0) {
	// throw new IllegalArgumentException("dir not in range");
	// }
	// if (lat >= getJe_tot() || lat < 0) {
	// throw new IllegalArgumentException("lat not in range");
	// }
	// if (lon >= getIe_tot() || lon < 0) {
	// throw new IllegalArgumentException("lon not in range");
	// }
	//
	// Index ind = buildingWidth.getIndex();
	// buildingWidth.set(ind.set(uc, dir, lat, lon), getBuildingWidth(uc, dir,
	// lat, lon)
	// + value);
	// }

	public void incStreetWidth(int uc, int dir, int lat, int lon, double value) {
		if (uc >= getNuclasses() || uc < 0) {
			throw new IllegalArgumentException("uc not in range");
		}
		if (dir >= getNstreedir() || dir < 0) {
			throw new IllegalArgumentException("dir not in range");
		}
		if (lat >= getJe_tot() || lat < 0) {
			throw new IllegalArgumentException("lat not in range");
		}
		if (lon >= getIe_tot() || lon < 0) {
			throw new IllegalArgumentException("lon not in range");
		}

		Index ind = streetWidth.getIndex();
		streetWidth.set(ind.set(uc, dir, lat, lon), getStreetWidth(uc, dir,
				lat, lon)
				+ value);
	}

	public void setStreetWidth(int uc, int dir, int lat, int lon, double value) {
		if (uc >= getNuclasses() || uc < 0) {
			throw new IllegalArgumentException("uc not in range");
		}
		if (dir >= getNstreedir() || dir < 0) {
			throw new IllegalArgumentException("dir not in range");
		}
		if (lat >= getJe_tot() || lat < 0) {
			throw new IllegalArgumentException("lat not in range");
		}
		if (lon >= getIe_tot() || lon < 0) {
			throw new IllegalArgumentException("lon not in range");
		}

		Index ind = streetWidth.getIndex();
		streetWidth.set(ind.set(uc, dir, lat, lon), value);
	}

	public double getStreetWidth(int uc, int dir, int lat, int lon) {
		if (uc >= getNuclasses() || uc < 0) {
			throw new IllegalArgumentException("uc not in range");
		}
		if (dir >= getNstreedir() || dir < 0) {
			throw new IllegalArgumentException("dir not in range");
		}
		if (lat >= getJe_tot() || lat < 0) {
			throw new IllegalArgumentException("lat not in range");
		}
		if (lon >= getIe_tot() || lon < 0) {
			throw new IllegalArgumentException("lon not in range");
		}

		Index ind = streetWidth.getIndex();
		return streetWidth.get(ind.set(uc, dir, lat, lon));
	}

	public double getStreetLength(int dir, int lat) {
		if (dir >= getNstreedir() || dir < 0) {
			throw new IllegalArgumentException("dir not in range");
		}
		if (lat >= getJe_tot() || lat < 0) {
			throw new IllegalArgumentException("lat not in range");
		}

		Index ind = streetLength.getIndex();
		return streetLength.get(ind.set(dir, lat));
	}

	public double getStreetDir(int sd) {
		if (sd >= getNstreedir() || sd < 0) {
			throw new IllegalArgumentException("dir not in range");
		}
		return streetdir.getValue(sd);
	}

	public void normBuildProb() {
		double sum;
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					sum = 0.;
					for (int sd = 0; sd < getNstreedir(); sd++) {
						for (int lev = 0; lev < getKe_urban(uc); lev++) {
							sum += getBuildProb(uc, sd, lev, lat, lon);
						}
					}
					if (sum > 1e-14) {
						sum = 1. / sum;
						for (int sd = 0; sd < getNstreedir(); sd++) {
							for (int lev = 0; lev < getKe_urban(uc); lev++) {
								setBuildProb(uc, sd, lev, lat, lon, sum
										* getBuildProb(uc, sd, lev, lat, lon));
							}

							for (int lev = getKe_urban(uc); lev < ke_urbanmax; lev++) {
								setBuildProb(uc, sd, lev, lat, lon, 0.);
							}
						}
					} else {
						for (int sd = 0; sd < getNstreedir(); sd++) {
							for (int lev = 0; lev < ke_urbanmax; lev++) {
								setBuildProb(uc, sd, lev, lat, lon, 0.);
							}
						}
					}
				}
			}
		}
	}

	public int getKe_urban(int uc) {
		Index ind = ke_urban.getIndex();
		return ke_urban.getInt(ind.set(uc));
	}

	public int getHeightIndex(double height) throws InvalidRangeException {
		return this.height.getIndexOf(height);
	}

	public int getStreetdirIndex(double angle) throws InvalidRangeException {
		if (angle > 90 || angle < -90) {
			throw new InvalidRangeException("angle not between -90 and 90");
		}
		return streetdir.getIndexOf(angle);
	}

}
