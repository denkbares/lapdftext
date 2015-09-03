package teknoStudio.dashboard.pdfArea;

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

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import java.awt.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import org.jpedal.PdfDecoderFX;
import org.jpedal.examples.baseviewer.BaseViewerFX;
import org.jpedal.examples.viewer.OpenViewerFX;
import org.jpedal.exception.PdfException;

import javax.inject.Inject;

/**
 *
 * @author airhacks.com
 */
public class PdfPresenter implements Initializable {

    @FXML
    AnchorPane pdfPane;

    PdfDecoderFX pdfDecoder;
    BaseViewerFX viewerBase;
    int currentPage = 1;
    float scale = 1.0f;
    float insetX, insetY = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //Load OpenViewer (jPedal)
        pdfDecoder = new org.jpedal.PdfDecoderFX();
        viewerBase = new BaseViewerFX();
        Group temp = new Group();
        temp.getChildren().add(pdfDecoder);
        pdfPane.getChildren().add(temp);
    }

    public boolean loadPDFFile(String dir) {
        try {
            pdfDecoder.clearScreen();
            pdfDecoder.openPdfFile(dir);
            currentPage = 1;
            fitToX(FitToPage.AUTO);
            return true;
        } catch (PdfException e) {
            System.out.println("---------ERROR---------\nIn module pdfArea\n" + e.getMessage());
            return false;
        }
    }

    public void clearScreen() {
        currentPage = 1;
        pdfDecoder.closePdfFile();
    }

    public void changePage(int pageNew) {
        try {
            pdfDecoder.decodePage(pageNew);
            pdfDecoder.waitForDecodingToFinish();
            currentPage = pageNew;
            fitToX(FitToPage.AUTO);
        } catch (Exception e) {
            System.out.println("--------ERROR-------\nFailed to decode page!\n" + e.getMessage());
        }
    }

    public void drawOnPage(ChunkBlock what) {
        int currentPageNumber = pdfDecoder.getPageNumber();
        int widthBox = Math.abs(what.getX2() - what.getX1());
        int heightBox = Math.abs(what.getY2() - what.getY1());

        Color boxColor = Color.YELLOW;
        if (what.getWasClassified()) {
            boxColor = Color.RED;
        }

        Rectangle chunkBox = new Rectangle(what.getX1(), what.getY1(), widthBox, heightBox);
        chunkBox.setStrokeWidth(2.0);

        int[] type = {0};
        Color[] colors = {boxColor};
        Object[] objects = {chunkBox};

        try {
            pdfDecoder.drawAdditionalObjectsOverPage(currentPageNumber, type, colors, objects);
        } catch (Exception e) {
            System.out.println("-----ERROR-----\nCould not draw onto PDF!\n" + e.getMessage());
        }
    }

    //-------------------------------------------
    public enum FitToPage{
        AUTO, WIDTH, HEIGHT, NONE
    }

    private void fitToX(final FitToPage fitToPage) {
        if(fitToPage == FitToPage.NONE) {
            return;
        }

        final float pageW=pdfDecoder.getPdfPageData().getCropBoxWidth2D(currentPage);
        final float pageH=pdfDecoder.getPdfPageData().getCropBoxHeight2D(currentPage);
        final int rotation = pdfDecoder.getPdfPageData().getRotation(currentPage);

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

        pdfDecoder.setPageParameters(scale, currentPage);
    }

}