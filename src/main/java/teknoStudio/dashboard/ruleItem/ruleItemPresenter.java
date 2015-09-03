package teknoStudio.dashboard.ruleItem;

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

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import teknoStudio.dashboard.Rule;

import java.net.URL;
import java.util.ResourceBundle;

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
    Button deleteRuleButton;

    @FXML
    Text minSupportText;


    Rule thisRule;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //TODO: Add event handler to get name changes
    }

    public void setThisRule(Rule r){
        thisRule=r;
        updateViewInfo();
    }

    private  void updateViewInfo(){
        minSupportText.setText("@minSupport:"+thisRule.getMinSupport());
        ruleNameTextField.setText(thisRule.getName());
        ruleTextArea.setText(thisRule.getRule());
    }
}
