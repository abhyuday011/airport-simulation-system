package uk.ac.warwick.cs261.ui.components;

import javafx.beans.DefaultProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

@DefaultProperty("responsiveChildren")
public class ResponsiveHBox extends HBox
{
    private final ObservableList<ResponsiveChild> responsiveChildren = FXCollections.observableArrayList();
    
    public ResponsiveHBox() 
    { 
        this((ResponsiveChild[]) null);
    }
    
    public ResponsiveHBox(ResponsiveChild... children)
    {
        super();
        
        this.responsiveChildren.addListener(this::onResponsiveChildrenChange);
        
        this.widthProperty().addListener(x -> updateChildWidths());
        
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
        
        setHgrow(content, Priority.NEVER);
        
        responsiveChild.relativeSizeProperty().addListener(x -> updateChildWidth(responsiveChild));
        
        updateChildWidth(responsiveChild);
    }
    
    private void removeChild(ResponsiveChild responsiveChild)
    {
        Node content = responsiveChild.getContent();
        
        if (content != null)
            getChildren().remove(content);
    }
    
    private void updateChildWidths()
    {
        double containerWidth = getWidth();
        
        if (containerWidth <= 0)
            return;
        
        for (ResponsiveChild responsiveChild : responsiveChildren)
            updateChildWidth(responsiveChild);
    }
    
    private void updateChildWidth(ResponsiveChild responsiveChild)
    {
        Region content = responsiveChild.getContent();

        if (content == null)
            return;

        double relativeSize = responsiveChild.getRelativeSize();
        double childWidth = getWidth() * relativeSize;
        
        content.setPrefWidth(childWidth);
        content.setMaxWidth(childWidth);
        content.setMinWidth(0);
    }
}