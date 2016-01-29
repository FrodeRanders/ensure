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
import java.util.List;

/**
 * @author Niklas Hofmann, Gerrit Linnemann
 */
public class NodePosition implements Comparable<NodePosition>, Cloneable {

    public static final NodePosition ROOT = new NodePosition();

    private final List<Integer> _path = new ArrayList<>();

    public NodePosition() {
    }

    public NodePosition(int... pathElements) {
        for (int i : pathElements) {
            _path.add(i);
        }
    }

    public NodePosition(List<Integer> path) {
        _path.addAll(path);
    }

    public List<Integer> getPath() {
        return _path;
    }

    public NodePosition getParent() {
        if (_path.isEmpty()) {
            return null;
        }

        NodePosition other = (NodePosition) clone();
        other._path.remove(other._path.size() - 1);

        return other;
    }

    public NodePosition getChild(int pathElement) {
        NodePosition other = (NodePosition) clone();
        other._path.add(pathElement);

        return other;
    }

    public int getLevel() {
        return this._path.size();
    }

    @Override
    public int compareTo(NodePosition other) {
        if (getLevel() < other.getLevel()) {
            return -1;
        } else if (getLevel() > other.getLevel()) {
            return +1;
        }

        for (int i = 0, j = _path.size(); i < j; ++i) {
            if (_path.get(i) < other._path.get(i)) {
                return -1;
            } else if (_path.get(i) > other._path.get(i)) {
                return +1;
            }
        }

        return 0;
    }

    @Override
    protected Object clone() {
        return new NodePosition(new ArrayList<>(_path));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodePosition other = (NodePosition) obj;
        return this._path == other._path || (this._path != null && this._path.equals(other._path));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this._path != null ? this._path.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "Position{" + "path=" + _path + ", level=" + getLevel() + '}';
    }

}
