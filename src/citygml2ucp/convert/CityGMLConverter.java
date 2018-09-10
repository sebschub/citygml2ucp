package citygml2ucp.convert;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List; //import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.jaxb.CityGMLBuilder;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.xml.io.CityGMLInputFactory;
import org.citygml4j.xml.io.reader.CityGMLReader;
import org.proj4.PJ;

import citygml2ucp.configuration.UrbanCLMConfiguration;
import citygml2ucp.tools.GMLFilenameFilter;
import citygml2ucp.tools.GroundOtherSkySVF;
import citygml2ucp.tools.GroundOtherWallSVF;
import citygml2ucp.tools.Integrator;
import citygml2ucp.tools.WallOtherSkySVF;
import citygml2ucp.tools.WallOtherWallSVF;

/**
 * Main programme.
 * 
 * @author Sebastian Schubert
 * 
 */
public class CityGMLConverter {

	/**
	 * Read data of impervious surfaces.
	 * 
	 * @param conf
	 *            Configuration of this run (includes file information)
	 * @param uclm
	 *            Urban configuration to store the data to
	 * @throws IOException
	 *             File from conf not found
	 */
	private static void readImpSurfaceFile(CityGMLConverterConf conf,
			UrbanCLMConfiguration uclm) throws IOException {
		Scanner scanner = new Scanner(new File(conf.impSurfFile));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Impervious Surface has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNext()) {
				values.add(Double.parseDouble(lScanner.next()));
			}
			int lat = uclm.getRLatIndex(values.get(conf.rowLat - 1));
			int lon = uclm.getRLonIndex(values.get(conf.rowLon - 1));
			uclm.setUrbanFrac(lat, lon, values.get(conf.rowImpSurf - 1) / 100.);
			lScanner.close();
		}
		scanner.close();

	}
	
	private static void asciiInput(CityGMLConverterConf conf,
			UrbanCLMConfiguration uclm) throws IOException {
		
//		urban fraction
		Scanner scanner = new Scanner(new File("urb_b"));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Impervious Surface has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNext()) {
				values.add(Double.parseDouble(lScanner.next()));
			}
			int lat = uclm.getRLatIndex(values.get(conf.rowLat - 1));
			int lon = uclm.getRLonIndex(values.get(conf.rowLon - 1));
			uclm.setUrbanFrac(lat, lon, values.get(conf.rowImpSurf - 1) / 100.);
			lScanner.close();
		}
		scanner.close();
		
//		building fraction
		scanner = new Scanner(new File(conf.bldFile));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Impervious Surface has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNext()) {
				values.add(Double.parseDouble(lScanner.next()));
			}
			int lat = uclm.getRLatIndex(values.get(conf.rowLat - 1));
			int lon = uclm.getRLonIndex(values.get(conf.rowLon - 1));
			uclm.setBuildingFrac(0,lat, lon, values.get(conf.rowImpSurf - 1) / 100.);
			lScanner.close();
		}
		scanner.close();

//		street width
		scanner = new Scanner(new File(conf.stwFile));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Impervious Surface has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNext()) {
				values.add(Double.parseDouble(lScanner.next()));
			}
			int lat = uclm.getRLatIndex(values.get(2 - 1));
			int lon = uclm.getRLonIndex(values.get(1 - 1));
			for (int id = 0; id < uclm.getNstreedir(); id++) {
				uclm.setStreetWidth(0,id,lat, lon, values.get(3 - 1));
			}
			lScanner.close();
		}
		scanner.close();
		
		
//		street direction
		scanner = new Scanner(new File(conf.stdFile));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Impervious Surface has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNext()) {
				values.add(Double.parseDouble(lScanner.next()));
			}
			int lat = uclm.getRLatIndex(values.get(3 - 1)+conf.dlat/2.);
			int lon = uclm.getRLonIndex(values.get(1 - 1)+conf.dlon/2.);
			for (int id = 0; id < uclm.getNstreedir(); id++) {
				uclm.setStreetFrac(0,id,lat, lon, values.get(4 + id));
			}
			lScanner.close();
		}
		scanner.close();
		
		
//		building height
		scanner = new Scanner(new File(conf.blhFile));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Impervious Surface has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNext()) {
				values.add(Double.parseDouble(lScanner.next()));
			}
			int lat = uclm.getRLatIndex(values.get(3 - 1)+conf.dlat/2.);
			int lon = uclm.getRLonIndex(values.get(1 - 1)+conf.dlon/2.);
			double[] bhvalues = new double[uclm.getKe_urbanMax()];
			double sum = 0;
			for (int i = 0; i < bhvalues.length; i++) {
				bhvalues[i]=values.get(4+i);
				sum+=bhvalues[i];
			}
			for (int i = 0; i < bhvalues.length; i++) {
				bhvalues[i]/=sum;
				for (int id = 0; id < uclm.getNstreedir(); id++) {
					uclm.setBuildProb(0, id, i, lat, lon, bhvalues[i]);
				}
			}
					
			lScanner.close();
		}
		scanner.close();
		
		System.out.println("correction input");
		
//		only little real building data available, use standard there
		for (int i = 0; i < uclm.getIe_tot(); i++) {
			for (int j = 0; j < uclm.getJe_tot(); j++) {
				if (uclm.getUrbanFrac(j, i)<0.05 && uclm.getUrbanFrac(j, i)>0.) {
					for (int id = 0; id < uclm.getNstreedir(); id++) {
						for (int k = 0; k < uclm.getKe_urban(0); k++) {
							uclm.setBuildProb(0, id, k, j, i, 0.);
						}
						uclm.setBuildProb(0, id, 0, j, i, 0.05);
						uclm.setBuildProb(0, id, 1, j, i, 0.2);
						uclm.setBuildProb(0, id, 2, j, i, 0.40);
						uclm.setBuildProb(0, id, 3, j, i, 0.3);
						uclm.setBuildProb(0, id, 4, j, i, 0.05);
						
						uclm.setStreetFrac(0, id, j, i, 1./uclm.getNstreedir());
						uclm.setStreetWidth(0, id, j, i, 25);
					}
				}
			}
		}
		
//		urban fraction
		scanner = new Scanner(new File(conf.urbFile));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Impervious Surface has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNext()) {
				values.add(Double.parseDouble(lScanner.next()));
			}
			int lat = uclm.getRLatIndex(values.get(conf.rowLat - 1));
			int lon = uclm.getRLonIndex(values.get(conf.rowLon - 1));
			uclm.setUrbanFrac(lat, lon, values.get(conf.rowImpSurf - 1) / 100.);
			lScanner.close();
		}
		scanner.close();
		
		for (int i = 0; i < uclm.getIe_tot(); i++) {
			for (int j = 0; j < uclm.getJe_tot(); j++) {
				if (uclm.getUrbanFrac(j, i)>0. && !(uclm.getStreetWidth(0, 0, j, i)>0.)) {
					for (int id = 0; id < uclm.getNstreedir(); id++) {
						for (int k = 0; k < uclm.getKe_urban(0); k++) {
							uclm.setBuildProb(0, id, k, j, i, 0.);
						}
						uclm.setBuildProb(0, id, 0, j, i, 0.05);
						uclm.setBuildProb(0, id, 1, j, i, 0.2);
						uclm.setBuildProb(0, id, 2, j, i, 0.40);
						uclm.setBuildProb(0, id, 3, j, i, 0.3);
						uclm.setBuildProb(0, id, 4, j, i, 0.05);
						
						uclm.setStreetWidth(0, id, j, i, 25);
						uclm.setStreetFrac(0, id, j, i, 1./uclm.getNstreedir());
					}
				}
			}
		}
		
		
		System.out.println("defining building width");
		
		for (int i = 0; i < uclm.getIe_tot(); i++) {
			for (int j = 0; j < uclm.getJe_tot(); j++) {
				if (uclm.getUrbanFrac(j, i)>0.) {
					for (int id = 0; id < uclm.getNstreedir(); id++) {
						uclm.setBuildingWidth(0, id, j, i, 
								uclm.getBuildingFrac(0, j, i)/(uclm.getUrbanFrac(j, i)-uclm.getBuildingFrac(0, j, i))
								*uclm.getStreetWidth(0, id, j, i));
					}
					uclm.setUrbanClassFrac(0, j, i, 1.);
				}
			}
		}
		
	}


	private static void inputClassData(CityGMLConverterConf conf,
			UrbanCLMConfiguration uclm) throws IOException {
		
//		assuming 0., 5., 10., 15., 20., 25., 30. height levels
		
		double frurb[] = {0.8,0.6,0.6};
		double s[] = {20., 20., 30};
		double b[] = {15., 10., 20.};
		double[][] h = new double[3][];
		h[0] = new double[]{0.00, 0.03, 0.02, 0.04, 0.19, 0.41, 0.26, 0.05};
		h[1] = new double[]{0.00, 0.17, 0.45, 0.25, 0.13, 0.00, 0.00, 0.00};
		h[2] = new double[]{0.00, 0.03, 0.02, 0.19, 0.26, 0.30, 0.20, 0.00};
				
		Scanner scanner = new Scanner(new File(conf.impSurfFile));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Class data file has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNextDouble()) {
				values.add(lScanner.nextDouble());
			}
			System.out.println("Found " + values.size() + " elements");
			for (int i = 0; i < values.size(); i++) {
				System.out.println(i + " " + values.get(i));
			}
			int lat = uclm.getRLatIndex(values.get(conf.rowLat - 1));
			int lon = uclm.getRLonIndex(values.get(conf.rowLon - 1));
			System.out.println("Lon index: " + lon + "  Lat index: " + lat);
			
			//dont want classes, just the highest fraction
			double fr[] = new double[3];
			double frtemp[] = new double[6];
			for (int i = 0; i < frtemp.length; i++) {
				System.out.println(conf.classIndex[i]);
				frtemp[i] = values.get(conf.classIndex[i]-1);
			}
			fr[0] = frtemp[0]; // continuos urban fabric
			fr[1] = frtemp[1]; // discontiunous urban fabric
			fr[2] = frtemp[2] + frtemp[3] + frtemp[4] + frtemp[5]; 
			
			double maxv = fr[0];
			int indexmax = 0;
			for (int i = 1; i < fr.length; i++) {
				if (fr[i]>maxv) {
					maxv = fr[i];
					indexmax = i;
				}
			}
			
			double sum = 0.;
			double weightedSum = 0.;
			for (int i = 0; i < fr.length; i++) {
				sum += fr[i];
				weightedSum += fr[i]*frurb[i];
			}
			System.out.println("Total sum: " + sum);
			System.out.println("Total weightedSum" + weightedSum);
			if (sum<1.e-10) {
				uclm.setUrbanFrac(lat, lon, 0.);
			} else {
				uclm.setUrbanFrac(lat, lon, weightedSum);
			}
			
			//for (int i = 0; i < fr.length; i++) {
				if (sum<1.e-10) {
					uclm.setUrbanClassFrac(0, lat, lon, 0.);
				} else {
					uclm.setUrbanClassFrac(0, lat, lon, 1);
				}
				for (int j = 0; j < conf.streetdir.length; j++) {
					uclm.setStreetFrac(0, j, lat, lon, 1./conf.streetdir.length);
					uclm.setBuildingWidth(0, j, lat, lon, b[indexmax]);
					uclm.setStreetWidth(0, j, lat, lon, s[indexmax]);
					for (int hz = 0; hz < conf.ke_urban[0]; hz++) {
						uclm.setBuildProb(0, j, hz, lat, lon, h[indexmax][hz]);
					}
				}				
			//}
			
			lScanner.close();
		}
		scanner.close();

	}
	
	
	/**
	 * Main routine of the converter.
	 * 
	 * @param args
	 *            Path to properties of the run
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		long startTime = new Date().getTime();

		CityGMLConverterConf conf;

		if (args.length == 1) {
			conf = new CityGMLConverterConf(args[0]);
		} else {
			conf = new CityGMLConverterConf();
		}

		conf.outputConf();

		// create new urban configuration
		UrbanCLMConfiguration uclm = new UrbanCLMConfiguration(conf.pollat,
				conf.pollon, conf.dlat, conf.dlon, conf.startlat_tot,
				conf.startlon_tot, conf.ie_tot, conf.je_tot, conf.nuclasses,
				conf.streetdir, conf.ke_urban, conf.height, conf.useClasses,
				conf.confItems, conf.confValues);

		if (conf.useClasses) {
			inputClassData(conf, uclm);
			if (conf.consistentOutput) {
				uclm.defineMissingData();
			}
		} else if(conf.fakeParameter) {
			readImpSurfaceFile(conf, uclm);
			for (int lon = 0; lon < conf.ie_tot; lon++) {
				for (int lat = 0; lat < conf.je_tot; lat++) {
					if (uclm.getUrbanFrac(lat, lon)>0.) {
						for (int uc = 0; uc < conf.nuclasses; uc++) {
							for (int dir = 0; dir < conf.streetdir.length; dir++) {
								uclm.setStreetFrac(uc, dir, lat, lon, 1./conf.streetdir.length);
								uclm.setBuildingWidth(uc, dir, lat, lon, conf.buildingWidth);
								uclm.setStreetWidth(uc, dir, lat, lon, conf.streetWidth);
								uclm.setBuildingFrac(uc, lat, lon, 1./(1.+conf.streetWidth/conf.buildingWidth));
								uclm.setUrbanClassFrac(uc, lat, lon, 1./conf.nuclasses);
								for (int hz = 0; hz < conf.ke_urban[uc]; hz++) {
									uclm.setBuildProb(uc, dir, hz, lat, lon, conf.buildingProp[hz]);
								}
							}
						}
					}
				}
			}
			if (conf.consistentOutput) {
				uclm.defineMissingData();
			}
		} else if(conf.asciiInput) {
			asciiInput(conf, uclm);
			if (conf.consistentOutput) {
				uclm.defineMissingData();
			}
		} else {

			PJ sourcePJ = new PJ(conf.proj4code);
			PJ targetPJ = new PJ("+init=epsg:4326 +latlong");

			// set up citygml4j context
			CityGMLContext ctx = CityGMLContext.getInstance();
			CityGMLBuilder builder = ctx.createCityGMLBuilder();
			CityGMLInputFactory in = builder.createCityGMLInputFactory();
			
			File[] flist;
			File folder = new File(conf.inputGMLFolder);
			if (folder.isDirectory()) {
				flist = new File(conf.inputGMLFolder)
						.listFiles(new GMLFilenameFilter());
			} else {
				flist = new File[1];
				flist[0] = folder;
			}

			CityGMLConverterStats stats = new CityGMLConverterStats(flist, conf);

			readImpSurfaceFile(conf, uclm);

			ThreadPoolExecutor exec = new ThreadPoolExecutor(conf.nThreads,
					conf.nThreads, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>(conf.nThreadsQueue),
					new ThreadPoolExecutor.CallerRunsPolicy());

			// here loop over citygmlfiles
			for (int i = 0; i < flist.length; i++) {

				File file = flist[i];

				System.out.println("Processing file " + (i + 1) + "/"
						+ flist.length + ": " + file);
				
				CityGMLReader reader = in.createCityGMLReader(file);
				while (reader.hasNext()) {
					CityGML citygml = reader.nextFeature();
					
					if (citygml.getCityGMLClass() == CityGMLClass.CITY_MODEL) {
						CityModel cityModel = (CityModel)citygml;
						
						CityGMLConverterThread cgmlct = new CityGMLConverterThread(
								uclm, conf, sourcePJ, targetPJ, stats, cityModel, i, file
								.toString());
						// everything that is need is now in cgmlct, rest can be deleted
						cityModel = null;

						if (conf.nThreads > 1) {
							exec.execute(cgmlct);
						} else {
							cgmlct.run();
						}
					}
				}
				
				reader.close();
				
			}

			exec.shutdown();
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

			System.out.println("Largest Building: " + uclm.maxHeight + " m");
			System.out.println("Smallest Building: " + uclm.minHeight + " m");

			uclm.fakeUrbanClassFrac();
			uclm.normBuildingFrac();
			uclm.normBuildProbAndCalcStreetFraction();
			uclm.normStreetWidth();
			uclm.calculateBuildingWidth();
			if (conf.consistentOutput) {
				uclm.defineMissingData();
			}
			if (conf.doHeightReduction) {
				uclm.reduceHeight(conf.heightReductionP);
			}
			
			stats.writeLogs();
			stats.toNetCDFfile(conf.statsFile);
		}

		long lasted = new Date().getTime() - startTime;
		System.out.printf("Urban parameter calculation took %.3f minutes.%n",
				lasted / 1000. / 60.);

		if (conf.calcSVF) {

			System.out.println("Starting calculation of Skyview factors.");
			startTime = new Date().getTime();

			uclm.initalizeSVFFields();

			// Integrator itg = new Integrator();

			ThreadPoolExecutor exec = new ThreadPoolExecutor(conf.nThreads, conf.nThreads,
					Long.MAX_VALUE, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>(conf.nThreadsQueue),
					new ThreadPoolExecutor.CallerRunsPolicy());

			for (int iurb = 0; iurb < uclm.getNuclasses(); iurb++) {
				for (int id = 0; id < uclm.getNstreedir(); id++) {
					for (int j = 0; j < uclm.getJe_tot(); j++) {
						for (int i = 0; i < uclm.getIe_tot(); i++) {
							if (uclm.getUrbanFrac(j, i) > 1.e-12) {
								GroundOtherWallSVF gow = new GroundOtherWallSVF(
										iurb, id, j, i, uclm, new Integrator());
								GroundOtherSkySVF gs = new GroundOtherSkySVF(
										iurb, id, j, i, uclm, new Integrator());
								WallOtherWallSVF wws = new WallOtherWallSVF(
										iurb, id, j, i, uclm, new Integrator());
								WallOtherSkySVF wss = new WallOtherSkySVF(iurb,
										id, j, i, uclm, new Integrator());
								System.out
										.println("SVF Calculation for iurb = "
												+ iurb + ", id = " + id
												+ ", j = " + j + ", i = " + i);
								if (conf.nThreads > 1) {
									exec.execute(gow);
									exec.execute(gs);
									exec.execute(wws);
									exec.execute(wss);
								} else {
									gow.run();
									gs.run();
									wws.run();
									wss.run();
								}
							}
						}
					}
				}
			}

			exec.shutdown();
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			if (conf.consistentOutput) {
				uclm.defineMissingDataSVF();
			}
			lasted = new Date().getTime() - startTime;
			System.out.printf(
					"Urban skyview factor calculation took %.3f minutes.%n",
					lasted / 1000. / 60.);
		}

		uclm.toNetCDFfile(conf.outputFile);

	}
}
