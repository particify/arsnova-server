package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;

import net.particify.arsnova.core.model.serialization.View;

@JsonView(View.Public.class)
public record ContentLicenseAttribution(String contentId, String license, String attribution) {}
