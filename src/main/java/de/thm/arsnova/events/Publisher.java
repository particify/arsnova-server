package de.thm.arsnova.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Service
public class Publisher implements ApplicationContextAware {

	@Autowired
	private ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext c) {
		this.context = c;
	}

	public void publish(ARSnovaEvent event) {
		this.context.publishEvent(event);
	}
}
