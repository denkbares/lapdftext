package edu.isi.bmkeg.lapdf.bin;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import edu.isi.bmkeg.lapdf.controller.LapdfEngine;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.parser.MaxPowerChunkingClass;
import edu.isi.bmkeg.utils.Converters;
import javafx.application.Application;
import javafx.stage.Stage;

public class ImagifyBlocks extends Application {

	private static String USAGE = "usage: <input-dir-or-file> [<output-dir>] \n\n"
			+ "<input-dir-or-file> - the full path to the PDF file or directory to be extracted \n"
			+ "<output-dir> (optional or '-') - the full path to the output directory \n\n"
			+ "Running this command on a PDF file or directory will attempt to generate \n"
			+ "one image per page with text chunks drawn out with labels describing \n"
			+ "the predominant Font + Style of each block. This is helpful in developing\n"
			+ "rule files.\n";

	public static void main(String args[]) throws Exception {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		LapdfEngine engine = new LapdfEngine();

		//NOTE : Here we can set which parser to use
		//Comment the line for using the default parser
		engine.setParser(new MaxPowerChunkingClass());

		List<String> parameters = getParameters().getRaw();
		String[] args = parameters.toArray(new String[parameters.size()]);

		if (args.length < 1) {
			System.err.println(USAGE);
			System.exit(1);
		}

		String inputFileOrDirPath = args[0];
		String outputDirPath = "";

		File inputFileOrDir = new File(inputFileOrDirPath);
		if (!inputFileOrDir.exists()) {
			System.err.println(USAGE);
			System.err.println("Input file / dir '" + inputFileOrDirPath + "' does not exist.");
			System.err.println("Please include full path");
			System.exit(1);
		}

		// output folder is set.
		if (args.length > 1) {
			outputDirPath = args[1];
		}
		else {
			outputDirPath = "-";
		}

		if (outputDirPath.equals("-")) {
			if (inputFileOrDir.isDirectory()) {
				outputDirPath = inputFileOrDirPath;
			}
			else {
				outputDirPath = inputFileOrDir.getParent();
			}
		}

		File outDir = new File(outputDirPath);
		if (!outDir.exists()) {
			outDir.mkdir();
		}

		if (inputFileOrDir.isDirectory()) {

			Pattern patt = Pattern.compile("\\.pdf$");
			Map<String, File> inputFiles = Converters.recursivelyListFiles(inputFileOrDir, patt);
			Iterator<String> it = inputFiles.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				File pdf = inputFiles.get(key);
				String pdfStem = pdf.getName();
				pdfStem = pdfStem.replaceAll("\\.pdf", "");

				String outImgPath = Converters.mimicDirectoryStructure(inputFileOrDir, outDir, pdf)
						.getPath();
				outImgPath = outImgPath.replaceAll("\\.pdf", "_blockImgs");

				File outImgDir = new File(outImgPath);
				if (!outImgDir.exists())
					outImgDir.mkdir();

				try {

					LapdfDocument lapdf = engine.blockifyFile(pdf);
					engine.dumpWordOrderImageOutlinesToFiles(lapdf, outImgDir, pdfStem);

				}
				catch (Exception e) {

					e.printStackTrace();

				}

			}

		}
		else {

			String pdfStem = inputFileOrDir.getName();
			pdfStem = pdfStem.replaceAll("\\.pdf$", "");

			String outImgPath = outDir.getPath() + "/" + pdfStem + "_blockImgs";
			File outImgDir = new File(outImgPath);
			if (!outImgDir.exists())
				outImgDir.mkdir();

			LapdfDocument lapdf = engine.blockifyFile(inputFileOrDir);
			engine.dumpWordOrderImageOutlinesToFiles(lapdf, outImgDir, pdfStem);
			System.exit(0);

		}
	}
}
