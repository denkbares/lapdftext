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
import java.util.ResourceBundle;

import com.denkbares.lapdf.tekno.studio.dashboard.DashboardPresenter;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.jpedal.PdfDecoderFX;
import org.jpedal.examples.baseviewer.BaseViewerFX;
import org.jpedal.exception.PdfException;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //Load OpenViewer (jPedal)
        pdfDecoder = new org.jpedal.PdfDecoderFX();
        viewerBase = new BaseViewerFX();
        StackPane stackingBox = new StackPane();
        boxesOverlay = new Canvas();
        stackingBox.getChildren().addAll(pdfDecoder, boxesOverlay);
        pdfPane.setCenter(stackingBox);


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
                if(Integer.parseInt(pageTextField.getText()) > dbp.getTower().getNumberOfPages()){
                    pageTextField.setText(""+dbp.getTower().getNumberOfPages());
                    changePage(dbp.getTower().getNumberOfPages());
                }
                else
                    changePage(Integer.parseInt(pageTextField.getText()));
            }
        });
    }

    public boolean loadPDFFile(String dir) {
        try {
            pdfDecoder.clearScreen();
            pdfDecoder.openPdfFile(dir);
            currentPageNo = 1;
            fitToX(FitToPage.AUTO);

            pdfPane.getCenter().toBack();

            return true;
        } catch (PdfException e) {
            System.out.println("---------ERROR---------\nIn module pdfArea\n" + e.getMessage());
            return false;
        }
    }

    public void clearScreen() {
        currentPageNo = 1;
        pdfDecoder.closePdfFile();
    }

    public void changePage(int pageNew) {
        try {
            if(setPage(pageNew)) {
                dbp.setPage(pageNew);
                boxesOverlay.getGraphicsContext2D().clearRect(0,0,boxesOverlay.getWidth(),boxesOverlay.getHeight());
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
        if(dbp.getTower()!=null && pNo <= dbp.getTower().getNumberOfPages() && pNo >= 1){
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
        if(currentPageNo < dbp.getTower().getNumberOfPages()){
            currentPageNo++;
            changePage(currentPageNo);

            dbp.updateBoxes();
            pageTextField.setText("" + currentPageNo);
        }
    }

    public void backPage() {
        if(currentPageNo > 1){
            currentPageNo--;
            changePage(currentPageNo);

            dbp.updateBoxes();
            pageTextField.setText("" + currentPageNo);
        }
    }

    public void beginningPage() {
        currentPageNo = 1;
        changePage(currentPageNo);

        dbp.updateBoxes();
        pageTextField.setText("" + currentPageNo);

    }

    public void endPage() {

        currentPageNo = dbp.getTower().getNumberOfPages();
        changePage(currentPageNo);

        dbp.updateBoxes();
        pageTextField.setText("" + currentPageNo);

    }

}