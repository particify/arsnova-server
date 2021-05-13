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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * This services handles serialization and deserialization for
 * delimiter-separated values (CSV). The CSV schemas can be declared through a
 * POJO classes with Jackson annotations in the
 * {@link de.thm.arsnova.model.export} package.
 *
 * @author Daniel Gerhardt
 */
@Service
public class CsvService {
	private static final int MAX_SEARCH_BYTES = 500;
	private final CsvMapper mapper = new CsvMapper();
	private final ConcurrentHashMap<Class<?>, CsvSchema> csvSchemas = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Class<?>, CsvSchema> tsvSchemas = new ConcurrentHashMap<>();
	private final String unicodeBom = "\ufeff";

	/**
	 * Serializes a list of objects as comma-separated values.
	 */
	public <T> byte[] toCsv(final List<T> objects, final Class<T> clazz, final Charset charset)
			throws JsonProcessingException {
		final CsvSchema schema = csvSchemaFor(clazz);
		final String csv = byteOrderMark(charset) + mapper.writer(schema).writeValueAsString(objects);
		return csv.getBytes(charset);
	}

	/**
	 * Serializes a list of objects as comma-separated values with UTF-8
	 * encoding.
	 */
	public <T> byte[] toCsv(final List<T> objects, final Class<T> clazz)
			throws JsonProcessingException {
		return toCsv(objects, clazz, StandardCharsets.UTF_8);
	}

	/**
	 * Serializes a list of objects as tab-separated values.
	 */
	public <T> byte[] toTsv(final List<T> objects, final Class<T> clazz, final Charset charset)
			throws JsonProcessingException {
		final CsvSchema schema = tsvSchemaFor(clazz);
		final String tsv = byteOrderMark(charset) + mapper.writer(schema).writeValueAsString(objects);
		return tsv.getBytes(charset);
	}

	/**
	 * Serializes a list of objects as tab-separated values with UTF-8
	 * encoding.
	 */
	public <T> byte[] toTsv(final List<T> objects, final Class<T> clazz)
			throws JsonProcessingException {
		return toTsv(objects, clazz, StandardCharsets.UTF_8);
	}

	/**
	 * Deserializes delimiter-separated values as a {@link List} of objects of
	 * the specified type. The delimiter is detected automatically. Supported
	 * delimiters are: tab, comma and semicolon.
	 *
	 * @param csv delimiter-separated values
	 * @param clazz reified target type for deserialization
	 * @param <T> target type for deserialization
	 */
	public <T> List<T> toObject(final byte[] csv, final Class<T> clazz) throws IOException {
		final char separator = detectSeparator(csv);
		final CsvSchema schema = separator == '\t' ? tsvSchemaFor(clazz) : csvSchemaFor(clazz);
		final MappingIterator<T> iterator = mapper.readerFor(clazz).with(schema).readValues(csv);
		return iterator.readAll();
	}

	private String byteOrderMark(final Charset charset) {
		return charset == StandardCharsets.UTF_16BE || charset == StandardCharsets.UTF_16LE
				? unicodeBom : "";
	}

	private char detectSeparator(final byte[] bytes) {
		final String firstLine =
				new String(Arrays.copyOfRange(bytes, 0, Math.min(MAX_SEARCH_BYTES, bytes.length)))
						.lines().findFirst().orElse("");
		return firstLine.contains("\t") ? '\t' : (firstLine.contains(";") ? ';' : ',');
	}

	private CsvSchema csvSchemaFor(final Class<?> clazz) {
		return csvSchemas.computeIfAbsent(clazz,
				(k) -> mapper.schemaFor(clazz)
						.withHeader()
						.withArrayElementSeparator("\n"));
	}

	private CsvSchema tsvSchemaFor(final Class<?> clazz) {
		return tsvSchemas.computeIfAbsent(clazz,
				(k) -> mapper.schemaFor(clazz)
						.withHeader()
						.withArrayElementSeparator("\n")
						.withColumnSeparator('\t'));
	}
}
