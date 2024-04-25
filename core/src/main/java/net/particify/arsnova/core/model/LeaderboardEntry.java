package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;

import net.particify.arsnova.core.model.serialization.View;

@JsonView(View.Public.class)
public record LeaderboardEntry(RoomUserAlias userAlias, int score, LeaderboardCurrentResult currentResult) {}
