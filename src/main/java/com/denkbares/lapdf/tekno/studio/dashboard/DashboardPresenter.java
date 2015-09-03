package com.denkbares.lapdf.tekno.studio.dashboard;

/*
 * #%L
 * igniter
 * %%
 * Copyright (C) 2013 - 2014 Adam Bien
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import com.denkbares.lapdf.tekno.studio.dashboard.cssArea.CssAreaPresenter;
import com.denkbares.lapdf.tekno.studio.dashboard.cssArea.CssAreaView;
import com.denkbares.lapdf.tekno.studio.dashboard.pdfArea.PdfPresenter;
import com.denkbares.lapdf.tekno.studio.dashboard.pdfArea.PdfView;

import javax.inject.Inject;

/**
 *
 * @author Maximilian Schirm
 */
public class DashboardPresenter implements Initializable {

    @FXML
    Text pdfStatusText;

    @FXML
    TextField pageTextField;

    @FXML
    BorderPane pdfBorderPane;

    @FXML
    BorderPane cssBorderPane;

    @Inject
    Tower tower;


    private CssAreaView cssViewr;
    private CssAreaPresenter cssPresentr;
    private PdfView pdfViewr;
    private PdfPresenter pdfPresentr;

    private ArrayList<ChunkBlock> chunkBlockList;
    private int currentPageNo = 1;
    private String rules;
    private File currentRulePath;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        pdfViewr = new PdfView();
        pdfPresentr = (PdfPresenter) pdfViewr.getPresenter();
        pdfBorderPane.setCenter(pdfViewr.getView());

        cssViewr = new CssAreaView();
        cssPresentr = (CssAreaPresenter) cssViewr.getPresenter();
        cssBorderPane.setCenter((cssViewr.getView()));


        //Limit Text Field input to Digits
        pageTextField.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent inputevent) {
                if (!inputevent.getCharacter().matches("\\d")) {
                    inputevent.consume();
                }
            }
        });
        //Navigate on enter key
        pageTextField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setPage(Integer.parseInt(pageTextField.getText()));
            }
        });
    }

    public void setPage(int pNo) {
        //if(tower!=null && pNo < tower.getNumberOfPages() && pNo >= 1){
            currentPageNo = pNo;
            pdfPresentr.changePage(currentPageNo);

            updateBoxes();
            pageTextField.setText("" + currentPageNo);
        //}
    }

    public void forwardPage() {
        if(currentPageNo < 40){
            currentPageNo++;
            pdfPresentr.changePage(currentPageNo);

            updateBoxes();
            pageTextField.setText("" + currentPageNo);
        }
    }

    public void backPage() {
        if(currentPageNo > 1){
            currentPageNo--;
            pdfPresentr.changePage(currentPageNo);

            updateBoxes();
            pageTextField.setText("" + currentPageNo);
        }
    }

    public void beginningPage() {
            currentPageNo = 1;
            pdfPresentr.changePage(currentPageNo);

            updateBoxes();
            pageTextField.setText("" + currentPageNo);

    }

    public void endPage() {

            currentPageNo = 40;
            pdfPresentr.changePage(currentPageNo);

            updateBoxes();
            pageTextField.setText("" + currentPageNo);

    }

    /**
     * Fetches the ChunkBlocks from the Model and tells the PDFView to draw them as boxes
     */
    public void updateBoxes(){
        try {
            final ArrayList<ChunkBlock> currentChunkBlockList = tower.getChunkBlocksOfPage(currentPageNo);
            if (!chunkBlockList.equals(currentChunkBlockList)) {
                //TODO Tell jPedal to draw boxes
                for (ChunkBlock b : currentChunkBlockList) {
                    //Draw box. Red if unclassified, yellow if classified. If classified, add class as text or mouseover.
                    if (b.getWasClassified() == false) {
                        //TODO draw red border

                    } else {
                        //TODO draw yellow border and text

                    }
                }
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public void openFileMenuAction(){
        String newRules = "";
        FileChooser opener = new FileChooser();
        opener.setTitle("Open Rules File");
        opener.setInitialDirectory(new File(System.getProperty("user.home")));
        opener.getExtensionFilters().add(new FileChooser.ExtensionFilter("d3web File", "*.d3web"));
        File openedRules = opener.showOpenDialog(new Popup());
        try {
            newRules = readFile(openedRules.getPath(),Charset.forName("UTF-8"));
        }
        catch(Exception e){
            System.out.println("---------ERROR---------\nFailed to load File :"+openedRules.getAbsolutePath()+"\n"+e.getMessage());
            pdfStatusText.setText("Error reading file " + openedRules.getAbsolutePath());
        }
        if(correctlyFormatted(decode(newRules))) {
            currentRulePath = openedRules;
            rules = decode(newRules);
            //TODO replace with load method
            cssPresentr.addRuleToGrid(new Rule(rules,currentRulePath.getName()));
        }
        else{
            System.out.println("The file ;" + currentRulePath.getAbsolutePath() + " is not a valid rules file!");
            pdfStatusText.setText("The file ;" + currentRulePath.getAbsolutePath() + " is not a valid rules file!");
        }
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public void saveFileMenuAction(){
        /*if(correctlyFormatted(cssTextArea.getText())) {
            File fileCopy = currentRulePath;
            try {
                rules = cssTextArea.getText();
                currentRulePath.delete();
                PrintWriter writer = new PrintWriter(fileCopy.getAbsoluteFile(), "UTF-8");
                //TODO Encode!
                rules = encode(rules);
                writer.print(rules);
                writer.close();
                System.out.println("Saved successfully!");
                pdfStatusText.setText("Saved successfully!");
            } catch (Exception e) {
                System.out.println("-------ERROR-------\nFailed to save rules to file!\n" + e.getMessage());
                pdfStatusText.setText("Failed to save rules!");
            }
        }
        else{
            System.out.println("Rules are not correctly formatted - cannot save!");
            pdfStatusText.setText("Rules are not correctly formatted - cannot save!");
        }**/
    }

    public void saveAsFileMenuAction(){
        FileChooser saver = new FileChooser();
        saver.setTitle("Export Rules");
        saver.setInitialDirectory(new File(System.getProperty("user.home")));
        saver.getExtensionFilters().add(new FileChooser.ExtensionFilter("d3web File", ".d3web"));
        File file = saver.showSaveDialog(new Popup());
        /*if(correctlyFormatted(cssTextArea.getText())) {
            //TODO Encode!
            rules = encode(rules);
            currentRulePath = file;
            try {
                rules = cssTextArea.getText();
                PrintWriter writer = new PrintWriter(file.getAbsoluteFile(), "UTF-8");
                writer.print(rules);
                writer.close();
                System.out.println("Saved successfully!");
                pdfStatusText.setText("Saved successfully!");
            } catch (Exception e) {
                System.out.println("-------ERROR-------\nFailed to save rules to file!\n" + e.getMessage());
                pdfStatusText.setText("Failed to save rules!");
            }
        }
        else{
            System.out.println("Rules are not correctly formatted - cannot save!");
            pdfStatusText.setText("Rules are not correctly formatted - cannot save!");
        }**/

    }

    public void exitFileMenuAction(){
        //TODO : Close all and ask for saving?
        System.exit(0);
    }


    //TODO : Move opening and reclassifying to different thread!
    public void openPdfMenuAction(){
        //TODO: REMOVE COMMENTING #DEBUG

        //try{
            FileChooser opener = new FileChooser();
            opener.setTitle("Open PDF File");
            opener.setInitialDirectory(new File(System.getProperty("user.home")));
            opener.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF File", "*.pdf"));
            File openedPDF = opener.showOpenDialog(new Popup());
            //tower.setPdfPath(openedPDF.getAbsolutePath());
            //Tell jPedal to load PDF File
            pdfPresentr.loadPDFFile(openedPDF.getAbsolutePath());
            pdfPresentr.changePage(1);
            pdfStatusText.setText("Successfully loaded the File " + openedPDF.getName() + "!");
        /*}
        catch (IOException e){
            System.out.println("Failed to open PDF File!");
            pdfStatusText.setText("Opening PDF File failed! Please try again!");
            System.out.println("-------------ERROR------------\n" + e.getMessage());
        }**/
    }

    public void reclassifyPdfMenuAction(){
        try{
            tower.refreshLapdfDocument();
            pdfStatusText.setText("Document successfully reclassified!");
        } catch (IOException e) {
            e.printStackTrace();
            pdfStatusText.setText("Failed to reclassify!");
        }
    }

    public void exportPdfMenuAction(){
        FileChooser saver = new FileChooser();
        saver.setTitle("Export XML");
        saver.setInitialDirectory(new File(System.getProperty("user.home")));
        saver.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML File", ".xml"));
        File file = saver.showSaveDialog(new Popup());
        if(tower.exportAsXML(file.getAbsolutePath())) {
            System.out.println("Successfully exported XML to " + file.getAbsolutePath());
            pdfStatusText.setText("Successfully exported XML File!");
        }
        else{
            System.out.println("Failed to export XML.");
            pdfStatusText.setText("Failed to export XML.");
        }
    }

    //TODO : Implement!
    public boolean correctlyFormatted(String s){
        return true;
    }

    //TODO : Implement!
    public String decode(String s){
        return s;
    }

    //TODO : Implement!
    public String encode(String s){
        return s;
    }

}
