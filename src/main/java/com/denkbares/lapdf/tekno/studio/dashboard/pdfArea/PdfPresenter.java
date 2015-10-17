package com.denkbares.lapdf.tekno.studio.dashboard.pdfArea;

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


import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import com.denkbares.lapdf.tekno.studio.dashboard.DashboardPresenter;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import javafx.fxml.Initializable;
import javafx.scene.canvas.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.apache.pdfbox.PDFDebugger;
import org.jpedal.PdfDecoderFX;
import org.jpedal.examples.baseviewer.BaseViewerFX;
import org.jpedal.exception.PdfException;
import org.jpedal.fonts.FontMappings;


/**
 *
 * @author Maximilian Schirm
 */
public class PdfPresenter implements Initializable {

    @FXML
    BorderPane pdfPane;

    PdfDecoderFX pdfDecoder;
    BaseViewerFX viewerBase;
    int currentPageNo = 1;
    float scale = 1.0f;
    float insetX, insetY = 10;
    DashboardPresenter dbp;
    Canvas boxesOverlay;
    boolean selectionMode = false;
    boolean wasInit = false;
    ArrayList<ChunkBlock> currentSelection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //Load OpenViewer (jPedal)
        pdfDecoder = new org.jpedal.PdfDecoderFX();
        FontMappings.setFontReplacements();
        pdfDecoder.setExtractionMode(PdfDecoderFX.TEXT);

        viewerBase = new BaseViewerFX();
        StackPane stackingBox = new StackPane();
        boxesOverlay = new Canvas();
        stackingBox.getChildren().addAll(pdfDecoder, boxesOverlay);
        pdfPane.setCenter(stackingBox);
        currentSelection = new ArrayList<ChunkBlock>();

        //Get Mouse clicks on canvas
        boxesOverlay.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(selectionMode){
                    if(event.getButton().equals(MouseButton.PRIMARY)){
                        //Obtain the ChunkBlock
                        for(ChunkBlock b : dbp.getCurrentChunkBlockList()){
                            if(wasClickedOn(b, event)){
                                currentSelection.add(b);
                                drawSelectOnPage(b);
                                System.out.println("Added the block " + b + "to current selection.");
                                break;
                            }
                        }
                    }
                    if(event.getButton().equals(MouseButton.SECONDARY)){
                        //Obtain the ChunkBlock, Remove it from selection
                        for(ChunkBlock b : dbp.getCurrentChunkBlockList()){
                            if(wasClickedOn(b, event)){
                                currentSelection.remove(b);
                                undrawSelectOnPage(b);
                                System.out.println("Removed the block " + b + "from current selection.");
                                break;
                            }
                        }
                    }
                }
            }
        });

        //Limit Text Field input to Digits
        pageTextField.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent inputevent) {
                if (!inputevent.getCharacter().matches("\\d")) {
                    inputevent.consume();
                }
            }
        });
        //Navigate to page on enter key
        pageTextField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(Integer.parseInt(pageTextField.getText()) > dbp.getTower().getNumberOfPages()){
                    pageTextField.setText(""+dbp.getTower().getNumberOfPages());
                    changePage(dbp.getTower().getNumberOfPages());
                }
                else
                    changePage(Integer.parseInt(pageTextField.getText()));
            }
        });
    }

    public void setSelectionMode(boolean b){
        clearSelectionOverlay();
        currentSelection = new ArrayList<ChunkBlock>();
        selectionMode = b;
    }

    public boolean getSelectionMode(){
        return selectionMode;
    }

    protected boolean wasClickedOn(ChunkBlock b, MouseEvent e){
        //Tells wheter a box was clicked upon by the MouseEvent
        //Generate drawn dimensions
        float x1,x2,y1,y2;
        x1 = b.getX1()*scale;
        x2 = b.getX2()*scale;
        y1 = b.getY1()*scale;
        y2 = b.getY2()*scale;
        //Determine if MouseClick was inside
        if(x1 <= e.getX() && e.getX() <= x2){
            if(y1 <= e.getY() && e.getY() <= y2)
                return true;
        }
        return false;
    }

    public boolean loadPDFFile(String dir) {
        try {
            pdfDecoder.openPdfFile(dir);
            currentPageNo = 1;
            fitToX(FitToPage.AUTO);

            //Necessary to control for overlap on first load
            if(!wasInit) {
                pdfPane.getCenter().toBack();
                wasInit = true;
            }

            return true;
        } catch (PdfException e) {
            System.out.println("---------ERROR---------\nIn module pdfArea\n" + e.getMessage());
            return false;
        }
    }

    public void clearSelectionOverlay(){
        boxesOverlay.getGraphicsContext2D().clearRect(0,0,boxesOverlay.getWidth(),boxesOverlay.getHeight());
        dbp.updateBoxes();
    }

    public void clearScreen() {
        currentPageNo = 1;
        pdfDecoder.closePdfFile();
    }

    public void changePage(int pageNew) {
        try {
            if(setPage(pageNew)) {
                dbp.setPage(pageNew);
                clearSelectionOverlay();
                boxesOverlay.setHeight(pdfDecoder.getHeight());
                boxesOverlay.setWidth(pdfDecoder.getWidth());
                dbp.updateBoxes();
            }
            else
                System.out.println("Illegal Page number passed for changePage()");
        } catch (Exception e) {
            System.out.println("--------ERROR-------\nFailed to decode page!\n" + e.getMessage());
        }
    }

    public void undrawSelectOnPage(ChunkBlock what) {
        float widthBox = scale * Math.abs(what.getX2()+8 - what.getX1());
        float heightBox = scale * Math.abs(what.getY2()+8 - what.getY1());

        boxesOverlay.getGraphicsContext2D().clearRect(scale*what.getX1()-3, scale*what.getY1()-3, widthBox, heightBox);
        drawOnPage(what);
    }

    public void drawSelectOnPage(ChunkBlock what) {

        float widthBox = scale * Math.abs(what.getX2()+4 - what.getX1());
        float heightBox = scale * Math.abs(what.getY2()+4 - what.getY1());

        //Decide which color box gets
        Color boxColor = Color.ORANGE;

        boxesOverlay.getGraphicsContext2D().setLineWidth(2.0);
        boxesOverlay.getGraphicsContext2D().setStroke(boxColor);
        boxesOverlay.getGraphicsContext2D().strokeRect(scale*what.getX1()-2, scale*what.getY1()-2, widthBox, heightBox);

        System.out.println("Drawn selection with x1:"+scale*what.getX1()+", y1:"+scale*what.getY1()+", h:"+scale*what.getHeight()+" and w:"+scale*what.getWidth()+" in Color "+boxColor.toString()+".");
    }

    public void drawOnPage(ChunkBlock what) {

        float widthBox = scale * Math.abs(what.getX2() - what.getX1());
        float heightBox = scale * Math.abs(what.getY2() - what.getY1());

        //Decide which color box gets
        Color boxColor = Color.YELLOW;
        if (what.getWasClassified()) {
            boxColor = Color.RED;
        }

        boxesOverlay.getGraphicsContext2D().setLineWidth(2.0);
        boxesOverlay.getGraphicsContext2D().setStroke(boxColor);
        boxesOverlay.getGraphicsContext2D().strokeRect(scale*what.getX1(), scale*what.getY1(), widthBox, heightBox);

        System.out.println("Drawn box with x1:"+scale*what.getX1()+", y1:"+scale*what.getY1()+", h:"+scale*what.getHeight()+" and w:"+scale*what.getWidth()+" in Color "+boxColor.toString()+".");
    }

    //-------------------------------------------
    public enum FitToPage{
        AUTO, WIDTH, HEIGHT, NONE
    }

    private void fitToX(final FitToPage fitToPage) {
        if(fitToPage == FitToPage.NONE) {
            return;
        }

        final float pageW=pdfDecoder.getPdfPageData().getCropBoxWidth2D(currentPageNo);
        final float pageH=pdfDecoder.getPdfPageData().getCropBoxHeight2D(currentPageNo);
        final int rotation = pdfDecoder.getPdfPageData().getRotation(currentPageNo);

        //Handle how we auto fit the content to the page
        if(fitToPage == FitToPage.AUTO && (pageW < pageH)){
            if(pdfDecoder.getPDFWidth()<pdfDecoder.getPDFHeight()) {
                fitToX(FitToPage.HEIGHT);
            }
            else {
                fitToX(FitToPage.WIDTH);
            }
        }

        //Handle how we fit the content to the page width or height
        if(fitToPage == FitToPage.WIDTH){
            final float width=(float) (pdfPane.getWidth());
            if(rotation==90 || rotation==270){
                scale = (width - insetX - insetX) / pageH;
            }else{
                scale = (width - insetX - insetX) / pageW;
            }
        }else if(fitToPage == FitToPage.HEIGHT){
            final float height=(float) (pdfPane.getHeight()-40);

            if(rotation==90 || rotation==270){
                scale = (height - insetY - insetY) / pageW;
            }else{
                scale = (height - insetY - insetY) / pageH;
            }
        }

        pdfDecoder.setPageParameters(scale, currentPageNo);
    }

    public void setDashboardPresenter(DashboardPresenter dbp){
        this.dbp = dbp;
    }

    public void setStatus(String status){
        pdfStatusText.setText(status);
    }

    //Merged Methods and Variables, formerly part of DashboardPresenter
    //FXML
    @FXML
    Text pdfStatusText;

    @FXML
    javafx.scene.control.TextField pageTextField;

    //Variables
    private String rules;

    public boolean setPage(int pNo) {
        if(dbp.isPDFLoaded() && pNo <= dbp.getTower().getNumberOfPages()+1 && pNo >= 1){
            currentPageNo = pNo;
            pageTextField.setText("" + currentPageNo);
            pdfDecoder.decodePage(currentPageNo);
            pdfDecoder.waitForDecodingToFinish();
            fitToX(FitToPage.AUTO);
            return true;
        }
        return false;
    }

    public void forwardPage() {
        if(dbp.isPDFLoaded() && currentPageNo <= dbp.getTower().getNumberOfPages()){
            currentPageNo++;
            changePage(currentPageNo);

            dbp.updateBoxes();
            pageTextField.setText("" + currentPageNo);
        }
    }

    public void backPage() {
        if(dbp.isPDFLoaded() && currentPageNo > 1){
            currentPageNo--;
            changePage(currentPageNo);

            dbp.updateBoxes();
            pageTextField.setText("" + currentPageNo);
        }
    }

    public void beginningPage() {
        if(dbp.isPDFLoaded()) {
            currentPageNo = 1;
            changePage(currentPageNo);

            dbp.updateBoxes();
            pageTextField.setText("" + currentPageNo);
        }
    }

    public void endPage() {
        if(dbp.isPDFLoaded()) {
            currentPageNo = dbp.getTower().getNumberOfPages()+1;
            changePage(currentPageNo);

            dbp.updateBoxes();
            pageTextField.setText("" + currentPageNo);
        }
    }

    public ArrayList<ChunkBlock> getSelectedBlocks(){
        return currentSelection;
    }

}