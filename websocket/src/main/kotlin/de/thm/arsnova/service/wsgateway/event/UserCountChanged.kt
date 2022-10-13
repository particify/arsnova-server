package de.thm.arsnova.service.wsgateway.event

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(include=As.WRAPPER_OBJECT, use=Id.NAME)
data class UserCountChanged(
    val userCount: Int
)
