package edu.isi.bmkeg.lapdf.bin;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import edu.isi.bmkeg.lapdf.controller.LapdfEngine;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.utils.Converters;

public class DebugLapdfFeatures {

	private static String USAGE = "usage: <input-dir-or-file> [<output-dir>] [<rule-file>]\n\n"
			+ "<input-dir-or-file> - the full path to the PDF file or directory to be extracted \n" 
			+ "<output-dir> (optional or '-') - the full path to the output directory \n" 
			+ "<rule-file> (optional or '-') - the full path to the rule file \n\n" 
			+ "Running this command on a PDF file or directory will generate \n"
			+ "a CSV file for the PDF that can act as a template for rule files. " 
			+ "All available features are listed .\n";

	public static void main(String args[]) throws Exception	{

		LapdfEngine engine = new LapdfEngine();

		if (args.length < 1 ) {
			System.err.println(USAGE);
			System.exit(1);
		}
		
		String inputFileOrDirPath = args[0];
		String outputDirPath = "";
		String ruleFilePath = "";
							
		File inputFileOrDir = new File( inputFileOrDirPath ); 
		if( !inputFileOrDir.exists() ) {
			System.err.println(USAGE);
			System.err.println("Input file / dir '" + inputFileOrDirPath + "' does not exist.");
			System.err.println("Please include full path");
			System.exit(1);
		}
		
		// output folder is set.
		if ( args.length > 1 ) {	
			outputDirPath = args[1];
		} else {
			outputDirPath = "-";
		}
		
		if( outputDirPath.equals( "-") ) {
			if( inputFileOrDir.isDirectory() ) {
				outputDirPath = inputFileOrDirPath;
			} else {
				outputDirPath = inputFileOrDir.getParent();				
			}
		}
		
		File outDir = new File( outputDirPath ); 
		if( !outDir.exists() ) {
			outDir.mkdir();
		}  

		// output folder is set.
		File ruleFile =  null;
		if ( args.length > 2 ) {	
			ruleFilePath = args[2];
		} else {
			ruleFilePath = "-";
		}
		
		if( ruleFilePath.equals( "-" ) ) {
			ruleFile = Converters.extractFileFromJarClasspath(".", "rules/general.drl");
		} else {
			ruleFile = new File( ruleFilePath );
		}
		
		if( !ruleFile.exists() ) {
			System.err.println(USAGE);
			System.err.println(ruleFilePath + " does not exist.");
			System.err.println("Please include full path");
		}  
		
		if( inputFileOrDir.isDirectory() ){

			Pattern patt = Pattern.compile("\\.pdf$");
			Map<String, File> inputFiles = Converters.recursivelyListFiles(inputFileOrDir, patt);
			Iterator<String> it = inputFiles.keySet().iterator();
			while( it.hasNext() ) {
				String key = it.next();
				File pdf = inputFiles.get(key);
				String pdfStem = pdf.getName();
				pdfStem = pdfStem.replaceAll("\\.pdf", "");
	
				String outPath = Converters.mimicDirectoryStructure(inputFileOrDir, outDir, pdf).getPath();
								
				String outXmlPath = outPath + "_openAccess.xml";
				File outXmlFile = new File(outXmlPath);
				
				String outCsvPath = outPath + "_features.csv";
				File outCsvFile = new File(outCsvPath);

				String outImgPath = outPath + "_secImgs";
				File outImgDir = new File(outImgPath);
				
				if( !outImgDir.exists() )
					outImgDir.mkdirs();
												
				try {
	
					LapdfDocument lapdf = engine.blockifyFile(pdf);
					engine.classifyDocument(lapdf, ruleFile);
					
					//engine.writeSectionsToOpenAccessXmlFile(lapdf, outXmlFile);
					engine.dumpChunkTypeImageOutlinesToFiles(lapdf, outImgDir, pdfStem);
					engine.dumpFeaturesToSpreadsheet(lapdf, outCsvFile);
				
				} catch (Exception e) {
				
					e.printStackTrace();
				
				}
				
			} 
			
		} else {
			
			String pdfStem = inputFileOrDir.getName();
			pdfStem = pdfStem.replaceAll("\\.pdf$", "");
			
			String outPath = outDir.getPath() + "/" + pdfStem;
			
			String outXmlPath = outPath + "_openAccess.xml";
			File outXmlFile = new File(outXmlPath);
			
			String outCsvPath = outPath + "_features.csv";
			File outCsvFile = new File(outCsvPath);

			String outImgPath = outPath + "_secImgs";
			File outImgDir = new File(outImgPath);
			
			if( !outImgDir.exists() )
				outImgDir.mkdirs();
			
			LapdfDocument lapdf = engine.blockifyFile(inputFileOrDir);
			engine.classifyDocument(lapdf, ruleFile);

			//engine.writeSectionsToOpenAccessXmlFile(lapdf, outXmlFile);
			engine.dumpChunkTypeImageOutlinesToFiles(lapdf, outImgDir, pdfStem);
			engine.dumpFeaturesToSpreadsheet(lapdf, outCsvFile);
			
		}
	
	}
	
}
