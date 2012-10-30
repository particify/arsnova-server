package de.thm.arsnova.entities;

import java.util.ArrayList;
import java.util.List;

public class Feedback {
	public static final int MIN_FEEDBACK_TYPE = 0;
	public static final int MAX_FEEDBACK_TYPE = 3;

	public static final int FEEDBACK_FASTER = 0;
	public static final int FEEDBACK_OK = 1;
	public static final int FEEDBACK_SLOWER = 2;
	public static final int FEEDBACK_AWAY = 3;

	private List<Integer> values;

	public Feedback(final int a, final int b, final int c, final int d) {
		values = new ArrayList<Integer>();
		values.add(a);
		values.add(b);
		values.add(c);
		values.add(d);
	}

	public final List<Integer> getValues() {
		return values;
	}
}
