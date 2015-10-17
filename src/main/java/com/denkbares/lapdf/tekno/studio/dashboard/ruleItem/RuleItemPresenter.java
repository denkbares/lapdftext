package com.denkbares.lapdf.tekno.studio.dashboard.ruleItem;

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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    int index;
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
                if(Double.parseDouble(minSupportTextField.getText()) > 1){
                    System.out.println("Entered too much for minSupport, set it back to max!");
                    thisRule.setMinSupport(1);
                }
                else {
                    thisRule.setMinSupport(Double.parseDouble(minSupportTextField.getText()));
                    System.out.println("Saved new Rule support : " + thisRule.getMinSupport());
                }
            }
        });

        minSupportTextField.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent inputevent) {
                if (!inputevent.getCharacter().matches("\\d") || !(inputevent.getCode().equals(KeyCode.PERIOD) && !minSupportTextField.getText().contains("."))) {
                    inputevent.consume();
                }
            }
        });
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

    public void setIndex(int index){
        this.index = index;
    }

    public int getIndex(){
        return index;
    }

}