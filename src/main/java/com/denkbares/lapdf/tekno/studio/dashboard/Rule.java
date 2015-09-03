package com.denkbares.lapdf.tekno.studio.dashboard;

/**
 * Created by Maximilian on 03.09.2015.
 */
public class Rule {

    private String rule;
    public String name;
    private double minSupport;

    public Rule(String rule, String name, double minSupport){
        this.rule = rule;
        this.name = name;
        this.minSupport = minSupport;
    }

    public Rule(String rule, String name){
        this.rule = rule;
        this.name = name;
        minSupport = 0.5;
    }

    public String getRule(){
        return rule;
    }

    public String getName(){
        return name;
    }

    public double getMinSupport(){
        return minSupport;
    }

    protected void setRule(String s){
        rule = s;
    }

    protected void setName(String s){
        name = s;
    }

    protected void setMinSupport(double d){
        minSupport = d;
    }

}
