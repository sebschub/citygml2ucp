/**
 * 
 */
package pik.clminputdata.configuration;

import java.util.LinkedList;
import java.util.List;

import pik.clminputdata.tools.WritableAxis;
import pik.clminputdata.tools.WritableDimension;
import pik.clminputdata.tools.WritableField;
import pik.clminputdata.tools.WritableFieldFloat;
import pik.clminputdata.tools.WritableFieldInt;

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
	protected WritableAxis streetdir;

	/**
	 * Dimension for urban classes
	 */
	protected WritableDimension nuclasses;

	/**
	 * Number of height levels in urban scheme for different urban classes
	 */
	protected WritableFieldInt ke_urban;

	/**
	 * maximum of ke_urban, used to initialize arrays (so space is wasted, but
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
	protected WritableField buildProb;

	/**
	 * Probability to have a building in a grid cell
	 */
	protected WritableField buildingFrac;

	/**
	 * Street width
	 */
	protected WritableField streetWidth;

	/**
	 * Urban fraction of a grid cell
	 */
	protected WritableField urbanFrac;

	/**
	 * Width of a building
	 */
	protected WritableField buildingWidth;

	/**
	 * Fraction of street direction in a grid cell
	 */
	protected WritableField streetFrac;

	/**
	 * Fraction of an urban class in a grid cell
	 */
	protected WritableField urbanClassFrac;

	protected WritableField fgow;

	protected WritableField fgs;

	protected WritableField fww;

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

	private void initalizeUrbanFields(int nuclasses, double[] streetdir,
			int[] ke_urban, double[] height) {
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
		for (int i = 0; i < nuclasses; i++) {
			setKe_urban(i, ke_urban[i]);
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
				"urban_height_roofs", "height above surface for roofs", "",
				height);
		toWrite.add(this.height1);

		if (streetdir[0] < -90. || streetdir[streetdir.length - 1] > 90.)
			throw new IllegalArgumentException(
					"smalles street direction must be larger than -90 deg");
		for (int i = 0; i < streetdir.length - 1; i++) {
			if (streetdir[i + 1] < streetdir[i]) {
				throw new IllegalArgumentException(
						"streetdir must increase with index.");
			}
		}
		this.streetdir = new WritableAxis("streetdir", streetdir.length, "",
				"degree_street", "degree of street direction", "degrees",
				streetdir, 180.);
		toWrite.add(this.streetdir);

		List<Dimension> ldim = new LinkedList<Dimension>();
		ldim.add(this.meridionalAxis);
		ldim.add(this.zonalAxis);
		// ldim is now latdim, londim

		// impervious surface fraction
		this.urbanFrac = new WritableFieldFloat("FR_URBAN", ldim,
				"urban_fraction", "fraction of urban surfaces in grid cell",
				"1", "rotated_pole");
		toWrite.add(this.urbanFrac);

		ldim.add(0, this.nuclasses);
		// ldim is now nucdim, latdim, londim

		this.urbanClassFrac = new WritableFieldFloat("FR_URBANCL", ldim,
				"urban_classes_fraction", "urban classes fraction", "1",
				"rotated_pole");
		toWrite.add(this.urbanClassFrac);

		// building fraction
		this.buildingFrac = new WritableFieldFloat("FR_BUILD", ldim,
				"building_fraction",
				"fraction of building surface in grid cell", "1",
				"rotated_pole");
		toWrite.add(this.buildingFrac);

		ldim.add(1, this.streetdir);
		// ldim is now nucdim, streetdir, latdim, londim

		this.streetFrac = new WritableFieldFloat("FR_STREETD", ldim,
				"street_fraction", "street fraction", "1", "rotated_pole");
		toWrite.add(streetFrac);

		this.streetLength = new WritableFieldFloat("STREET_LGT", ldim.subList(
				1, 3), "Street Length", "average street length", "km",
				"rotated_pole");
		toWrite.add(streetLength);
		calculateStreetLength();

		// street width
		this.streetWidth = new WritableFieldFloat("STREET_W", ldim,
				"street_width", "street width in grid cell", "m",
				"rotated_pole");
		toWrite.add(this.streetWidth);

		// building width
		this.buildingWidth = new WritableFieldFloat("BUILD_W", ldim,
				"building_width", "building width in grid cell", "m",
				"rotated_pole");
		toWrite.add(this.buildingWidth);

		ldim.add(2, this.height1);
		// dims is now nucdim, streetdir, zdim, latdim, londim

		// building probability
		this.buildProb = new WritableFieldFloat("BUILD_PROP", ldim,
				"building_probability",
				"probability to have a building at the height", "1",
				"rotated_pole");
		toWrite.add(this.buildProb);

		streetSurfaceSum = new double[getNuclasses()][getNstreedir()][getJe_tot()][getIe_tot()];

	}

	public void initalizeSVFFields() {
		
		double[] heightWalls = new double[ke_urbanmax - 1];
		for (int i = 0; i < heightWalls.length; i++) {
			heightWalls[i] = 0.5 * (heighta[i] + heighta[i + 1]);
		}
		this.height = new WritableAxis("uheight", ke_urbanmax - 1, "Z",
				"urban_height_walls", "height above surface for walls", "",
				heightWalls);
		toWrite.add(this.height);

		this.heightsend = new WritableAxis("uheights", ke_urbanmax - 1, "Z",
				"urban_height_sending_walls",
				"height above surface for radiation sending walls", "",
				heightWalls);
		toWrite.add(this.heightsend);
		
		List<Dimension> ldim = new LinkedList<Dimension>();
		ldim.add(this.nuclasses);
		ldim.add(this.streetdir);
		ldim.add(this.height1);
		ldim.add(this.meridionalAxis);
		ldim.add(this.zonalAxis);
		// dims is now nucdim, streetdir, zdim, latdim, londim
		
		fgs = new WritableFieldFloat("FGS", ldim, "SVF_ground2sky",
				"skyview factor from ground to sky with building in beetween",
				"1", "rotated_pole");
		toWrite.add(this.fgs);
		
		ldim.add(2, this.height);
		// dims is now nucdim, streetdir, zdimwall, zdim, latdim, londim
		
		fgow = new WritableFieldFloat("FGOW", ldim, "SVF_ground2otherwall",
				"skyview factor from ground to wall of other canyon", "1",
				"rotated_pole");
		toWrite.add(this.fgow);

		ldim.add(4, this.heightsend);
		// dims is now nucdim, streetdir, zdimwall, zdim, zdimwallsend, latdim,
		// londim

		fww = new WritableFieldFloat("FWW", ldim, "SVF_wall2wall",
				"skyview factor from wall to wall of other canyon", "1",
				"rotated_pole");
		toWrite.add(this.fww);

	}
	
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
						25., 30., 38., 45. });
	}

	public UrbanCLMConfiguration(double pollat, double pollon, double dlat,
			double dlon, double startlat_tot, double startlon_tot, int ie_tot,
			int je_tot, int nuclasses, double[] streetdir, int[] ke_urban,
			double[] height) throws IllegalArgumentException {
		super(pollat, pollon, dlat, dlon, startlat_tot, startlon_tot, ie_tot,
				je_tot);
		initalizeUrbanFields(nuclasses, streetdir, ke_urban, height);
	}

	public int getNuclasses() {
		return nuclasses.getLength();
	}

	public int getNstreedir() {
		return streetdir.getLength();
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

		Index ind = urbanFrac.getIndex();
		urbanFrac.set(ind.set(lat, lon), value);
	}

	public double getUrbanFrac(int lat, int lon) {
		Index ind = urbanFrac.getIndex();
		return urbanFrac.get(ind.set(lat, lon));
	}

	public double getUrbanClassFrac(int uc, int lat, int lon) {
		Index ind = urbanClassFrac.getIndex();
		return urbanClassFrac.get(ind.set(uc, lat, lon));
	}

	public void setUrbanClassFrac(int uc, int lat, int lon, double val) {
		Index ind = urbanClassFrac.getIndex();
		urbanClassFrac.set(ind.set(uc, lat, lon), val);
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
		Index index = buildingWidth.getIndex();
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int dir = 0; dir < getNstreedir(); dir++) {
				for (int lat = 0; lat < getJe_tot(); lat++) {
					for (int lon = 0; lon < getIe_tot(); lon++) {
						index.set(uc, dir, lat, lon);
						double bfrac = getBuildingFrac(uc, lat, lon);
						if (bfrac > 1.e-12) {
							buildingWidth
									.set(index, bfrac
											/ (getUrbanFrac(lat, lon)
													* getUrbanClassFrac(uc,
															lat, lon) - bfrac)
											* getStreetWidth(uc, dir, lat, lon));
						} else {
							buildingWidth.set(index, 0.);
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

		Index ind = buildingWidth.getIndex();
		return buildingWidth.get(ind.set(uc, dir, lat, lon));
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
		Index ind = streetFrac.getIndex();
		streetFrac.set(ind.set(uc, sd, rlati, rloni), value);
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

		Index ind = buildProb.getIndex();
		return buildProb.get(ind.set(uc, sd, lev, rlati, rloni));
	}

	public double getBuildingFrac(int uc, int irlat, int irlon) {
		Index ind = buildingFrac.getIndex();
		return buildingFrac.get(ind.set(uc, irlat, irlon));
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

		Index ind = buildingFrac.getIndex();
		buildingFrac.set(ind.set(uc, lat, lon), value);
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

		Index ind = buildingWidth.getIndex();
		buildingWidth.set(ind.set(uc, dir, lat, lon), value);
	}

	public void incBuildingFrac(int uc, int irlat, int irlon, double incr) {
		Index ind = buildingFrac.getIndex();
		ind.set(uc, irlat, irlon);
		buildingFrac.set(ind, buildingFrac.get(ind) + incr);
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

	public void reduceHeight(double ignoredBP) {

		int[] maxHeight = new int[getNuclasses()];
		// get the height above which buildings can be ignored
		for (int uc = 0; uc < getNuclasses(); uc++) {
			maxHeight[uc] = 0;
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					if (getBuildingFrac(uc, lat, lon) > 1.e-12) {
						for (int sd = 0; sd < getNstreedir(); sd++) {
							int localeMaxHeight = getKe_urban(uc);
							double ignored = 0;
							for (int lev = getKe_urban(uc) - 1; lev >= 0.; lev--) {
								ignored += getBuildProb(uc, sd, lev, lat, lon);
								if (ignored < ignoredBP)
									localeMaxHeight--;
							}
							if (localeMaxHeight > maxHeight[uc])
								maxHeight[uc] = localeMaxHeight;
						}
					}
				}
			}
		}

		int maxmaxHeight = 0;
		for (int uc = 0; uc < getNuclasses(); uc++) {
			if (maxmaxHeight< maxHeight[uc]) {
				maxmaxHeight = maxHeight[uc];
			}
		}
		
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					if (getBuildingFrac(uc, lat, lon) > 1.e-12) {
						for (int sd = 0; sd < getNstreedir(); sd++) {
							double sum = 0.;
							for (int lev = 0; lev < maxHeight[uc]; lev++) {
								sum += getBuildProb(uc, sd, lev, lat, lon);
							}
							sum = 1. / sum;
							for (int lev = 0; lev < maxHeight[uc]; lev++) {
								setBuildProb(uc, sd, lev, lat, lon, getBuildProb(
										uc, sd, lev, lat, lon)
										* sum);
							}
							for (int lev = maxHeight[uc]; lev < maxmaxHeight; lev++) {
								setBuildProb(uc, sd, lev, lat, lon, 0.);
							}
						}
					}
				}
			}
		}
		
		for (int uc = 0; uc < getNuclasses(); uc++) {
			setKe_urban(uc, maxHeight[uc]);
		}
		ke_urbanmax = maxmaxHeight;
		System.out.println("Height reduce to " + ke_urbanmax + " levels.");
		
		height1.setLength(maxmaxHeight);
		
		buildProb.resetDim();
		
	}

	public void defineMissingData() {
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					if (getBuildingFrac(uc, lat, lon) < 1.e-12
							|| getUrbanFrac(lat, lon) < 1.e-12) {
						for (int sd = 0; sd < getNstreedir(); sd++) {
							setBuildingWidth(uc, sd, lat, lon,
									buildingWidth.missingValue);
							setStreetFrac(uc, sd, lat, lon,
									streetFrac.missingValue);
							setStreetWidth(uc, sd, lat, lon,
									streetFrac.missingValue);
							setUrbanClassFrac(uc, lat, lon,
									urbanClassFrac.missingValue);
							for (int h = 0; h < ke_urbanmax; h++) {
								setBuildProb(uc, sd, h, lat, lon,
										buildProb.missingValue);
							}
						}
						setUrbanFrac(lat, lon, urbanFrac.missingValue);
						setBuildingFrac(uc, lat, lon, buildingFrac.missingValue);
					}
				}
			}
		}
	}

	public void defineMissingDataSVF() {
		for (int uc = 0; uc < getNuclasses(); uc++) {
			for (int lat = 0; lat < getJe_tot(); lat++) {
				for (int lon = 0; lon < getIe_tot(); lon++) {
					if (getBuildingFrac(uc, lat, lon) < 1.e-12
							|| getUrbanFrac(lat, lon) < 1.e-12) {
						for (int sd = 0; sd < getNstreedir(); sd++) {
							for (int h = 0; h < ke_urbanmax; h++) {
								Index index = fgs.getIndex();
								fgs.set(index.set(uc,sd,h,lat,lon), fgs.missingValue);
								for (int h2 = 0; h2 < ke_urbanmax-1; h2++) {
									index = fgow.getIndex();
									fgow.set(index.set(uc,sd,h2,h,lat,lon), fgow.missingValue);
									index = fww.getIndex();
									for (int h3 = 0; h3 < ke_urbanmax-1; h3++) {
										fww.set(index.set(uc,sd,h2,h,h3,lat,lon), fww.missingValue);
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
	
	public int getKe_urban(int uc) {
		Index ind = ke_urban.getIndex();
		return ke_urban.getInt(ind.set(uc));
	}
	
	public void setKe_urban(int uc, int ke) {
		Index ind = ke_urban.getIndex();
		ke_urban.setInt(ind.set(uc), ke);
	}

	public double[] getHeightA() {
		return this.heighta;
	}

	public int getHeightIndex(double height) {
		return this.height1.getIndexOf(height);
	}

	public int getStreetdirIndex(double angle) {
		return streetdir.getIndexOf(angle);
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

	public void setFgs(int uc, int id, int j, int i, double[] fgs) {
		Index ind = this.fgs.getIndex();
		for (int k = 0; k < fgs.length; k++) {
			ind.set(uc, id, k, j, i);
			this.fgs.set(ind, fgs[k]);
		}
	}

	public void setFww(int uc, int id, int j, int i, double[][][] fww) {
		Index ind = this.fww.getIndex();
		for (int k = 0; k < fww.length; k++) {
			for (int l = 0; l < fww[k].length; l++) {
				for (int m = 0; m < fww[k][l].length; m++) {
					ind.set(uc, id, m, l, k, j, i);
					this.fww.set(ind, fww[k][l][m]);
				}
			}
		}
	}

}
