package denoptim.graph;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

import java.util.Comparator;

/**
 * Comparator for DENOPTIMAttachmentPoints. APs are sorted by creation time,
 * effectively. This is the standard behaviour of 
 * {@link AttachmentPoint#compareTo(AttachmentPoint)}.
 * 
 * @author Marco Foscato
 */

public class AttachmentPointComparator implements Comparator<AttachmentPoint>
{
    @Override
    public int compare(AttachmentPoint ap1, AttachmentPoint ap2)
    {
        return ap1.compareTo(ap2);
    }
}

