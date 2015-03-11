/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.entities;

public class SortOrder {

	private String sortVariant;
    private String[] sortOrder;

	private String _id;
	private String _rev;
    
    public void setSortVariant(final String sortVariant) {
        this.sortVariant = sortVariant;
    }
    
    public String getSortVariant() {
        return this.sortVariant;
    }
    
    public void setSortOrder(final String[] sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public String[] getSortOrder() {
        return this.sortOrder;
    }

	public void set_id(final String id) {
		_id = id;
	}

	public String get_id() {
		return _id;
	}

	public void set_rev(final String rev) {
		_rev = rev;
	}

	public String get_rev() {
		return _rev;
	} 
}
