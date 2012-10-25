package de.thm.arsnova.entities;

import java.util.ArrayList;
import java.util.List;

public class Feedback {
	private List<Integer> values;

	public Feedback(int a, int b, int c, int d) {
		values = new ArrayList<Integer>();
		values.add(a);
		values.add(b);
		values.add(c);
		values.add(d);
	}

	public List<Integer> getValues() {
		return values;
	}
}
