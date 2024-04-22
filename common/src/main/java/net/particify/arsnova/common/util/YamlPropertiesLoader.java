package net.particify.arsnova.common.util;

import com.google.common.io.CharSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.PropertiesPersister;

public class YamlPropertiesLoader implements PropertiesPersister {
  @Override
  public void load(Properties props, InputStream is) {
    YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
    yaml.setResources(new InputStreamResource(is));
    props.putAll(yaml.getObject());
  }

  @Override
  public void load(Properties props, Reader reader) throws IOException {
    InputStream inputStream = asInputStream(reader, StandardCharsets.UTF_8);
    load(props, inputStream);
  }

  @Override
  public void store(Properties props, OutputStream os, String header) throws IOException {
    throw new UnsupportedEncodingException("Storing is not supported by YamlPropertiesLoader");
  }

  @Override
  public void store(Properties props, Writer writer, String header) throws IOException {
    throw new UnsupportedEncodingException("Storing is not supported by YamlPropertiesLoader");
  }

  @Override
  public void loadFromXml(Properties props, InputStream is) throws IOException {
    throw new UnsupportedEncodingException("Loading from XML is not supported by YamlPropertiesLoader");
  }

  @Override
  public void storeToXml(Properties props, OutputStream os, String header) throws IOException {
    throw new UnsupportedEncodingException("Storing to XML is not supported by YamlPropertiesLoader");
  }

  @Override
  public void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException {
    throw new UnsupportedEncodingException("Storing to XML is not supported by YamlPropertiesLoader");
  }

  private static InputStream asInputStream(Reader reader, Charset charset) throws IOException {
    return new CharSource() {
      @Override public Reader openStream() {
        return reader;
      }
    }.asByteSource(charset).openStream();
  }
}
