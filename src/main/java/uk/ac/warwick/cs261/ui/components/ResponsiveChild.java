package uk.ac.warwick.cs261.ui.components;

import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;

@DefaultProperty("content")
public class ResponsiveChild
{
    private final DoubleProperty relativeSize = new SimpleDoubleProperty();
    private final ObjectProperty<Region> content = new SimpleObjectProperty<>(null);

    public ResponsiveChild() { this(1.0, null); }

    public ResponsiveChild(double relativeSize) { this(relativeSize, null); }

    public ResponsiveChild(double relativeSize, Region content)
    {
        setRelativeSize(relativeSize);
        setContent(content);
    }

    public double getRelativeSize() { return relativeSize.get(); }
    public void setRelativeSize(double relativeSize) { this.relativeSize.set(relativeSize); }

    public DoubleProperty relativeSizeProperty() { return relativeSize; }
    
    public Region getContent() { return content.get(); }
    public void setContent(Region content) { this.content.set(content); }
    
    public ObjectProperty<Region> contentProperty() { return content; }
}