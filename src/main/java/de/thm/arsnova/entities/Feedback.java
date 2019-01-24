/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

import java.util.ArrayList;
import java.util.List;

/**
 * The feedback values of a single session.
 */
public class Feedback {
	public static final int MIN_FEEDBACK_TYPE = 0;
	public static final int MAX_FEEDBACK_TYPE = 3;

	public static final int FEEDBACK_FASTER = 0;
	public static final int FEEDBACK_OK = 1;
	public static final int FEEDBACK_SLOWER = 2;
	public static final int FEEDBACK_AWAY = 3;

	private final List<Integer> values;

	public Feedback(final int a, final int b, final int c, final int d) {
		values = new ArrayList<>();
		values.add(a);
		values.add(b);
		values.add(c);
		values.add(d);
	}

	public final List<Integer> getValues() {
		return values;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Feedback)) {
			return false;
		}
		Feedback other = (Feedback) obj;

		if (this.values.size() != other.values.size()) {
			return false;
		}

		boolean result = true;
		for (int i = 0; i < values.size(); i++) {
			result = result && (this.values.get(i).equals(other.values.get(i)));
		}
		return result;
	}

	@Override
	public int hashCode() {
		// See http://stackoverflow.com/a/113600
		int result = 42;
		for (Integer v : values) {
			result = 37 * result + v;
		}
		return result;
	}

	@Override
	public String toString() {
		return "Feedback [values=" + values + "]";
	}
}
