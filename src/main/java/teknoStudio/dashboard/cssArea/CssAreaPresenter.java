package teknoStudio.dashboard.cssArea;

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
import java.util.Comparator;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import teknoStudio.dashboard.Rule;
import teknoStudio.dashboard.ruleItem.RuleItemPresenter;
import teknoStudio.dashboard.ruleItem.RuleItemView;

/**
 *
 * @author airhacks.com
 */
public class CssAreaPresenter implements Initializable {

    @FXML
    Button addRuleButton;

    @FXML
    TextField ruleNameTextField;

    @FXML
    GridPane rulesGridPane;

    ArrayList<Integer> filledRows;
    ArrayList<Rule> rulesList;
    ArrayList<RuleItemView> rulesViewList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rulesList = new ArrayList<Rule>();
        rulesViewList = new ArrayList<RuleItemView>();
        filledRows = new ArrayList<Integer>();
    }

    public void addRuleButtonPressed(){
        //For debugging only
        addRuleToGrid(new Rule("Gruetze", ruleNameTextField.getText()));

    }

    public void addRuleToGrid(Rule r){
        RuleItemView tempRuleView = new RuleItemView();
        rulesViewList.add(tempRuleView);
        RuleItemPresenter tempRulePresenter = (RuleItemPresenter)tempRuleView.getPresenter();
        tempRulePresenter.setThisRule(r);

        //Add to Grid
        if(rulesGridPane.getChildren() != null) {
            if (rulesGridPane.getChildren().size() <= filledRows.size()) {
                //Add new Row
                rulesGridPane.addRow(rulesGridPane.getChildren().size() + 1, tempRuleView.getView());
                filledRows.add(rulesGridPane.getChildren().size()+1);
            } else {
                rulesGridPane.add(tempRuleView.getView(), 0, getLastEmptyRow());
                filledRows.add(getLastEmptyRow());
            }
        }
        else{
            rulesGridPane.add(tempRuleView.getView(), 0, 0);
        }
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
