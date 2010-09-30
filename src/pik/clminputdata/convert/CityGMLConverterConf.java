package pik.clminputdata.convert;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import pik.clminputdata.tools.PropertiesEnh;
import pik.clminputdata.tools.StringUtils;

/**
 * Reading and storing of CityGMLConverter configuration.
 * 
 * The configuration is either read from a standard file name or from
 * specifically given file.
 * 
 * @author Sebastian Schubert
 * 
 */
public class CityGMLConverterConf {

	// variables with default

	/**
	 * Latitude of rotated pole
	 */
	double pollat;
	private static final double pollatDefault = 32.5;

	/**
	 * Longitude of rotated pole
	 */
	double pollon;
	private static final double pollonDefault = -170.0;

	/**
	 * Grid spacing in meridional direction (rotated latitude)
	 */
	double dlat;
	private static final double dlatDefault = 0.008;

	/**
	 * Grid spacing in zonal direction (rotated longitude)
	 */
	double dlon;
	private static final double dlonDefault = 0.008;

	/**
	 * Lower left latitude of region
	 */
	double startlat_tot;
	private static final double startlat_totDefault = -7.972;

	/**
	 * Lower left longitude of region
	 */
	double startlon_tot;
	private static final double startlon_totDefault = -1.252;

	/**
	 * Total number of grid points in meridional direction (rotated latitude)
	 */
	int ie_tot;
	private static final int ie_totDefault = 51;

	/**
	 * Total number of grid points in zonal direction (rotated latitude)
	 */
	int je_tot;
	private static final int je_totDefault = 51;

	// int ke_tot;
	// private static final int ke_totDefault = 20;

	/**
	 * Number of urban classes (ONLY 1 SUPPORTED AT THE MOMENT)
	 */
	int nuclasses;
	private static final int nuclassesDefault = 1;

	/**
	 * Angles of street direction
	 */
	double[] streetdir;
	private static final double streetdirDefault[] = { 0., 90. };

	/**
	 * Number of urban height levels for every urban class
	 */
	int[] ke_urban;
	private static final int ke_urbanDefault[] = { 10 };

	/**
	 * Urban height levels
	 */
	double[] height;
	private static final double heightDefault[] = { 0., 5., 10., 15., 20., 25.,
			30., 35., 40., 45. };

	/**
	 * Use entered parameters
	 */
	boolean fakeParameter;
	private static final boolean fakeParameterDefault = false;
	
	double buildingWidth;
	private static final double buildingWidthDefault = 10;
	
	double streetWidth;
	private static final double streetWidthDefault = 20;
	
	double[] buildingProp;
	private static final double buildingPropDefault[] = { 0., 0.05, 0.25, 0.10, 0.20, 0.20, 0.10, 0.05, 0.05, 0.};

	
	boolean useClasses;
	private static final boolean useClassesDefault = false;
	
	int nClass;
	private static final int nClassDefault = 6;
	
	int[] classIndex;
	private static final int classIndexDefault[] = {6,7,8,9,10,11};
	
	/**
	 * Input coordinate system for proj4 transformation
	 */
	String proj4code = "+init=epsg:3068";
	private static String proj4codeDefault = "+init=epsg:3068";

	/**
	 * Largest distance of buildings for determination of street width
	 */
	double maxbuild_radius;
	double maxbuild_radius_sq;
	private static final double maxbuild_radiusDefault = 100.;

	/**
	 * Largest distance building can be in the way
	 */
	double maxcheck_radius;
	private static final double maxcheck_radiusDefault = 100.;

	/**
	 * Minimal distance of surface for street width
	 */
	double mindist;
	private static final double mindistDefault = 2.;

	/**
	 * Use effective distance between two surfaces (as they would be just
	 * opposite of each other?)
	 */
	boolean effDist;
	private static final boolean effDistDefault = false;

	/**
	 * Do a height reduction ignoring large buildings?
	 */
	boolean doHeightReduction = false;
	/**
	 * Value of summed probablity which is ok to ignore
	 */
	double heightReductionP;
	private static final double heightReductionPDefault = 0.;
	
	/**
	 * Calculate skyview factors?
	 */
	boolean calcSVF;
	private static final boolean calcSVFDefault = true;
	
	/**
	 * Number of maximal parallel threads
	 */
	int nThreads;
	private static int nThreadsDefault = 1;

	/**
	 * Maximum threads in the queue
	 */
	int nThreadsQueue;
	private static int nThreadsQueueDefault = 1;

	/**
	 * Folder/File of the CityGML data set
	 */
	String inputGMLFolder;
	private static String inputGMLFolderDefault = "/home/schubert/Documents/workspace/datasets/gml/";

	/**
	 * Output folder
	 */
	String outputFolder;
	private static String outputFolderDefault = "/home/schubert/";

	/**
	 * Log for non planar surfaces (relative to outputFolder)
	 */
	String logNonPlanar;
	private static String logNonPlanarDefault = "NonPlanar.log";

	/**
	 * Log for no distance but surface fraction in a grid cell (relative to
	 * outputFolder)
	 */
	String logNoSurfButBuildFrac;
	private static String logNoSurfButBuildFracDefault = "NoSurfButBuildingFrac.log";

	/**
	 * Log for No defined roofs (relative to outputFolder)
	 */
	String logNoRoof;
	private static String logNoRoofDefault = "NoRoof.log";

	/**
	 * Log for No defined walls (relative to outputFolder)
	 */
	String logNoWall;
	private static String logNoWallDefault = "NoWall.log";

	/**
	 * Log for No defined grounds (relative to outputFolder)
	 */
	String logNoGround;
	private static String logNoGroundDefault = "NoGround.log";

	/**
	 * Name of the main output file (relative to outputFolder)
	 */
	String outputFile;
	private static String outputFileDefault = "city.nc";

	/**
	 * Building statistics (relative to outputFolder)
	 */
	String statsFile;
	private static String statsFileDefault = "stats.nc";

	/**
	 * ASCII file which includes the impervious surface
	 */
	String impSurfFile;
	private static String impSurfFileDefault = "/home/schubert/Documents/workspace/datasets/vg";

	/**
	 * Row of latitude in impSurfFile
	 */
	int rowLat;
	private static int rowLatDefault = 2;

	/**
	 * Row of longitude in impSurfFile
	 */
	int rowLon;
	private static int rowLonDefault = 1;

	/**
	 * Row of impervious surface in impSurfFile
	 */
	int rowImpSurf;
	private static int rowImpSurfDefault = 3;

	/**
	 * Number of lines to skip at the at end of the file
	 */
	int skipLines;
	private static int skipLinesDefault = 0;

	/**
	 * String separating the values in impSurfFile
	 */
	String sepString;
	private static String sepStringDefault = ",";

	/**
	 * Output all fields only where building and urban fraction > 1e-12?
	 */
	boolean consistentOutput;
	private static boolean consistentOutputDefault = true;

	/**
	 * Constructor reading from default configuration file.
	 * @throws Exception 
	 */
	public CityGMLConverterConf() throws Exception {
		
		nuclasses = nuclassesDefault;

		streetdir = streetdirDefault;

		ke_urban = ke_urbanDefault;

		height = heightDefault;

		fakeParameter = fakeParameterDefault;
		
		buildingWidth = buildingWidthDefault;
		streetWidth = streetWidthDefault;
		buildingProp = buildingPropDefault;

		useClasses = useClassesDefault;
		nClass = nClassDefault;
		classIndex = classIndexDefault;
		
		proj4code = proj4codeDefault;

		maxbuild_radius = maxbuild_radiusDefault;

		maxcheck_radius = maxcheck_radiusDefault;

		mindist = mindistDefault;

		effDist = effDistDefault;

		heightReductionP = heightReductionPDefault;
		
		calcSVF = calcSVFDefault;
		
		nThreads = nThreadsDefault;
		nThreadsQueue = nThreadsQueueDefault;

		logNonPlanar = logNonPlanarDefault;
		logNoSurfButBuildFrac = logNoSurfButBuildFracDefault;
		logNoRoof = logNoRoofDefault;
		logNoWall = logNoWallDefault;
		logNoGround = logNoGroundDefault;

		outputFile = outputFileDefault;
		statsFile = statsFileDefault;

		rowLat = rowLatDefault;
		rowLon = rowLonDefault;

		rowImpSurf = rowImpSurfDefault;

		skipLines = skipLinesDefault;

		sepString = sepStringDefault;
	}

	/**
	 * Constructor.
	 * 
	 * @param confFilename
	 *            Name of configuration file
	 * @throws Exception 
	 */
	public CityGMLConverterConf(String confFilename) throws Exception {
		readConf(new File(confFilename));
	}

	
	static void addComponent( Container cont, 
			GridBagLayout gbl, 
			Component c, 
			int x, int y, 
			int width, int height, 
			double weightx, double weighty ) 
	{ 
		GridBagConstraints gbc = new GridBagConstraints(); 
		gbc.fill = GridBagConstraints.BOTH; 
		gbc.gridx = x; gbc.gridy = y; 
		gbc.gridwidth = width; gbc.gridheight = height; 
		gbc.weightx = weightx; gbc.weighty = weighty; 
		gbl.setConstraints( c, gbc ); 
		cont.add( c ); 
	} 
	
	public void generateConf() {
			
		JFrame f = new JFrame("CityGML2CLM Configuration Generator");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(400, 300);
		f.setLocationRelativeTo(null);
		
		Container c = f.getContentPane();
		GridBagLayout gbl = new GridBagLayout();
		c.setLayout(gbl);
		
		JLabel gridText = new JLabel("Grid Parameters", SwingConstants.CENTER);
		
		JLabel pollonText = new JLabel("Pollon");
		JTextField pollonField = new JTextField(String.valueOf(pollon), 4);
		
		JLabel pollatText = new JLabel("Pollat");
		JTextField pollatField = new JTextField(String.valueOf(pollat), 4);
		
		JLabel dlonText = new JLabel("dlon");
		JTextField dlonField = new JTextField(String.valueOf(dlon), 4);
		
		JLabel dlatText = new JLabel("dlat");
		JTextField dlatField = new JTextField(String.valueOf(dlat), 4);
		
		JLabel startlonText = new JLabel("startlon");
		JTextField startlonField = new JTextField(String.valueOf(startlon_tot), 4);
		
		JLabel startlatText = new JLabel("startlat");
		JTextField startlatField = new JTextField(String.valueOf(startlat_tot), 4);

		JLabel ieText = new JLabel("ie_tot");
		JTextField ieField = new JTextField(String.valueOf(ie_tot), 4);
		
		JLabel jeText = new JLabel("je_tot");
		JTextField jeField = new JTextField(String.valueOf(je_tot), 4);
		
		
		JLabel urbanText = new JLabel("Urban Parameters", SwingConstants.CENTER);
		
		JLabel nClassText = new JLabel("Number of urban classes");
		JTextField nClassField = new JTextField(String.valueOf(nuclasses), 2);
		
		JLabel streetDirText = new JLabel("Street directions");
		JTextField streetDirField = new JTextField(StringUtils.get(streetdir), 30);
		
		JLabel nkeurbanText = new JLabel("Number of urban height levels for each class");
		JTextField nkeurbanField = new JTextField(StringUtils.get(ke_urban), 30);		
		
		JLabel keurbanText = new JLabel("Height levels");
		JTextField keurbanField = new JTextField(StringUtils.get(height), 2);		
		
		final JLabel maxbuildText = new JLabel("Max. building distance");
		final JTextField maxbuildField = new JTextField(String.valueOf(maxbuild_radius), 2);
		
		final JLabel maxcheckText = new JLabel("Max. checking distance");
		final JTextField maxcheckField = new JTextField(String.valueOf(maxcheck_radius), 2);
		
		final JLabel mindistText = new JLabel("Min. distance");
		final JTextField mindistField = new JTextField(String.valueOf(mindist), 2);
		
		final JLabel heightRedText = new JLabel("Height Reduction Percentage");
		final JTextField heightRedField = new JTextField(String.valueOf(heightReductionP), 2);
		
		final JLabel proj4Text = new JLabel("proj4 Code");
		final JTextField proj4Field = new JTextField(String.valueOf(proj4code), 10);

		final JCheckBox effDistBox = new JCheckBox("Effective Distance",effDist);
		
		JButton save = new JButton("Save");
		JButton cancel = new JButton("Cancel");
		
		final JRadioButton calcParaButton = new JRadioButton("CityGML Input", !(fakeParameter||useClasses));
		final JRadioButton classParaButton = new JRadioButton("Class Input", useClasses);
		final JRadioButton fakeParaButton = new JRadioButton("Fake Input", fakeParameter);
		ButtonGroup g = new ButtonGroup();
		g.add(calcParaButton);
		g.add(classParaButton);
		g.add(fakeParaButton);
		
		JCheckBox calcSVFBox = new JCheckBox("Calculation VF", calcSVF);
		
//		actions
		ItemListener calcMethod = new ItemListener() {
			@Override public void itemStateChanged(ItemEvent e) {
				if (calcParaButton.isSelected()) {
					maxbuildText.setVisible(true);
					maxbuildField.setVisible(true);
					maxcheckText.setVisible(true);
					maxcheckField.setVisible(true);
					mindistText.setVisible(true);
					mindistField.setVisible(true);
					heightRedField.setVisible(true);
					heightRedText.setVisible(true);
					proj4Field.setVisible(true);
					proj4Text.setVisible(true);
					effDistBox.setVisible(true);
				} else {
					maxbuildText.setVisible(false);
					maxbuildField.setVisible(false);
					maxcheckText.setVisible(false);
					maxcheckField.setVisible(false);
					mindistText.setVisible(false);
					mindistField.setVisible(false);
					heightRedField.setVisible(false);
					heightRedText.setVisible(false);
					proj4Field.setVisible(false);
					proj4Text.setVisible(false);
					effDistBox.setVisible(false);
				}
			}
		};
		
		ActionListener bClick = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				String action = e.getActionCommand();
				if (action=="Cancel") {
					System.exit(0);
				}
			}
		};
		
		save.addActionListener(bClick);
		cancel.addActionListener(bClick);
		
//		two is enough
		calcParaButton.addItemListener(calcMethod);
		classParaButton.addItemListener(calcMethod);
		
		int r = 0;
		addComponent(c, gbl, gridText, 0, r++, 2, 1,0, 0);
		
		addComponent(c, gbl, pollonText, 0, r, 1, 1,0, 0);
		addComponent(c, gbl, pollonField, 1, r, 1, 1,0, 0);
		addComponent(c, gbl, pollatText, 2, r, 1, 1,0, 0);
		addComponent(c, gbl, pollatField, 3, r++, 1, 1,0, 0);
		
		addComponent(c, gbl, dlonText, 0, r, 1, 1,0, 0);
		addComponent(c, gbl, dlonField, 1, r, 1, 1,0, 0);
		addComponent(c, gbl, dlatText, 2, r, 1, 1,0, 0);
		addComponent(c, gbl, dlatField, 3, r++, 1, 1,0, 0);
		
		addComponent(c, gbl, startlonText, 0, r, 1, 1,0, 0);
		addComponent(c, gbl, startlonField, 1, r, 1, 1,0, 0);
		addComponent(c, gbl, startlatText, 2, r, 1, 1,0, 0);
		addComponent(c, gbl, startlatField, 3, r++, 1, 1,0, 0);
		
		addComponent(c, gbl, ieText, 0, r, 1, 1,0, 0);
		addComponent(c, gbl, ieField, 1, r, 1, 1,0, 0);
		addComponent(c, gbl, jeText, 2, r, 1, 1,0, 0);
		addComponent(c, gbl, jeField, 3, r++, 1, 1,0, 0);
					
		addComponent(c, gbl, urbanText, 0, r++, 2, 1,0, 0);
		
		addComponent(c, gbl, nClassText, 0, r, 1, 1,0, 0);
		addComponent(c, gbl, nClassField, 1, r++, 1, 1,0, 0);
		addComponent(c, gbl, streetDirText, 0, r, 1, 1,0, 0);
		addComponent(c, gbl, streetDirField, 1, r++, 1, 1,0, 0);
		addComponent(c, gbl, nkeurbanText, 0, r, 1, 1,0, 0);
		addComponent(c, gbl, nkeurbanField, 1, r++, 1, 1,0, 0);
		addComponent(c, gbl, keurbanText, 0, r, 1, 1,0, 0);
		addComponent(c, gbl, keurbanField, 1, r++, 1, 1,0, 0);		
		
		int s = r;
		addComponent(c, gbl, calcParaButton, 0, r++, 1, 1,0, 0);
		addComponent(c, gbl, classParaButton, 0, r++, 1, 1,0, 0);
		addComponent(c, gbl, fakeParaButton, 0, r++, 1, 1,0, 0);
		
		addComponent(c, gbl, maxbuildText, 2, s, 1, 1,0, 0);
		addComponent(c, gbl, maxbuildField, 3, s++, 1, 1,0, 0);
		addComponent(c, gbl, maxcheckText, 2, s, 1, 1,0, 0);
		addComponent(c, gbl, maxcheckField, 3, s++, 1, 1,0, 0);
		addComponent(c, gbl, mindistText, 2, s, 1, 1,0, 0);
		addComponent(c, gbl, mindistField, 3, s++, 1, 1,0, 0);
		addComponent(c, gbl, heightRedText, 2, s, 1, 1,0, 0);
		addComponent(c, gbl, heightRedField, 3, s++, 1, 1,0, 0);
		addComponent(c, gbl, proj4Text, 2, s, 1, 1,0, 0);
		addComponent(c, gbl, proj4Field, 3, s++, 1, 1,0, 0);
		addComponent(c, gbl, effDistBox, 3, s++, 1, 1,0, 0);

		
		addComponent(c, gbl, calcSVFBox, 0, r++, 1, 1,0, 0);
		
		addComponent(c, gbl, save, 0, r, 1, 1, 0, 0);
		addComponent(c, gbl, cancel, 1, r, 1, 1, 0, 0);
				
		f.pack();
		f.setVisible(true);
	}
	
	/**
	 * Read a configuration file.
	 * 
	 * @param confFile
	 *            Configuration file
	 * @throws Exception 
	 */
	private void readConf(File confFile)
			throws Exception {
		if (confFile.exists()) {

			// read the file
			Reader reader = new FileReader(confFile);
			PropertiesEnh prop = new PropertiesEnh();
			prop.load(reader);

			pollat = prop.getDouble("pollat", pollatDefault);
			pollon = prop.getDouble("pollon", pollonDefault);
			dlat = prop.getDouble("dlat", dlatDefault);
			dlon = prop.getDouble("dlon", dlonDefault);

			startlat_tot = prop.getDouble("startlat_tot", startlat_totDefault);
			startlon_tot = prop.getDouble("startlon_tot", startlon_totDefault);

			ie_tot = prop.getInt("ie_tot", ie_totDefault);
			je_tot = prop.getInt("je_tot", je_totDefault);
			// ke_tot = prop.getInt("ke_tot", ke_totDefault);

			nuclasses = prop.getInt("nuclasses", nuclassesDefault);

			streetdir = prop.getDoubleArray("streetdir", streetdirDefault);
			ke_urban = prop.getIntArray("ke_urban", ke_urbanDefault);
			height = prop.getDoubleArray("height", heightDefault);

			fakeParameter = prop.getBoolean("fakeParameter", fakeParameterDefault);
			
			useClasses = prop.getBoolean("useClasses", useClassesDefault);
			if (fakeParameter&&useClasses) {
				throw new Exception("useClasses and fakeParameter cannot be true at the same time");
			}
			
			if (fakeParameter) {
				buildingWidth = prop.getDouble("buildingWidth", buildingWidthDefault);
				streetWidth = prop.getDouble("streetWidth", streetWidthDefault);
				buildingProp = prop.getDoubleArray("buildingProp", buildingPropDefault);
				if (buildingProp.length!=height.length) {
					throw new Exception("Wrong height number of height levels");
				}
			}
			
			if (useClasses) {
				nClass = prop.getInt("nClass", nClassDefault);
				classIndex = prop.getIntArray("classIndex", classIndexDefault );
			}
			
			proj4code = prop.getString("proj4code", proj4codeDefault);

			maxbuild_radius = prop.getDouble("maxbuild_radius",
					maxbuild_radiusDefault);
			maxbuild_radius_sq = maxbuild_radius * maxbuild_radius;
			maxcheck_radius = prop.getDouble("maxcheck_radius",
					maxcheck_radiusDefault);
			mindist = prop.getDouble("mindist", mindistDefault);

			effDist = prop.getBoolean("effDist", effDistDefault);

			
			heightReductionP = prop.getDouble("heightReductionP", heightReductionPDefault);
			if (heightReductionP > 0.) doHeightReduction = true;
			
			calcSVF = prop.getBoolean("calcSVF", calcSVFDefault);
			
			nThreads = prop.getInt("nThreads", nThreadsDefault);
			nThreadsQueue = prop.getInt("nThreadsQueue", nThreadsQueueDefault);

			inputGMLFolder = prop.getString("inputGMLFolder",
					inputGMLFolderDefault);
			outputFolder = prop.getString("outputFolder", outputFolderDefault);

			logNonPlanar = prop.getString("logNonPlanar", logNonPlanarDefault);
			logNonPlanar = outputFolder + logNonPlanar;
			logNoSurfButBuildFrac = prop.getString("logNoSurfButBuildFrac",
					logNoSurfButBuildFracDefault);
			logNoSurfButBuildFrac = outputFolder + logNoSurfButBuildFrac;

			logNoGround = prop.getString("logNoGround", logNoGroundDefault);
			logNoGround = outputFolder + logNoGround;
			logNoRoof = prop.getString("logNoRoof", logNoRoofDefault);
			logNoRoof = outputFolder + logNoRoof;
			logNoWall = prop.getString("logNoWall", logNoWallDefault);
			logNoWall = outputFolder + logNoWall;

			outputFile = prop.getString("outputFile", outputFileDefault);
			outputFile = outputFolder + outputFile;

			statsFile = prop.getString("statsFile", statsFileDefault);
			statsFile = outputFolder + statsFile;

			impSurfFile = prop.getString("impSurfFile", impSurfFileDefault);

			rowLat = prop.getInt("rowLat", rowLatDefault);
			rowLon = prop.getInt("rowLon", rowLonDefault);
			rowImpSurf = prop.getInt("rowImpSurf", rowImpSurfDefault);

			skipLines = prop.getInt("skipLines", skipLinesDefault);

			sepString = prop.getString("sepString", sepStringDefault);

			consistentOutput = prop.getBoolean("consistentOutput",
					consistentOutputDefault);

		} else {
			throw new FileNotFoundException("Configuration file not found");
		}
	}

	/**
	 * Output of the used configuration.
	 */
	public void outputConf() {

		System.out.println("CONFIGURATION OF THE RUN");
		System.out.println();

		System.out.println("pollat: " + pollat);
		System.out.println("pollon: " + pollon);
		System.out.println("dlat: " + dlat);
		System.out.println("dlon: " + dlon);

		System.out.println("startlat_tot: " + startlat_tot);
		System.out.println("startlon_tot: " + startlon_tot);

		System.out.println("ie_tot: " + ie_tot);
		System.out.println("je_tot: " + je_tot);
		// System.out.println("ke_tot: " + ke_tot);

		System.out.println("nuclasses: " + nuclasses);

		System.out.print("streetdir: ");
		for (int i = 0; i < streetdir.length; i++) {
			System.out.print(streetdir[i] + " ");
		}
		System.out.println();
		System.out.print("ke_urban: ");
		for (int i = 0; i < ke_urban.length; i++) {
			System.out.print(ke_urban[i] + " ");
		}
		System.out.println();
		System.out.print("height: ");
		for (int i = 0; i < height.length; i++) {
			System.out.print(height[i] + " ");
		}
		System.out.println();
		System.out.println("fakeParameters: " + fakeParameter);
		if(fakeParameter) {
			System.out.println("buildingWidth: " + buildingWidth);
			System.out.println("streetWidth: " + streetWidth);
		}
		System.out.println("useClasses: " + useClasses);
		if (useClasses) {
			System.out.println("nClass: " + nClass);
			System.out.print("classIndex: ");
			for (int i = 0; i < classIndex.length; i++) {
				System.out.print(classIndex[i] + " ");
			}
		}
		System.out.println();
		
		System.out.println("maxbuild_radius: " + maxbuild_radius);
		System.out.println("maxcheck_radius: " + maxcheck_radius);
		System.out.println("mindist: " + mindist);

		System.out.println("effDist: " + effDist);

		System.out.println("heightReductionP: " + heightReductionP);
		
		System.out.println("calcSVF: " + calcSVF);
		
		System.out.println("proj4code: " + proj4code);

		System.out.println("nThreads: " + nThreads);
		System.out.println("nThreadsQueue: " + nThreadsQueue);

		System.out.println("inputGMLFolder: " + inputGMLFolder);
		System.out.println("outputFolder: " + outputFolder);

		System.out.println("logNonPlanar: " + logNonPlanar);
		System.out.println("logNoSurfButBuildFrac: " + logNoSurfButBuildFrac);

		System.out.println("LogNoWall: " + logNoWall);
		System.out.println("LogNoGround: " + logNoGround);
		System.out.println("LogNoRoof: " + logNoRoof);

		System.out.println("outputFile: " + outputFile);

		System.out.println("statsFile: " + statsFile);

		System.out.println("impSurfFile: " + impSurfFile);

		System.out.println("rowLat: " + rowLat);
		System.out.println("rowLon: " + rowLon);
		System.out.println("rowImpSurf: " + rowImpSurf);

		System.out.println("skipLines: " + skipLines);

		System.out.println("sepString: " + sepString);

		System.out.println("consistentOutput: " + consistentOutput);

		System.out.println();
		System.out.println();

	}

}
