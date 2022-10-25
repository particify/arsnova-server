package net.particify.arsnova.comments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource(
    value = {"classpath:arsnova.comment.properties.example", "file:/etc/arsnova/arsnova.comment.properties"},
    ignoreResourceNotFound = true,
    encoding = "UTF-8"
)
public class CommentsApplication extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(CommentsApplication.class);
  }

  public static void main(String[] args) {
    SpringApplication.run(CommentsApplication.class, args);
  }
}
