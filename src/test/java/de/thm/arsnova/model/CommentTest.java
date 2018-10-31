package de.thm.arsnova.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class CommentTest {
	@Test
	public void timestampReferenceTest() {
		Comment comment = new Comment();
		Date date = new Date();
		comment.setTimestamp(date);
		Assert.assertNotSame(date, comment.getTimestamp());
	}
}
