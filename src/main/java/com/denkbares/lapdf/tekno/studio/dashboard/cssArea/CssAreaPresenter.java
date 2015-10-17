package com.denkbares.lapdf.tekno.studio.dashboard.cssArea;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ResourceBundle;

import com.denkbares.lapdf.tekno.studio.dashboard.DashboardPresenter;
import com.denkbares.lapdf.tekno.studio.dashboard.rules.RuleMaker;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import com.denkbares.lapdf.tekno.studio.dashboard.Rule;
import com.denkbares.lapdf.tekno.studio.dashboard.ruleItem.RuleItemView;
import com.denkbares.lapdf.tekno.studio.dashboard.ruleItem.RuleItemPresenter;
import javafx.scene.layout.VBox;
import org.apache.commons.logging.Log;

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
    Button confirmRuleButton;

    @FXML
    Button cancelRuleButton;

    @FXML
    TextField ruleNameTextField;

    @FXML
    VBox rulesVBox;

    ArrayList<Integer> filledRows;
    ArrayList<Rule> rulesList;
    ArrayList<RuleItemView> rulesViewList;
    DashboardPresenter dbp;
    RuleMaker ruleMaker;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ruleMaker = new RuleMaker();
        rulesList = new ArrayList<Rule>();
        rulesViewList = new ArrayList<RuleItemView>();
        filledRows = new ArrayList<Integer>();

        ruleNameTextField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode() == KeyCode.ESCAPE)
                    cancelRuleButtonPressed();
            }
        });

        ruleNameTextField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(dbp.getSelectionMode()){
                    confirmRuleButtonPressed();
                }
                else{
                    addRuleButtonPressed();
                }
            }
        });
    }

    public void confirmRuleButtonPressed(){
        try {
            ArrayList<ChunkBlock> selection = dbp.getSelectedBlocks();
            dbp.setSelectionMode(false);
            Rule tempRule = ruleMaker.makeRule(selection);
            if (tempRule == null) {

            } else {
                tempRule.setName(ruleNameTextField.getText());
                rulesList.add(tempRule);
                addRuleToGrid(tempRule);
            }
        }
        catch (Exception e){
            //In case no PDF is loaded or no blocks are selected, enables debugging and creating rules without using PDF reference
            e.printStackTrace();
            Rule tempRule = new Rule(ruleNameTextField.getText());
            rulesList.add(tempRule);
            addRuleToGrid(tempRule);
        }
    }

    public void cancelRuleButtonPressed(){
        dbp.setSelectionMode(false);
    }

    public void setDbp(DashboardPresenter d){
        dbp = d;
    }

    public Collection<Rule> getRules(){
        return rulesList;
    }

    public String getRulesAsText(){
        String out = "";
        for(Rule r : rulesList){
            out += "%%CoveringList \n"+ r.getName() + "{" + r.getRule();
        }
        return out;
    }

    public void addRuleButtonPressed(){
        if(dbp.isPDFLoaded())
            dbp.setSelectionMode(true);
        else
            confirmRuleButtonPressed();
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

        if(rulesVBox.getChildren().size() == 1) {
            rulesViewList.remove(0);
            rulesVBox.getChildren().remove(0);
        }
        else {
            rulesViewList.remove(rule.getIndex());
            rulesVBox.getChildren().remove(rule.getIndex());
            updateIndicies(rule.getIndex());
        }

    }

    public void updateIndicies(int deletedIndex){
        //Update Indicies after deleting element at deleted index
        for(RuleItemView r : rulesViewList){
            RuleItemPresenter presenter = (RuleItemPresenter) r.getPresenter();
            if(presenter.getIndex() > deletedIndex){
                presenter.setIndex(presenter.getIndex()-1);
            }
        }
    }

    //TODO Delete
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