/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.utils;

/**
 * Types of mutation defined in relation to what happens to the target vertex
 * (i.e., the actual mutation site), and the child vertices, i.e., any vertices
 * reachable by a directed path from the target vertex.
 */

public enum MutationType {
    
    /**
     * Removes the target vertex and all child vertices.
     */
    DELETE,
    
    /**
     * append vertices on the target vertex according to substitution
     * probability.
     */
    EXTEND,
    
    /**
     * Replace the target vertex and any of the child vertices.
     * The effect on the child vertices is decided by substitution probability.
     */
    CHANGEBRANCH,
    
    /**
     * Replace the target vertex keeping all the child structure.
     */
    CHANGELINK,
    
    /**
     * Adds a vertex between two previously connected vertexes.
     */
    ADDLINK,
    
    /**
     * Removes a vertex from a tree and merges remaining branches into the
     * remaining trunk.
     */
    DELETELINK, 
    
    /**
     * Removes a vertex and all its neighbors recursively until a branching 
     * point, i.e., until a vertex that is connected to more than two 
     * non-capping vertexes.
     */
    DELETECHAIN

}
