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
package de.thm.arsnova;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class PaginationListDecorator<T> implements List<T> {
	private final List<T> list;
	private List<T> subList;
	private int offset;
	private int limit;

	public PaginationListDecorator(final List<T> list, final int offset, final int limit) {
		this.list = list;
		this.offset = offset;
		this.limit = limit;
		checkRange();
		subList = list.subList(this.offset, this.offset + this.limit);
	}

	private void checkRange() {
		if (offset < 0) {
			offset = 0;
		}
		if (limit <= 0 || limit > list.size() - offset) {
			limit = list.size();
		}
	}

	public List<T> getList() {
		return list;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(final int offset) {
		this.offset = offset;
		checkRange();
		subList = list.subList(this.offset, this.offset + this.limit);
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(final int limit) {
		this.limit = limit;
		checkRange();
		subList = list.subList(this.offset, this.offset + this.limit);
	}

	public int getTotalSize() {
		return list.size();
	}

	@Override
	public Iterator<T> iterator() {
		return subList.iterator();
	}

	@Override
	public boolean add(final T e) {
		return subList.add(e);
	}

	@Override
	public void add(final int index, final T element) {
		subList.add(index, element);
	}

	@Override
	public boolean addAll(final Collection<? extends T> c) {
		return subList.addAll(c);
	}

	@Override
	public boolean addAll(final int index, final Collection<? extends T> c) {
		return subList.addAll(index, c);
	}

	@Override
	public void clear() {
		subList.clear();
	}

	@Override
	public boolean contains(final Object o) {
		return subList.contains(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return subList.containsAll(c);
	}

	@Override
	public T get(final int index) {
		return subList.get(index);
	}

	@Override
	public int indexOf(final Object o) {
		return subList.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return subList.isEmpty();
	}

	@Override
	public int lastIndexOf(final Object o) {
		return subList.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		return subList.listIterator();
	}

	@Override
	public ListIterator<T> listIterator(final int index) {
		return subList.listIterator(index);
	}

	@Override
	public boolean remove(final Object o) {
		return subList.remove(o);
	}

	@Override
	public T remove(final int index) {
		return subList.remove(index);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return subList.removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return subList.retainAll(c);
	}

	@Override
	public T set(final int index, final T element) {
		return subList.set(index, element);
	}

	@Override
	public int size() {
		return subList.size();
	}

	@Override
	public List<T> subList(final int fromIndex, final int toIndex) {
		return subList.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return subList.toArray();
	}

	@Override
	public <A> A[] toArray(final A[] a) {
		return subList.toArray(a);
	}

}
