/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FeedbackTest {

	@Test
	public void differentObjectsShouldNotBeEqual() {
		Feedback f = new Feedback(0, 0, 0, 0);
		String x = "";

		assertFalse(f.equals(x));
	}

	@Test
	public void differentlySizedFeedbacksShouldNotBeEqual() {
		Feedback f1 = new Feedback(0, 0, 0, 0);
		Feedback f2 = new Feedback(0, 0, 0, 0);
		f2.getValues().add(0);

		assertFalse(f1.equals(f2));
	}

	@Test
	public void nullShouldNotBeEqual() {
		Feedback f = new Feedback(0, 0, 0, 0);
		assertFalse(f.equals(null));
	}

	@Test
	public void sameContentsShouldBeEqual() {
		Feedback f1 = new Feedback(1, 2, 3, 4);
		Feedback f2 = new Feedback(1, 2, 3, 4);

		assertTrue(f1.equals(f2));
		assertTrue(f2.equals(f1));
	}

	@Test
	public void differentContentsShouldNotBeEqual() {
		Feedback f1 = new Feedback(1, 2, 3, 4);
		Feedback f2 = new Feedback(4, 3, 2, 1);

		assertFalse(f1.equals(f2));
		assertFalse(f2.equals(f1));
	}
}
