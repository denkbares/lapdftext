package com.denkbares.lapdf.tekno.studio.dashboard.ruleItem;

import java.net.URL;
import java.util.ResourceBundle;

import com.denkbares.lapdf.tekno.studio.dashboard.cssArea.CssAreaPresenter;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;

import com.denkbares.lapdf.tekno.studio.dashboard.rules.Rule;

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
        //Activate on return key pressed
        ruleNameTextField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                thisRule.name = ruleNameTextField.getText();
                System.out.println("Saved new Rule name : " + thisRule.getName());
            }
        });

        //Makes sure that minSupport values are legal
        minSupportTextField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (Double.parseDouble(minSupportTextField.getText()) > 1) {
                    System.out.println("Entered too much for minSupport, set it back to max!");
                    thisRule.setMinSupport(1);
                } else {
                    thisRule.setMinSupport(Double.parseDouble(minSupportTextField.getText()));
                    System.out.println("Saved new Rule support : " + thisRule.getMinSupport());
                }
            }
        });

        minSupportTextField.addEventFilter(KeyEvent.KEY_PRESSED , new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent inputevent) {
                //Make sure that event is either a digit or a period
                if(! (inputevent.getText().matches("\\d") || inputevent.getText().equals("."))){
                    inputevent.consume();
                }

                //Make sure that only one period is contained in the field
                else if(inputevent.getText().equals(".") && minSupportTextField.getText().contains(".")){
                    inputevent.consume();
                }

                //If pressed CTRL-DEL on Mouse Over, delete Rule
                if(inputevent.getCode().equals(KeyCode.DELETE) && inputevent.isControlDown()){
                    deleteRuleButtonPressed();
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