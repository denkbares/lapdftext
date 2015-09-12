package com.denkbares.lapdf.tekno.studio.dashboard.ruleItem;

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

import com.denkbares.lapdf.tekno.studio.dashboard.cssArea.CssAreaPresenter;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import com.denkbares.lapdf.tekno.studio.dashboard.Rule;

/**
 *
 * @author Maximilian Schirm
 */
public class RuleItemPresenter implements Initializable {


    @FXML
    TextArea ruleTextArea;

    @FXML
    TextField ruleNameTextField;

    @FXML
    TextField minSupportTextField;

    @FXML
    Button deleteRuleButton;

    @FXML
    Text minSupportText;

    CssAreaPresenter controller;

    Rule thisRule;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //TODO: Add event handler to get name changes
        //Navigate on enter key
        ruleNameTextField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                thisRule.name=ruleNameTextField.getText();
                System.out.println("Saved new Rule name : " + thisRule.getName());
            }
        });

        minSupportTextField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                thisRule.setMinSupport(Double.parseDouble(minSupportTextField.getText()));
                System.out.println("Saved new Rule support : " + thisRule.getMinSupport());
            }
        });
        //TODO : Add Filter for Accepting only valid values for Min Support, handle exceptions
    }

    public void setSuper(CssAreaPresenter boss){
        controller = boss;
    }

    public void deleteRuleButtonPressed(){
        controller.deleteRule(this);
    }

    public Rule getRule(){
        return thisRule;
    }

    public void setThisRule(Rule r){
        thisRule=r;
        updateViewInfo();
    }

    private  void updateViewInfo(){
        minSupportTextField.setText(""+thisRule.getMinSupport());
        ruleNameTextField.setText(thisRule.getName());
        ruleTextArea.setText(thisRule.getRule());
    }
}
