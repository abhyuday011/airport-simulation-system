package uk.ac.warwick.cs261.ui.components;

import javafx.beans.DefaultProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

@DefaultProperty("responsiveChildren")
public class ResponsiveVBox extends VBox
{
    private final ObservableList<ResponsiveChild> responsiveChildren = FXCollections.observableArrayList();
    
    public ResponsiveVBox() 
    { 
        this((ResponsiveChild[]) null);
    }
    
    public ResponsiveVBox(ResponsiveChild... children)
    {
        super();
        
        this.responsiveChildren.addListener(this::onResponsiveChildrenChange);
        
        this.heightProperty().addListener(x -> updateChildHeights());
        
        if (children != null)
            this.responsiveChildren.addAll(children);
    }
    
    public ObservableList<ResponsiveChild> getResponsiveChildren() 
    { 
        return responsiveChildren; 
    }
    
    private void onResponsiveChildrenChange(ListChangeListener.Change<? extends ResponsiveChild> change)
    {
        while (change.next()) 
        {
            if (change.wasAdded()) 
            {
                for (ResponsiveChild child : change.getAddedSubList())
                    addChild(child);
            }
            
            if (change.wasRemoved()) 
            {
                for (ResponsiveChild child : change.getRemoved())
                    removeChild(child);
            }
        }
    }
    
    private void addChild(ResponsiveChild responsiveChild)
    {
        Region content = responsiveChild.getContent();

        if (content == null)
            return;
        
        getChildren().add(content);
        
        setVgrow(content, Priority.NEVER);
        
        responsiveChild.relativeSizeProperty().addListener(x -> updateChildHeight(responsiveChild));
        
        updateChildHeight(responsiveChild);
    }
    
    private void removeChild(ResponsiveChild responsiveChild)
    {
        Node content = responsiveChild.getContent();
        
        if (content != null)
            getChildren().remove(content);
    }
    
    private void updateChildHeights()
    {
        double containerHeight= getHeight();
        
        if (containerHeight <= 0)
            return;
        
        for (ResponsiveChild responsiveChild : responsiveChildren)
            updateChildHeight(responsiveChild);
    }
    
    private void updateChildHeight(ResponsiveChild responsiveChild)
    {
        Region content = responsiveChild.getContent();

        if (content == null)
            return;

        double relativeSize = responsiveChild.getRelativeSize();
        double childHeight = getHeight() * relativeSize;
        
        content.setPrefHeight(childHeight);
        content.setMaxHeight(childHeight);
        content.setMinHeight(0);
    }
}