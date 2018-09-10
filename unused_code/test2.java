package citygml2ucp.tools;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

public class test2 {
	public static void main(String[] args) throws Exception {

		Writer writer = null; 
		Reader reader = null; 
		 
		try 
		{ 
		  writer = new FileWriter( "/home/schubert/test.log" ); 
		  Properties prop1 = new Properties( System.getProperties() ); 
		  prop1.setProperty( "MeinNameIst", "Forrest Gump" ); 
		  prop1.store( writer, "Eine Insel mit zwei Bergen" ); 
		 
//		  reader = new FileReader( "/home/schubert/test.log" ); 
//		 
//		  Properties prop2 = new Properties(); 
//		  prop2.load( reader ); 
//		  prop2.list( System.out ); 
		} 
		catch ( IOException e ) 
		{ 
		  e.printStackTrace(); 
		} 
		finally 
		{ 
		  try { writer.close(); } catch ( Exception e ) { } 
		  try { reader.close(); } catch ( Exception e ) { } 
		}
		
//
//	   NetcdfFileWriteable writeableFile = NetcdfFileWriteable.createNew("/home/schubert/temp/test.nc");
//
//	   // define dimensions, including unlimited
//	   Dimension latDim = writeableFile.addDimension("lat", 3);
//	   Dimension lonDim = writeableFile.addDimension("lon", 4);
//	   Dimension timeDim = writeableFile.addUnlimitedDimension("time");
//
//	   // define Variables
//	   Dimension[] dim3 = new Dimension[3];
//	   dim3[0] = timeDim;
//	   dim3[1] = latDim;
//	   dim3[2] = lonDim;
//
//	   writeableFile.addVariable("lat", DataType.FLOAT, new Dimension[] {latDim});
//	   writeableFile.addVariableAttribute("lat", "units", "degrees_north");
//
//	   writeableFile.addVariable("lon", DataType.FLOAT, new Dimension[] {lonDim});
//	   writeableFile.addVariableAttribute("lon", "units", "degrees_east");
//
//	   writeableFile.addVariable("rh", DataType.INT, dim3);
//	   writeableFile.addVariableAttribute("rh", "long_name", "relative humidity");
//	   writeableFile.addVariableAttribute("rh", "units", "percent");
//
//	   writeableFile.addVariable("T", DataType.DOUBLE, dim3);
//	   writeableFile.addVariableAttribute("T", "long_name", "surface temperature");
//	   writeableFile.addVariableAttribute("T", "units", "degC");
//
//	   writeableFile.addVariable("time", DataType.INT, new Dimension[] {timeDim});
//	   writeableFile.addVariableAttribute("time", "units", "hours since 1990-01-01");
//
//	   // create the file
//	   writeableFile.create();
//	   
//	   Array timeData = Array.factory( DataType.INT, new int[] {1});
//	   
	   
}
}
