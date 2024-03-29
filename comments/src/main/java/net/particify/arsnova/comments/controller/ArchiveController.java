package net.particify.arsnova.comments.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import net.particify.arsnova.comments.model.Archive;
import net.particify.arsnova.comments.model.command.CreateArchiveCommand;
import net.particify.arsnova.common.uuid.UuidHelper;
import net.particify.arsnova.comments.service.ArchiveService;

@RestController("ArchiveController")
@RequestMapping(ArchiveController.REQUEST_MAPPING)
public class ArchiveController extends AbstractEntityController {
  private static final Logger logger = LoggerFactory.getLogger(ArchiveController.class);

  private final ArchiveService service;

  protected static final String REQUEST_MAPPING = "/room/{roomId}/comment/-/archive";
  private static final String GET_BY_ROOM_ID = "/";

  @Autowired
  public ArchiveController(
      final ArchiveService service
  ) {
    this.service = service;
  }

  @GetMapping(GET_MAPPING)
  public Optional<Archive> get(
      @PathVariable final UUID id
  ) {
    return service.get(id);
  }

  @GetMapping(GET_BY_ROOM_ID)
  public List<Archive> getByRoom(
      @PathVariable final UUID roomId
  ) {
    return service.getByRoomId(roomId);
  }

  @PostMapping(POST_MAPPING)
  @ResponseStatus(HttpStatus.CREATED)
  public Archive post(
      @RequestBody final CreateArchiveCommand cmd,
      final HttpServletResponse httpServletResponse
  ) {
    final Archive archive = service.create(cmd);

    final String uri = UriComponentsBuilder.fromPath(REQUEST_MAPPING).path(GET_MAPPING)
        .buildAndExpand(archive.getRoomId(), archive.getId()).toUriString();
    httpServletResponse.setHeader(HttpHeaders.LOCATION, uri);
    httpServletResponse.setHeader(ENTITY_ID_HEADER, UuidHelper.uuidToString(archive.getId()));

    return archive;
  }

  @DeleteMapping(DELETE_MAPPING)
  public void delete(
      @PathVariable final UUID id
  ) {
    service.delete(id);
  }
}
