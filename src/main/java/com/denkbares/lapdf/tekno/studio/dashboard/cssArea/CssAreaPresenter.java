package com.denkbares.lapdf.tekno.studio.dashboard.cssArea;

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
import java.util.Collection;
import java.util.Comparator;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import com.denkbares.lapdf.tekno.studio.dashboard.Rule;
import com.denkbares.lapdf.tekno.studio.dashboard.ruleItem.RuleItemView;
import com.denkbares.lapdf.tekno.studio.dashboard.ruleItem.RuleItemPresenter;
import javafx.scene.layout.VBox;

/**
 *
 * @author Maximilian Schirm
 */
public class CssAreaPresenter implements Initializable {

    @FXML
    BorderPane cssAreaBorderPane;

    @FXML
    Button addRuleButton;

    @FXML
    TextField ruleNameTextField;

    @FXML
    VBox rulesVBox;

    ArrayList<Integer> filledRows;
    ArrayList<Rule> rulesList;
    ArrayList<RuleItemView> rulesViewList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rulesList = new ArrayList<Rule>();
        rulesViewList = new ArrayList<RuleItemView>();
        filledRows = new ArrayList<Integer>();

        ruleNameTextField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                addRuleButtonPressed();
            }
        });
    }

    public Collection<Rule> getRules(){
        return rulesList;
    }

    public void addRuleButtonPressed(){
        //For debugging only
        if(ruleNameTextField.getText() == ""){
            Rule temp = new Rule("Gruetze", "Rule"+rulesList.size());
            addRuleToGrid(temp);
            rulesList.add(temp);
        }
        else {
            Rule temp = new Rule("Gruetze", ruleNameTextField.getText());
            addRuleToGrid(temp);
            rulesList.add(temp);
        }
    }

    public void addRuleToGrid(Rule r){
        RuleItemView tempRuleView = new RuleItemView();
        rulesViewList.add(tempRuleView);
        RuleItemPresenter tempRulePresenter = (RuleItemPresenter)tempRuleView.getPresenter();
        tempRulePresenter.setThisRule(r);
        tempRulePresenter.setSuper(this);
        tempRulePresenter.setIndex(rulesVBox.getChildren().size());

        rulesVBox.getChildren().add(tempRuleView.getView());
    }

    public void deleteRule(RuleItemPresenter rule){
        rulesList.remove(rule.getRule());
        if(rule.getIndex() == 0 && rulesVBox.getChildren().size() <= 1)
            rulesVBox.getChildren().remove(1);
        else
            rulesVBox.getChildren().remove(rule.getIndex());
    }

    private int getLastEmptyRow(){
        filledRows.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if(o1>o2)
                    return 1;
                else if(o2>o1)
                    return -1;
                else
                    return 0;
            }
        });
        int lastIndex = 0;
        for(int i : filledRows){
            if(Math.abs(i-lastIndex) >= 2)
                return i-1;
            lastIndex = i;
        }
        return lastIndex+1;
    }


}
