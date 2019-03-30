package de.thm.arsnova.service.comment;

abstract public class AbstractEntityController {
    protected static final String ENTITY_ID_HEADER = "Arsnova-Entity-Id";
    protected static final String DEFAULT_ID_MAPPING = "/{id}";
    protected static final String PATCH_MAPPING = DEFAULT_ID_MAPPING;
    protected static final String GET_MAPPING = DEFAULT_ID_MAPPING;
    protected static final String POST_MAPPING = "/";
    protected static final String PUT_MAPPING = DEFAULT_ID_MAPPING;
    protected static final String DELETE_MAPPING = DEFAULT_ID_MAPPING;
    protected static final String REQUEST_MAPPING = "/";
    protected static final String FIND_MAPPING = "/find";
}
