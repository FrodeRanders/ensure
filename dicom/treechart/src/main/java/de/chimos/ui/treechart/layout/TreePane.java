/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.chimos.ui.treechart.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * @author Niklas Hofmann, Gerrit Linnemann
 */
public class TreePane extends Region {
    private static final Double Y_AXIS_SPACING = 30.0;
    private static final Double X_AXIS_SPACING = 15.0;
    private static final Double LINE_SPACING = 1.5;
    private static final Boolean SHOW_LINES = Boolean.TRUE;

    private SimpleDoubleProperty _yAxisSpacingField;
    private SimpleDoubleProperty _xAxisSpacingField;
    private SimpleDoubleProperty _lineSpacingField;
    private SimpleBooleanProperty _showLinesField;

    public DoubleProperty yAxisSpacingProperty() {
        if (this._yAxisSpacingField == null) {
            this._yAxisSpacingField = new SimpleDoubleProperty(Y_AXIS_SPACING);
        }
        return (DoubleProperty) this._yAxisSpacingField;
    }

    public DoubleProperty xAxisSpacingProperty() {
        if (this._xAxisSpacingField == null) {
            this._xAxisSpacingField = new SimpleDoubleProperty(X_AXIS_SPACING);
        }
        return (DoubleProperty) this._xAxisSpacingField;
    }

    public DoubleProperty lineSpacingProperty() {
        if (this._lineSpacingField == null) {
            this._lineSpacingField = new SimpleDoubleProperty(LINE_SPACING);
        }
        return (DoubleProperty) this._lineSpacingField;
    }

    public BooleanProperty showLinesProperty() {
        if (this._showLinesField == null) {
            this._showLinesField = new SimpleBooleanProperty(SHOW_LINES);
        }
        return (BooleanProperty) this._showLinesField;
    }

    private ChangeListener<Number> _spacingChangeListener = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            layoutChildren();
        }
    };

    private ChangeListener<Boolean> _showLinesChangeListener = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            layoutChildren();
        }
    };
  
    public Double getYAxisSpacing() {
        return yAxisSpacingProperty().getValue();
    }
  
    public Double getXAxisSpacing() {
        return xAxisSpacingProperty().getValue();
    }
  
    public Double getLineSpacing() {
        return lineSpacingProperty().getValue();
    }
  
    public Boolean getShowLines() {
        return showLinesProperty().getValue();
    }
    
    public void setYAxisSpacing(Double value) {
        this.yAxisSpacingProperty().setValue(value);
    }
    
    public void setXAxisSpacing(Double value) {
        this.xAxisSpacingProperty().setValue(value);
    }
    
    public void setLineSpacing(Double value) {
        this.lineSpacingProperty().setValue(value);
    }
    
    public void setShowLines(Boolean value) {
        this.showLinesProperty().setValue(value);
    }

    public TreePane() {
        super();
        
        minHeightProperty().bind(prefHeightProperty());
        maxHeightProperty().bind(prefHeightProperty());

        minWidthProperty().bind(prefWidthProperty());
        maxWidthProperty().bind(prefWidthProperty());

        yAxisSpacingProperty().addListener(_spacingChangeListener);
        xAxisSpacingProperty().addListener(_spacingChangeListener);
        lineSpacingProperty().addListener(_spacingChangeListener);

        showLinesProperty().addListener(_showLinesChangeListener);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if (_nodeByPosition.isEmpty()) {
            adjustLineCount(0);

            setPrefWidth(0);
            setPrefHeight(0);

            return;
        }

        // Calculate width per position based on layout bounds
        Map<NodePosition, Double> widthByPosition = new HashMap<>();
        Map<Integer, Double> levelHeight = new HashMap<>();

        Map<Integer, Set<NodePosition>> positionsByLevel = new HashMap<>();
        Map<NodePosition, Set<NodePosition>> positionsByParentPosition = new HashMap<>();

        int maxLevel = Collections.max(_nodeByLevel.keySet());

        for (int curLevel = maxLevel; curLevel >= 0; --curLevel) {
            levelHeight.put(curLevel, 0.0);

            positionsByLevel.put(curLevel, new HashSet<NodePosition>());
        }

        for (int curLevel = maxLevel; curLevel >= 0; --curLevel) {
            // Get bounds of nodes on current level
            Set<Node> curLevelNodes = _nodeByLevel.get(curLevel);

            if (curLevelNodes != null) {
                // Get node bounds
                for (Node node : curLevelNodes) {
                    // Node data
                    NodePosition nodePosition = _positionByNode.get(node);
                    Bounds nodeBounds = node.getLayoutBounds();

                    // Get bounds
                    widthByPosition.put(nodePosition, nodeBounds.getWidth() + this.getXAxisSpacing());
                    levelHeight.put(curLevel, Math.max(levelHeight.get(curLevel), nodeBounds.getHeight() + this.getYAxisSpacing()));

                    // Register positions
                    positionsByLevel.get(curLevel).add(nodePosition);

                    if (curLevel > 0) {
                        positionsByLevel.get(curLevel - 1).add(nodePosition.getParent());
                    }
                }
            }

            // Calculate position widths of current level
            for (NodePosition position : positionsByLevel.get(curLevel)) {
                // Register positions
                if (position.getLevel() > 0) {
                    NodePosition parentPosition = position.getParent();

                    positionsByLevel.get(position.getLevel() - 1).add(parentPosition);

                    if (!positionsByParentPosition.containsKey(parentPosition)) {
                        positionsByParentPosition.put(parentPosition, new HashSet<NodePosition>());
                    }

                    positionsByParentPosition.get(parentPosition).add(position);
                }

                // Get width of children
                double widthOfChildren = 0;

                Set<NodePosition> parentPositions = positionsByParentPosition.get(position);

                if (parentPositions != null) {
                    for (NodePosition childPosition : parentPositions) {
                        if (widthByPosition.containsKey(childPosition)) {
                            widthOfChildren += widthByPosition.get(childPosition);
                        }
                    }
                }

                // Get maximum of node bound and sum of child node bounds
                if (!widthByPosition.containsKey(position)) {
                    widthByPosition.put(position, widthOfChildren);
                } else {
                    widthByPosition.put(position, Math.max(widthByPosition.get(position), widthOfChildren));
                }
            }
        }

        // Calculate position boxes
        Map<NodePosition, Rectangle2D> boxesByPosition = new HashMap<>();

        if (!positionsByLevel.containsKey(0) || positionsByLevel.get(0).size() != 1) {
            throw new IllegalStateException();
        }

        boxesByPosition.put(NodePosition.ROOT, new Rectangle2D(0, 0, widthByPosition.get(NodePosition.ROOT), levelHeight.get(0)));

        for (int curLevel = 0; curLevel <= maxLevel; ++curLevel) {
            for (NodePosition position : positionsByLevel.get(curLevel)) {
                Rectangle2D positionBox = boxesByPosition.get(position);

                List<NodePosition> childPositions = new ArrayList<>();

                if (positionsByParentPosition.containsKey(position)) {
                    childPositions.addAll(positionsByParentPosition.get(position));
                }

                Collections.sort(childPositions);

                double childX = positionBox.getMinX();

                for (NodePosition childPosition : childPositions) {
                    double childWidth = widthByPosition.get(childPosition);

                    boxesByPosition.put(childPosition, new Rectangle2D(childX, positionBox.getMaxY(), childWidth, levelHeight.get(childPosition.getLevel())));

                    childX += childWidth;
                }
            }
        }

        // Position nodes
        Map<NodePosition, Double> xCenterHintByPosition = new HashMap<>();
        Map<NodePosition, Double> yCenterHintByPosition = new HashMap<>();

        for (int curLevel = maxLevel; curLevel >= 0; --curLevel) {
            for (NodePosition position : positionsByLevel.get(curLevel)) {
                // Calculate center hints
                Rectangle2D positionBox = boxesByPosition.get(position);

                double xCenterHint = (positionBox.getMinX() + positionBox.getMaxX()) / 2;

                if (xCenterHintByPosition.containsKey(position)) {
                    xCenterHint = xCenterHintByPosition.get(position);
                }

                double yCenterHint = (positionBox.getMinY() + positionBox.getMaxY()) / 2;

                xCenterHintByPosition.put(position, xCenterHint);
                yCenterHintByPosition.put(position, yCenterHint);

                // Position node
                if (_nodeByPosition.containsKey(position)) {
                    Node node = _nodeByPosition.get(position);
                    Bounds nodeBounds = node.getLayoutBounds();

                    node.relocate(xCenterHint - nodeBounds.getWidth() / 2, yCenterHint - nodeBounds.getHeight() / 2);
                }

                // Update parent node position hint
                NodePosition parentPosition = position.getParent();

                if (xCenterHintByPosition.containsKey(parentPosition)) {
                    xCenterHintByPosition.put(parentPosition, (xCenterHintByPosition.get(parentPosition) + xCenterHint) / 2);
                } else {
                    xCenterHintByPosition.put(parentPosition, xCenterHint);
                }
            }
        }

        // Update lines
        if (this.getShowLines()) {
            adjustLineCount(boxesByPosition.size() - 1);

            int currentLine = 0;

            for (NodePosition position : boxesByPosition.keySet()) {
                if (!positionsByParentPosition.containsKey(position)) {
                    continue;
                }

                for (NodePosition childPosition : positionsByParentPosition.get(position)) {
                    Bounds fromBounds = _nodeByPosition.containsKey(position) ? _nodeByPosition.get(position).getLayoutBounds() : null;
                    Bounds toBounds = _nodeByPosition.containsKey(childPosition) ? _nodeByPosition.get(childPosition).getLayoutBounds() : null;

                    Point2D lineFrom = new Point2D(
                            xCenterHintByPosition.get(position),
                            yCenterHintByPosition.get(position)
                            + (fromBounds != null ? (fromBounds.getHeight() / 2) : 0)
                            + this.getLineSpacing()
                    );

                    Point2D lineTo = new Point2D(
                            xCenterHintByPosition.get(childPosition),
                            yCenterHintByPosition.get(childPosition)
                            - (toBounds != null ? (toBounds.getHeight() / 2) : 0)
                            - this.getLineSpacing()
                    );

                    Line l = _lines.get(currentLine);

                    l.setStartX(lineFrom.getX());
                    l.setStartY(lineFrom.getY());

                    l.setEndX(lineTo.getX());
                    l.setEndY(lineTo.getY());

                    ++currentLine;
                }
            }
        } else {
            adjustLineCount(0);
        }

        // Update preferred size
        double totalHeight = 0;
        for (Double h : levelHeight.values()) {
            totalHeight += h;
        }

        setPrefWidth(widthByPosition.get(NodePosition.ROOT));
        setPrefHeight(totalHeight);
    }

    private List<Line> _lines = new ArrayList<>();

    protected void adjustLineCount(int count) {
        while (count < _lines.size()) {
            getChildren().remove(_lines.get(_lines.size() - 1));
            getManagedChildren().remove(_lines.get(_lines.size() - 1));

            _lines.remove(_lines.size() - 1);
        }

        while (count > _lines.size()) {
            Line l = new Line();
            l.getStyleClass().add("tree-pane-line");

            getChildren().add(l);
            getManagedChildren().add(l);
            l.toBack();

            _lines.add(l);
        }
    }

    public void addChild(Node node, NodePosition position) {
        if (node == null || position == null) {
            throw new IllegalArgumentException();
        }

        if (!getChildren().contains(node)) {
            getChildren().add(node);
            getManagedChildren().add(node);
            node.toFront();
        }

        setPosition(node, position);

        layoutChildren();
    }

    public void removeChild(Node node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }

        unsetPosition(node);

        getChildren().remove(node);
        getManagedChildren().remove(node);

        layoutChildren();
    }
    
    public void clearChildren(){
        getChildren().clear();
        getManagedChildren().clear();
                
        layoutChildren();
    }
    
    public boolean isChildrenEmpty(){
        return getChildren().isEmpty();
    }

    private Map<Node, NodePosition> _positionByNode = new HashMap<>();
    private Map<NodePosition, Node> _nodeByPosition = new HashMap<>();
    private Map<NodePosition, Set<Node>> _nodeByParentPosition = new HashMap<>();
    private Map<Integer, Set<Node>> _nodeByLevel = new HashMap<>();

    public NodePosition getPosition(Node node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }

        if (!_positionByNode.containsKey(node)) {
            return null;
        }

        return _positionByNode.get(node);
    }

    public Node getNode(NodePosition position) {
        if (position == null) {
            throw new IllegalArgumentException();
        }

        if (!_nodeByPosition.containsKey(position)) {
            return null;
        }

        return _nodeByPosition.get(position);
    }

    public Set<Node> getNodesOfParent(NodePosition position) {
        if (position == null) {
            throw new IllegalArgumentException();
        }

        if (!_nodeByParentPosition.containsKey(position)) {
            return null;
        }

        return Collections.unmodifiableSet(_nodeByParentPosition.get(position));
    }

    public Set<Node> getNodesOfLevel(Integer level) {
        if (level == null) {
            throw new IllegalArgumentException();
        }

        if (!_nodeByLevel.containsKey(level)) {
            return null;
        }

        return Collections.unmodifiableSet(_nodeByLevel.get(level));
    }

    protected void setPosition(Node node, NodePosition position) {
        if (node == null || position == null) {
            throw new IllegalArgumentException();
        }

        unsetPosition(node);

        if (_nodeByPosition.containsKey(position)) {
            unsetPosition(_nodeByPosition.get(position));
        }

        NodePosition parentPosition = position.getParent();
        int level = position.getLevel();

        _positionByNode.put(node, position);
        _nodeByPosition.put(position, node);

        if (!_nodeByParentPosition.containsKey(parentPosition)) {
            _nodeByParentPosition.put(parentPosition, new HashSet<Node>());
        }

        _nodeByParentPosition.get(parentPosition).add(node);

        if (!_nodeByLevel.containsKey(level)) {
            _nodeByLevel.put(level, new HashSet<Node>());
        }

        _nodeByLevel.get(level).add(node);
    }

    protected void unsetPosition(Node node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }

        if (!_positionByNode.containsKey(node)) {
            return;
        }

        NodePosition position = _positionByNode.get(node);

        _positionByNode.remove(node);
        _nodeByPosition.remove(position);
        _nodeByParentPosition.get(position.getParent()).remove(node);
        _nodeByLevel.get(position.getLevel()).remove(node);
    }

}
