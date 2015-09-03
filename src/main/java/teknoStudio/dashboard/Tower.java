package teknoStudio.dashboard;

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

import edu.isi.bmkeg.lapdf.controller.LapdfEngine;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author adam-bien.com
 */
public class Tower {

    //LaPDFDocument
    LapdfDocument doc;
    //LapdfEngine
    LapdfEngine engine;

    String pdfPath;
    String rulePath;

    @PostConstruct
    public void init() {
        try {
//            engine = new LapdfEngine();
        }
        catch(Exception e){
            System.out.println("ERROR : Failed to initialize the Tower!");
            System.out.println("----------------------EXCEPTION-------------------\n" + e.getMessage());
        }

        pdfPath = "";
        rulePath = "";

    }

    public void setPdfPath(String pdfPath) throws IOException{
        if(! (this.pdfPath == pdfPath)) {
            this.pdfPath = pdfPath;
            try{
                refreshLapdfDocument();
            }
            catch(IOException e){
                System.out.println("Cannot access selected File! Please retry!");
                throw new IOException(e.getMessage());
            }
        }
    }

    public void setRulePath(String rulePath){
        //TODO Make sure that a valid path was given
        this.rulePath = rulePath;
    }

    public void refreshLapdfDocument() throws IOException{
        try{
            doc = engine.blockifyFile(new File(pdfPath));
        }
        catch(Exception e){
            throw new IOException("Failed to refresh the LaPDFDocument");
        }
    }

    public boolean exportAsXML(String dir){
        if(dir == null){
            return false;
        }
        else{
            try{
                engine.writeSpatialXmlToFile(doc, new File(dir));
                return true;
            }
            catch(Exception e){
                System.out.println("--------ERROR--------\n Failed to save XML File!"+e.getMessage());
                return false;
            }
        }
    }

    public ArrayList<ChunkBlock> getChunkBlocksOfPage(int pageNo) throws IllegalStateException, IOException {
        if(doc == null)
            throw new IllegalStateException("A page needs to be loaded first in order to retrieve ChunkBlocks!");
        else
            return new ArrayList<ChunkBlock>(doc.readAllChunkBlocks());
    }

    public int getNumberOfPages(){
        if(doc != null) {
            return doc.getTotalNumberOfPages();
        }
        else return 0;
    }
}
