package de.thm.arsnova.service;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.model.TextRenderingOptions;

@Service
public class TextRenderingService {
	private static final String ENDPOINT = "/render";
	private static final Logger logger = LoggerFactory.getLogger(TextRenderingService.class);

	private final boolean enabled;
	private final WebClient webClient;

	public TextRenderingService(final SystemProperties systemProperties) {
		this.enabled = systemProperties.getFormattingService().isEnabled();
		final String host = systemProperties.getFormattingService().getHostUrl();
		this.webClient = WebClient.create(host + ENDPOINT);
	}

	@Cacheable(value = "rendered-texts", unless = "#result == null")
	public String renderText(final String unrenderedText, final TextRenderingOptions textRenderingOptions)
			throws IOException {
		if (!enabled || unrenderedText == null) {
			return null;
		}

		final RenderingRequestEntity requestEntity = new RenderingRequestEntity();
		requestEntity.setText(unrenderedText);
		requestEntity.setOptions(textRenderingOptions);
		logger.trace("Sending text to formatting service: {}", unrenderedText.hashCode());
		final RenderingResponseEntity responseEntity = webClient.post()
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.bodyValue(requestEntity)
				.retrieve()
				.bodyToMono(RenderingResponseEntity.class)
				.doOnError(e -> logger.error("Text rendering request failed.", e))
				.onErrorResume(e -> Mono.just(new RenderingResponseEntity()))
				.block();
		return responseEntity.html;
	}

	private static class RenderingRequestEntity {
		private String text;
		private TextRenderingOptions options;

		public String getText() {
			return text;
		}

		public void setText(final String text) {
			this.text = text;
		}

		public TextRenderingOptions getOptions() {
			return options;
		}

		public void setOptions(final TextRenderingOptions options) {
			this.options = options;
		}
	}

	private static class RenderingResponseEntity {
		private String html;

		public String getHtml() {
			return html;
		}

		public void setHtml(final String html) {
			this.html = html;
		}
	}
}
