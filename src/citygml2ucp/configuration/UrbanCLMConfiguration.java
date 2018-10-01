/**
 * 
 */
package citygml2ucp.configuration;

import java.util.LinkedList;
import java.util.List;

import citygml2ucp.tools.WritableAxis;
import citygml2ucp.tools.WritableDimension;
import citygml2ucp.tools.WritableField;
import citygml2ucp.tools.WritableFieldDouble;
import citygml2ucp.tools.WritableFieldInt;
import ucar.ma2.Index;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;

/**
 * Data for a run of CCLM with urban model.
 * 
 * @author Sebastian Schubert
 * 
 */
public class UrbanCLMConfiguration extends CLMConfiguration {

	/**
	 * Dimension for street directions
	 */
	protected WritableAxis ang_udir;

	/**
	 * Dimension for urban classes
	 */
	protected WritableDimension n_uclass;

	/**
	 * Number of height levels in urban scheme for different urban classes
	 */
	protected WritableFieldInt ke_uhl;

	/**
	 * maximum of ke_uhl, used to initialize arrays (so space is wasted, but
	 * only NetCDF 4 natively supports ragged arrays, java library not at the
	 * moment, though)
	 */
	protected int ke_urbanmax;

	/**
	 * Dimension for the urban height
	 */
	protected WritableAxis height, height1, heightsend;
	protected double[] heighta;

	/**
	 * Probability to have a building on a certain height level
	 */
	protected WritableField fr_roof;

	/**
	 * Probability to have a building in a grid cell
	 */
	protected WritableField fr_build;

	/**
	 * Street width
	 */
	protected WritableField w_street;

	/**
	 * Urban fraction of a grid cell
	 */
	protected WritableField fr_urb;

	/**
	 * Width of a building
	 */
	protected WritableField w_build;

	/**
	 * Fraction of street direction in a grid cell
	 */
	protected WritableField fr_udir;

	/**
	 * Fraction of an urban class in a grid cell
	 */
	protected WritableField fr_uclass;

	/**
	 * Skyview factor from ground to a wall of adjacent street canyon
	 */
	protected WritableField fgow;

	/**
	 * Skyview factor from ground to the sky of two canyons
	 */
	protected WritableField fgos;

	/**
	 * Skyview factor from wall to wall of adjacent canyon
	 */
	protected WritableField fwow;

	/**
	 * Skyview factor from wall to sky of two adjacent canyons
	 */
	protected WritableField fwos;

	/**
	 * Sum of areas in a street (used for normalization).
	 */
	private double streetSurfaceSum[][][][];

	/**
	 * Street length as a function of latitude in rotated system
	 */
	public WritableField streetLength;

	/**
	 * Maximum height of buildings
	 */
	public double maxHeight = -Double.MAX_VALUE;
	/**
	 * Minimum height of buildings
	 */
	public double minHeight = Double.MAX_VALUE;

	private boolean justClasses;

	private void initalizeUrbanFields(int n_uclass, double[] ang_udir,
			int[] ke_uhl, double[] height, boolean justClasses) {

		this.justClasses = justClasses;

		if (n_uclass < 1) {
			throw new IllegalArgumentException("n_uclass must be positive");
		}
		this.n_uclass = new WritableDimension("uclass", n_uclass);
		addToWrite(this.n_uclass);

		List<Dimension> ldim2 = new LinkedList<Dimension>();
		ldim2.add(this.n_uclass);

		if (!(ke_uhl.length == n_uclass)) {
			throw new IllegalArgumentException(
					"ke_uhl must be of length n_uclass");
		}
		this.ke_uhl = new WritableFieldInt("ke_uhl", ldim2,
				"urban_heigth_levels", "number of urban height levels", "", "");
		for (int i = 0; i < n_uclass; i++) {
			setKe_urban(i, ke_uhl[i]);
		}
		addToWrite(this.ke_uhl);

		// find max of ke_uhl:
		int maxv = ke_uhl[0];
		for (int i = 0; i < ke_uhl.length; i++) {
			if (ke_uhl[i] < 1) {
				throw new IllegalArgumentException(
						"all ke_uhl elements must be positive.");
			}
			if (ke_uhl[i] > maxv) {
				maxv = ke_uhl[i];
			}
		}

		ke_urbanmax = maxv;

		// height
		this.heighta = height;
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

		this.height1 = new WritableAxis("uheight1", ke_urbanmax, "Z",
				"urban_height_half_levels", "height above surface for half levels", "m",
				height);
		addToWrite(this.height1);

		if (ang_udir[0] < -90. || ang_udir[ang_udir.length - 1] > 90.)
			throw new IllegalArgumentException(
					"smallest street direction must be larger than -90 deg");
		for (int i = 0; i < ang_udir.length - 1; i++) {
			if (ang_udir[i + 1] < ang_udir[i]) {
				throw new IllegalArgumentException(
						"angle_udir must increase with index.");
			}
		}
		this.ang_udir = new WritableAxis("angle_udir", ang_udir.length, "",
				"degree_street", "degree of street direction", "degrees",
				ang_udir, 180.);
		addToWrite(this.ang_udir);

		List<Dimension> ldim = new LinkedList<Dimension>();
		ldim.add(this.meridionalAxis);
		ldim.add(this.zonalAxis);
		// ldim is now latdim, londim

		// impervious surface fraction
		this.fr_urb = new WritableFieldDouble("FR_URB", ldim,
				"urban_fraction", "fraction of urban surfaces",
				"1", "rotated_pole");
		addToWrite(this.fr_urb);

		ldim.add(0, this.n_uclass);
		// ldim is now nucdim, latdim, londim

		this.fr_uclass = new WritableFieldDouble("FR_UCLASS", ldim,
				"urban_classes_fraction", "urban classes fraction", "1",
				"rotated_pole");
		addToWrite(this.fr_uclass);

		// building fraction
		this.fr_build = new WritableFieldDouble("FR_BUILD", ldim,
				"building_fraction",
				"fraction of building surface", "1",
				"rotated_pole");
		if (!justClasses)
			addToWrite(this.fr_build);

		ldim.add(1, this.ang_udir);
		// ldim is now nucdim, angle_udir, latdim, londim

		this.fr_udir = new WritableFieldDouble("FR_UDIR", ldim,
				"fraction_street_direction", "fraction of street directions", "1", "rotated_pole");
		addToWrite(fr_udir);

		this.streetLength = new WritableFieldDouble("STREET_LGT", ldim.subList(
				1, 3), "Street Length", "average street length", "km",
				"rotated_pole");
		addToWrite(streetLength);
		calculateStreetLength();

		// street width
		this.w_street = new WritableFieldDouble("W_STREET", ldim,
				"street_width", "street width", "m",
				"rotated_pole");
		addToWrite(this.w_street);

		// building width
		this.w_build = new WritableFieldDouble("W_BUILD", ldim,
				"building_width", "building width", "m",
				"rotated_pole");
		addToWrite(this.w_build);

		ldim.add(2, this.height1);
		// dims is now nucdim, angle_udir, zdim, latdim, londim

		// building probability
		this.fr_roof = new WritableFieldDouble("FR_ROOF", ldim,
				"building_height_fraction",
				"building height fraction", "1",
				"rotated_pole");
		addToWrite(this.fr_roof);

		streetSurfaceSum = new double[getNuclasses()][getNstreedir()][getJe_tot()][getIe_tot()];

	}

	/**
	 * Create fields necessary for skyview factor calculation.
	 */
	public void initalizeSVFFields() {

		double[] heightWalls = new double[ke_urbanmax - 1];
		for (int i = 0; i < heightWalls.length; i++) {
			heightWalls[i] = 0.5 * (heighta[i] + heighta[i + 1]);
		}
		this.height = new WritableAxis("uheight", ke_urbanmax - 1, "Z",
				"urban_height_walls", "height above surface for walls", "",
				heightWalls);
		addToWrite(this.height);

		this.heightsend = new WritableAxis("uheights", ke_urbanmax - 1, "Z",
				"urban_height_sending_walls",
				"height above surface for radiation sending walls", "",
				heightWalls);
		addToWrite(this.heightsend);

		List<Dimension> ldim = new LinkedList<Dimension>();
		ldim.add(this.n_uclass);
		ldim.add(this.ang_udir);
		ldim.add(this.height1);
		ldim.add(this.meridionalAxis);
		ldim.add(this.zonalAxis);
		// dims is now nucdim, angle_udir, zdim, latdim, londim

		fgos = new WritableFieldDouble("FGOS", ldim, "SVF_ground2othersky",
				"skyview factor from ground to sky with building in beetween",
				"1", "rotated_pole");
		addToWrite(this.fgos);

		ldim.add(2, this.height);
		// dims is now nucdim, angle_udir, zdimwall, zdim, latdim, londim

		fgow = new WritableFieldDouble("FGOW", ldim, "SVF_ground2otherwall",
				"skyview factor from ground to wall of other canyon", "1",
				"rotated_pole");
		addToWrite(this.fgow);

		ldim.add(4, this.heightsend);
		// dims is now nucdim, angle_udir, zdimwall, zdim, zdimwallsend, latdim,
		// londim

		fwow = new WritableFieldDouble("FWOW", ldim, "SVF_wall2otherwall",
				"skyview factor from wall to wall of other canyon", "1",
				"rotated_pole");
		addToWrite(this.fwow);

		ldim.remove(2);
		// dims is now nucdim, angle_udir, zdim, zdimwallsend, latdim, londim
		fwos = new WritableFieldDouble("FWOS", ldim, "SVF_wall2othersky",
				"skyview factor from wall to sky of two canyons", "1",
				"rotated_pole");
		addToWrite(this.fwos);

//		set to missing value everywhere
		for (int i = 0; i < getIe_tot(); i++) {
			for (int j = 0; j < getJe_tot(); j++) {
				for (int iurb = 0; iurb < getNuclasses(); iurb++) {
					for (int id = 0; id < getNstreedir(); id++) {
						Index ind = this.fgos.getIndex();
						for (int o = 0; o < getKe_urbanMax(); o++) {
							ind.set(iurb, id, o, j, i);
							this.fgos.set(ind, this.fgos.missingValue);
						}
						
						ind = this.fgow.getIndex();
						for (int k = 0; k < getKe_urbanMax(); k++) {
							for (int k2 = 0; k2 < getKe_urbanMax()-1; k2++) {
								ind.set(iurb, id, k2, k, j, i);
								this.fgow.set(ind, fgow.missingValue);
							}
						}
						
						ind = this.fwos.getIndex();
						for (int k = 0; k < getKe_urbanMax()-1; k++) {
							for (int l = 0; l < getKe_urbanMax(); l++) {
								ind.set(iurb, id, l, k, j, i);
								this.fwos.set(ind, fwos.missingValue);
							}
						}
					
						ind = this.fwow.getIndex();
						for (int k = 0; k < getKe_urbanMax()-1; k++) {
							for (int l = 0; l < getKe_urbanMax(); l++) {
								for (int m = 0; m < getKe_urbanMax()-1; m++) {
									ind.set(iurb, id, m, l, k, j, i);
									this.fwow.set(ind, fwow.missingValue);
								}
							}
						}
						
					}
				}
			}
		}
				
	}

	/**
	 * Calculate the average distance in a grid cell with a given angle.
	 * 
	 * The grid cell is assumed to be rectangular.
	 */
	private void calculateStreetLength() {

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

	public UrbanCLMConfiguration() throws IllegalArgumentException {
		super();
		initalizeUrbanFields(1, new double[] { -45., 0., 45., 90., },
				new int[] { 10 }, new double[] { 0., 3., 7., 10., 13., 19.,
						25., 30., 38., 45. }, false);
	}

	public UrbanCLMConfiguration(double pollat, double pollon, double dlat,
			double dlon, double startlat_tot, double startlon_tot, int ie_tot,
			int je_tot, int nuclasses, double[] streetdir, int[] ke_urban,
			double[] height, boolean justClasses)
			throws IllegalArgumentException {
		super(pollat, pollon, dlat, dlon, startlat_tot, startlon_tot, ie_tot,
				je_tot);
		initalizeUrbanFields(nuclasses, streetdir, ke_urban, height,
				justClasses);
	}

	public UrbanCLMConfiguration(double pollat, double pollon, double dlat,
			double dlon, double startlat_tot, double startlon_tot, int ie_tot,
			int je_tot, int nuclasses, double[] streetdir, int[] ke_urban,
			double[] height, boolean justClasses, List<String> confItems,
			List<String> confValues) throws IllegalArgumentException {
		super(pollat, pollon, dlat, dlon, startlat_tot, startlon_tot, ie_tot,
				je_tot, confItems, confValues);
		initalizeUrbanFields(nuclasses, streetdir, ke_urban, height,
				justClasses);
	}

	
	public int getNuclasses() {
		return n_uclass.getLength();
	}

	public int getNstreedir() {
		return ang_udir.getLength();
	}

	public void setBuildProb(int uc, int sd, int lev, LatLonPoint llp,
			double value) {
		ProjectionPoint pp = rotpol.latLonToProj(llp);
		setBuildProb(uc, sd, lev, getRLatIndex(pp.getY()), getRLonIndex(pp
				.getX()), value);
	}

	public void incBuildProb(int uc, int sd, int heighti, int rlati, int rloni,
			double value) {
		setBuildProb(uc, sd, heighti, rlati, rloni, getBuildProb(uc, sd,
				heighti, rlati, rloni)
				+ value);
	}

	public void setUrbanFrac(int lat, int lon, double value) {
		if (lat >= getJe_tot() || lat < 0) {
			throw new IllegalArgumentException("lat not in range");
		}
		if (lon >= getIe_tot() || lon < 0) {
			throw new IllegalArgumentException("lon not in range");
		}

		Index ind = fr_urb.getIndex();
		fr_urb.set(ind.set(lat, lon), value);
	}

	public double getUrbanFrac(int lat, int lon) {
		Index ind = fr_urb.getIndex();
		return fr_urb.get(ind.set(lat, lon));
	}

	public double getUrbanClassFrac(int uc, int lat, int lon) {
		Index ind = fr_uclass.getIndex();
		return fr_uclass.get(ind.set(uc, lat, lon));
	}

	public void setUrbanClassFrac(int uc, int lat, int lon, double val) {
		Index ind = fr_uclass.getIndex();
		fr_uclass.set(ind.set(uc, lat, lon), val);
	}

	/**
	 * Only one urban class so far, so set the fraction of this one class to 1.
	 */
	public void fakeUrbanClassFrac() {
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					setUrbanClassFrac(uc, lat, lon, 1.);
				}
			}
		}
	}

	/**
	 * Calculate the building width from A_B/A_S = B/W .
	 */
	public void calculateBuildingWidth() {
		Index index = w_build.getIndex();
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int dir = 0; dir < getNstreedir(); dir++) {
				for (int lat = 0; lat < getJe_tot(); lat++) {
					for (int lon = 0; lon < getIe_tot(); lon++) {
						index.set(uc, dir, lat, lon);
						double bfrac = getBuildingFrac(uc, lat, lon);
						if (bfrac > 1.e-12) {
							w_build
									.set(index, bfrac
											/ (getUrbanFrac(lat, lon)
													* getUrbanClassFrac(uc,
															lat, lon) - bfrac)
											* getStreetWidth(uc, dir, lat, lon));
						} else {
							w_build.set(index, 0.);
						}
					}
				}
			}
		}
	}

	public double getBuildingWidth(int uc, int dir, int lat, int lon) {
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

		Index ind = w_build.getIndex();
		return w_build.get(ind.set(uc, dir, lat, lon));
	}

	public double getStreetFrac(int uc, int id, int lat, int lon) {
		Index ind = fr_udir.getIndex();
		return fr_udir.get(ind.set(uc, id, lat, lon));
	}

	public void setStreetFrac(int uc, int sd, int rlati, int rloni, double value) {
		if (uc >= getNuclasses() || uc < 0) {
			throw new IllegalArgumentException("uc not in range");
		}
		if (rlati >= getJe_tot() || rlati < 0) {
			throw new IllegalArgumentException("lat not in range");
		}
		if (rloni >= getIe_tot() || rloni < 0) {
			throw new IllegalArgumentException("lon not in range");
		}
		Index ind = fr_udir.getIndex();
		fr_udir.set(ind.set(uc, sd, rlati, rloni), value);
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
		Index ind = fr_roof.getIndex();
		fr_roof.set(ind.set(uc, sd, lev, rlati, rloni), value);
	}

	public double getBuildProb(int uc, int sd, int lev, LatLonPoint llp) {
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

		Index ind = fr_roof.getIndex();
		return fr_roof.get(ind.set(uc, sd, lev, rlati, rloni));
	}

	public double getBuildingFrac(int uc, int irlat, int irlon) {
		Index ind = fr_build.getIndex();
		return fr_build.get(ind.set(uc, irlat, irlon));
	}

	public void setBuildingFrac(int uc, int lat, int lon, double value) {
		if (uc >= getNuclasses() || uc < 0) {
			throw new IllegalArgumentException("uc not in range");
		}
		if (lat >= getJe_tot() || lat < 0) {
			throw new IllegalArgumentException("lat not in range");
		}
		if (lon >= getIe_tot() || lon < 0) {
			throw new IllegalArgumentException("lon not in range");
		}

		Index ind = fr_build.getIndex();
		fr_build.set(ind.set(uc, lat, lon), value);
	}

	public void setBuildingWidth(int uc, int dir, int lat, int lon, double value) {
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

		Index ind = w_build.getIndex();
		w_build.set(ind.set(uc, dir, lat, lon), value);
	}

	public void incBuildingFrac(int uc, int irlat, int irlon, double incr) {
		Index ind = fr_build.getIndex();
		ind.set(uc, irlat, irlon);
		fr_build.set(ind, fr_build.get(ind) + incr);
	}

	/**
	 * Normalize the area of buildings to the cell size.
	 */
	public void normBuildingFrac() {
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					setBuildingFrac(uc, lat, lon, getBuildingFrac(uc, lat, lon)
							/ getArea(lat));
				}
			}
		}
	}

	/**
	 * Normalize the street width which has been weightes with respective wall
	 * surface.
	 */
	public void normStreetWidth() {
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

		Index ind = w_street.getIndex();
		w_street.set(ind.set(uc, dir, lat, lon), getStreetWidth(uc, dir,
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

		Index ind = w_street.getIndex();
		w_street.set(ind.set(uc, dir, lat, lon), value);
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

		Index ind = w_street.getIndex();
		return w_street.get(ind.set(uc, dir, lat, lon));
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
		return ang_udir.getValue(sd);
	}

	/**
	 * Normalize the building probability for one street direction and calculate
	 * the fraction of the street direction in a grid cell.
	 */
	public void normBuildProbAndCalcStreetFraction() {
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					double sum = 0.;
					double[] sumStreet = new double[getNstreedir()];
					for (int sd = 0; sd < getNstreedir(); sd++) {
						sumStreet[sd] = 0.;
						for (int lev = 0; lev < getKe_urban(uc); lev++) {
							sumStreet[sd] += getBuildProb(uc, sd, lev, lat, lon);
						}
						sum += sumStreet[sd];
					}
					if (sum > 1e-14) {
						sum = 1. / sum;
						for (int sd = 0; sd < getNstreedir(); sd++) {
							if (sumStreet[sd] > 1.e-12) {
								for (int lev = 0; lev < getKe_urban(uc); lev++) {
									setBuildProb(uc, sd, lev, lat, lon,
											getBuildProb(uc, sd, lev, lat, lon)
													/ sumStreet[sd]);
								}
							} else {
								for (int lev = 0; lev < getKe_urban(uc); lev++) {
									setBuildProb(uc, sd, lev, lat, lon, 0.);
								}
							}

							for (int lev = getKe_urban(uc); lev < ke_urbanmax; lev++) {
								setBuildProb(uc, sd, lev, lat, lon, 0.);
							}

							setStreetFrac(uc, sd, lat, lon, sum * sumStreet[sd]);
						}
					} else {
						for (int sd = 0; sd < getNstreedir(); sd++) {
							for (int lev = 0; lev < ke_urbanmax; lev++) {
								setBuildProb(uc, sd, lev, lat, lon, 0.);
							}
							setStreetFrac(uc, sd, lat, lon, 0.);
						}
					}
				}
			}
		}
	}

	/**
	 * Find the highest index for every grid cell for which the summed probability over that level
	 * is smaller than {@code ignoreBP}, set height parameters
	 * correspondingly and reduce output size.
	 * 
	 * @param ignoredBP
	 *            Percentage of ignored buildings
	 */
	public void reduceHeight(double ignoredBP) {

		int[] maxLength = new int[getNuclasses()];
		// get the height above which buildings can be ignored
		for (int uc = 0; uc < getNuclasses(); uc++) {
			maxLength[uc] = 0;
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					if (getBuildingFrac(uc, lat, lon) > 1.e-12) {
						for (int sd = 0; sd < getNstreedir(); sd++) {
							int localeMaxLength = getKe_urban(uc);
							double ignored = 0;
							for (int lev = getKe_urban(uc) - 1; lev >= 0.; lev--) {
								ignored += getBuildProb(uc, sd, lev, lat, lon);
								if (ignored < ignoredBP)
									localeMaxLength--;
							}
							double sum = 0.;
							for (int lev = 0; lev < localeMaxLength; lev++) {
								sum += getBuildProb(uc, sd, lev, lat, lon);
							}
							if (sum > 1.e-14) {
								sum = 1. / sum;
								for (int lev = 0; lev < localeMaxLength; lev++) {
									setBuildProb(uc, sd, lev, lat, lon,
											getBuildProb(uc, sd, lev, lat, lon)
													* sum);
								}
							}
							for (int lev = localeMaxLength; lev < getKe_urban(uc); lev++) {
								setBuildProb(uc, sd, lev, lat, lon, 0.);
							}
							if (localeMaxLength > maxLength[uc])
								maxLength[uc] = localeMaxLength;
						}
					}
				}
			}
		}

		int maxmaxLength = 0;
		for (int uc = 0; uc < getNuclasses(); uc++) {
			if (maxmaxLength < maxLength[uc]) {
				maxmaxLength = maxLength[uc];
			}
		}

		
		for (int uc = 0; uc < getNuclasses(); uc++) {
			setKe_urban(uc, maxLength[uc]);
		}
		ke_urbanmax = maxmaxLength;
		System.out.println("Height reduced to " + ke_urbanmax + " levels.");

		height1.setLength(maxmaxLength);

		fr_roof.resetDim();

	}

	/**
	 * Set fields to the missing value where not defined.
	 * 
	 * Building and urban fraction > 1.e-12 define an urban cell.
	 */
	public void defineMissingData() {
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					boolean setUndef = true;
					for (int id = 0; id < getNstreedir(); id++) {
						if (getStreetFrac(uc, id, lat, lon) > 1.e-10) {
							setUndef = false;
						}
					}
					if (justClasses) {
						setUndef = (getUrbanFrac(lat, lon) < 1.e-12)
								|| setUndef;
					} else {
						setUndef = (getBuildingFrac(uc, lat, lon) < 1.e-12)
								|| (getUrbanFrac(lat, lon) < 1.e-12)
								|| setUndef;
					}
					if (setUndef) {
						for (int sd = 0; sd < getNstreedir(); sd++) {
							setBuildingWidth(uc, sd, lat, lon,
									w_build.missingValue);
							setStreetFrac(uc, sd, lat, lon,
									fr_udir.missingValue);
							setStreetWidth(uc, sd, lat, lon,
									fr_udir.missingValue);
							setUrbanClassFrac(uc, lat, lon,
									fr_uclass.missingValue);
							for (int h = 0; h < ke_urbanmax; h++) {
								setBuildProb(uc, sd, h, lat, lon,
										fr_roof.missingValue);
							}
						}
						setUrbanFrac(lat, lon, fr_urb.missingValue);
						setBuildingFrac(uc, lat, lon, fr_build.missingValue);
					}
				}
			}
		}
	}

	/**
	 * Set svf fields to the missing value where not defined.
	 * 
	 * Building and urban fraction > 1.e-12 define an urban cell.
	 */
	public void defineMissingDataSVF() {
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					if (getBuildingFrac(uc, lat, lon) < 1.e-12
							|| getUrbanFrac(lat, lon) < 1.e-12) {
						for (int sd = 0; sd < getNstreedir(); sd++) {
							for (int h = 0; h < ke_urbanmax; h++) {
								Index index = fgos.getIndex();
								fgos.set(index.set(uc, sd, h, lat, lon),
										fgos.missingValue);
								for (int h2 = 0; h2 < ke_urbanmax - 1; h2++) {
									index = fgow.getIndex();
									fgow.set(
											index.set(uc, sd, h2, h, lat, lon),
											fgow.missingValue);
									index = fwos.getIndex();
									fwos.set(
											index.set(uc, sd, h, h2, lat, lon),
											fwos.missingValue);
									index = fwow.getIndex();
									for (int h3 = 0; h3 < ke_urbanmax - 1; h3++) {
										fwow.set(index.set(uc, sd, h2, h, h3,
												lat, lon), fwow.missingValue);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public int getKe_urbanMax() {
		return ke_urbanmax;
	}

	/**
	 * Get the highest level with buildings present
	 * @param iurb Urban class
	 * @param id Street direction
	 * @param jindex Lat index
	 * @param iindex Lon index
	 * @return Height index
	 */
	public int getLocalKe_urbanMax(int iurb, int id, int jindex, int iindex) {
		int max = getKe_urbanMax()-1;
		while (getBuildProb(iurb, id, max, jindex, iindex)<1.e-12) {
			max--;
		}
		return max+1;
	}
	
	public int getKe_urban(int uc) {
		Index ind = ke_uhl.getIndex();
		return ke_uhl.getInt(ind.set(uc));
	}

	public void setKe_urban(int uc, int ke) {
		Index ind = ke_uhl.getIndex();
		ke_uhl.setInt(ind.set(uc), ke);
	}

	public double[] getHeightA() {
		return this.heighta;
	}

	public int getHeightIndex(double height) {
		return this.height1.getIndexOf(height);
	}

	public int getStreetdirIndex(double angle) {
		return ang_udir.getIndexOf(angle);
	}

	public void setFgow(int uc, int id, int j, int i, double[][] fgow) {
		Index ind = this.fgow.getIndex();
		for (int k = 0; k < fgow.length; k++) {
			for (int k2 = 0; k2 < fgow[k].length; k2++) {
				ind.set(uc, id, k2, k, j, i);
				this.fgow.set(ind, fgow[k][k2]);
			}
		}
	}

	public void setFgos(int uc, int id, int j, int i, double[] fgs) {
		Index ind = this.fgos.getIndex();
		for (int k = 0; k < fgs.length; k++) {
			ind.set(uc, id, k, j, i);
			this.fgos.set(ind, fgs[k]);
		}
	}

	public void setFwow(int uc, int id, int j, int i, double[][][] fww) {
		Index ind = this.fwow.getIndex();
		for (int k = 0; k < fww.length; k++) {
			for (int l = 0; l < fww[k].length; l++) {
				for (int m = 0; m < fww[k][l].length; m++) {
					ind.set(uc, id, m, l, k, j, i);
					this.fwow.set(ind, fww[m][l][k]);
				}
			}
		}
	}

	public void setFwos(int uc, int id, int j, int i, double[][] fws) {
		Index ind = this.fwos.getIndex();
		for (int k = 0; k < fws.length; k++) {
			for (int l = 0; l < fws[k].length; l++) {
				ind.set(uc, id, l, k, j, i);
				this.fwos.set(ind, fws[k][l]);
			}
		}
	}

}
