/*
 *   DENOPTIM
 *   Copyright (C) 2020 Marco Foscato <marco.foscato@uib.no>
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

package gui;

import javax.swing.JComponent;

/**
 * Interface for parameter forms. 
 * This interface requires the existence of a methods that allow recovery of the parameters from the forms.
 * 
 * @author Marco Foscato
 */
public interface IParametersForm {
	public void importParametersFromDenoptimParamsFile(String fileName) throws Exception;
	public void importSingleParameter(String key, String value) throws Exception;
    public void putParametersToString(StringBuilder sb) throws Exception;
    public boolean hasUnsavedChanges();
    public void setUnsavedChanges(boolean val);
}
