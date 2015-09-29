package com.denkbares.lapdf.tekno.studio;

import edu.isi.bmkeg.lapdf.controller.LapdfEngine;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import javafx.stage.FileChooser;
import javafx.stage.Popup;

import java.io.File;

/**
 * Created by Maximilian on 11.09.2015.
 */
public class DebugEngineMethods {

    File testFile;

    public DebugEngineMethods(){
        FileChooser opener = new FileChooser();
        opener.setTitle("Open PDF File");
        opener.setInitialDirectory(new File(System.getProperty("user.home")));
        opener.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF File", "*.pdf"));
        testFile = opener.showOpenDialog(new Popup());

        //tests, set breakpoint!
        debugBlockifyFile();
    }

    public void debugBlockifyFile(){
        try {
            LapdfEngine engineToTest = new LapdfEngine();
            LapdfDocument docResult = engineToTest.blockifyFile(testFile);
        }
        catch (Exception e){
            System.out.println("Error Occured at : debugBlockifyFile() " + e.getStackTrace());
        }

    }
}