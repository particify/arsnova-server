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

package de.thm.arsnova.model;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.util.Date;
import org.junit.Test;
import org.springframework.core.style.ToStringCreator;

/**
 * Tests {@link Entity}'s overrides for {@link Object#hashCode()}, {@link Object#equals(Object)}, and
 * {@link Object#toString()}.
 *
 * @author Daniel Gerhardt
 */
public class EntityTest {
	class SomeEntity extends Entity {
		private String testA;

		public SomeEntity(String id, String rev, Date creationTimestamp, Date updateTimestamp, String testA) {
			this.id = id;
			this.rev = rev;
			this.creationTimestamp = creationTimestamp;
			this.updateTimestamp = updateTimestamp;
			this.testA = testA;
		}

		@Override
		public int hashCode() {
			return hashCode(super.hashCode(), testA);
		}

		@Override
		public ToStringCreator buildToString() {
			return super.buildToString().append("testA", testA);
		}
	}

	class AnotherEntity extends SomeEntity {
		private String testB;

		public AnotherEntity(String id, String rev, Date creationTimestamp, Date updateTimestamp, String testA, String testB) {
			super(id, rev, creationTimestamp, updateTimestamp, testA);
			this.testB = testB;
		}

		@Override
		public int hashCode() {
			return hashCode(super.hashCode(), testB);
		}

		@Override
		public ToStringCreator buildToString() {
			return super.buildToString().append("testB", testB);
		}
	}

	@Test
	public void testHashCode() {
		SomeEntity entity1 = new SomeEntity("id", "rev", new Date(0), new Date(0), "test");
		SomeEntity entity2 = new SomeEntity("id", "rev", new Date(0), new Date(0), "test");
		SomeEntity entity3 = new SomeEntity("wrongId", "rev", new Date(0), new Date(0), "test");
		assertEquals(entity1.hashCode(), entity2.hashCode());
		assertNotEquals(entity1.hashCode(), entity3.hashCode());
		AnotherEntity entity4 = new AnotherEntity("id", "rev", new Date(0), new Date(0), "someTest", "anotherTest");
		AnotherEntity entity5 = new AnotherEntity("id", "rev", new Date(0), new Date(0), "someTest", "anotherTest");
		AnotherEntity entity6 = new AnotherEntity("id", "rev", new Date(0), new Date(0), "someTest", "wrong");
		assertEquals(entity4.hashCode(), entity5.hashCode());
		assertNotEquals(entity4.hashCode(), entity6.hashCode());
	}

	@Test
	public void testEquals() {
		SomeEntity entity1 = new SomeEntity("id", "rev", new Date(0), new Date(0), "test");
		SomeEntity entity2 = new SomeEntity("id", "rev", new Date(0), new Date(0), "test");
		SomeEntity entity3 = new SomeEntity("wrongId", "rev", new Date(0), new Date(0), "test");
		assertEquals(entity1, entity2);
		assertNotEquals(entity1, entity3);
	}

	@Test
	public void testToString() {
		SomeEntity entity1 = new SomeEntity("id", "rev", new Date(0), new Date(0), "test");
		assertThat(entity1.toString(), startsWith("[EntityTest.SomeEntity"));
		assertThat(entity1.toString(), endsWith("testA = 'test']"));
		AnotherEntity entity2 = new AnotherEntity("id", "rev", new Date(0), new Date(0), "someTest", "anotherTest");
		assertThat(entity2.toString(), startsWith("[EntityTest.AnotherEntity"));
		assertThat(entity2.toString(), endsWith("testA = 'someTest', testB = 'anotherTest']"));
	}
}
