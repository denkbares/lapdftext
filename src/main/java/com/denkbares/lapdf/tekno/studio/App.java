package com.denkbares.lapdf.tekno.studio;

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

import com.denkbares.lapdf.tekno.studio.dashboard.DashboardPresenter;
import com.denkbares.lapdf.tekno.studio.dashboard.DashboardView;

import com.airhacks.afterburner.injection.Injector;
import com.denkbares.lapdf.tekno.studio.dashboard.Focus;
import com.denkbares.lapdf.tekno.studio.dashboard.NavCommand;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 *
 * @author adam-bien.com
 */
public class App extends Application {


    @Override
    public void start(Stage stage) throws Exception {

        DashboardView appView = new DashboardView();
        DashboardPresenter appPresenter = (DashboardPresenter) appView.getPresenter();
        Scene scene = new Scene(appView.getView());


        stage.setTitle("TEKNO Studio");
        final String uri = getClass().getResource("app.css").toExternalForm();
        scene.getStylesheets().add(uri);
        stage.setScene(scene);
        stage.show();

        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                //Focus shortcuts
                if(event.getText().equalsIgnoreCase("R") && event.isControlDown()){
                    event.consume();
                    appPresenter.setFocus(Focus.RULE_NAME_FIELD);
                }
                if(event.getText().equalsIgnoreCase("c") && event.isControlDown()){
                    event.consume();
                    appPresenter.setFocus(Focus.PAGE_NO_FIELD);
                }

                //Page Nav
                if(event.getCode().equals(KeyCode.LEFT) && event.isControlDown()){
                    appPresenter.navCommand(NavCommand.GOBACK);
                }
                if(event.getCode().equals(KeyCode.RIGHT) && event.isControlDown()){
                    appPresenter.navCommand(NavCommand.GOFORWARD);
                }
                if(event.getCode().equals(KeyCode.LEFT) && event.isAltDown()){
                    appPresenter.navCommand(NavCommand.BEGINNING);
                }
                if(event.getCode().equals(KeyCode.RIGHT) && event.isAltDown()){
                    appPresenter.navCommand(NavCommand.END);
                }
            }
        });
    }

    @Override
    public void stop() throws Exception {
        Injector.forgetAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}