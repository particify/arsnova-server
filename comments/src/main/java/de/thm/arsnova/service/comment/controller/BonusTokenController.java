package de.thm.arsnova.service.comment.controller;

import de.thm.arsnova.service.comment.model.BonusToken;
import de.thm.arsnova.service.comment.model.BonusTokenPK;
import de.thm.arsnova.service.comment.service.BonusTokenFindQueryService;
import de.thm.arsnova.service.comment.service.BonusTokenService;
import de.thm.arsnova.service.comment.service.FindQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController("BonusTokenController")
@RequestMapping("/room/{roomId}/bonustoken")
public class BonusTokenController extends AbstractEntityController {
    private static final Logger logger = LoggerFactory.getLogger(BonusTokenController.class);

    protected static final String REQUEST_MAPPING = "/bonustoken";
    protected static final String DELETE_MAPPING = "/deleteby";

    private final BonusTokenService service;
    private final BonusTokenFindQueryService findQueryService;

    @Autowired
    public BonusTokenController(
            BonusTokenService service,
            BonusTokenFindQueryService findQueryService
    ) {
        this.service = service;
        this.findQueryService = findQueryService;
    }

    @DeleteMapping(DELETE_MAPPING)
    public void delete(
            @RequestParam("roomid") final String roomId,
            @RequestParam(value = "commentid", required = false) final String commentId,
            @RequestParam(value = "userid", required = false) final String userId
    ) {
        if (commentId != null && userId != null) {
            logger.debug("Searching for and deleting bonus token, roomId = {}, commentId = {}", roomId, commentId);

            service.deleteByPK(roomId, commentId, userId);
        } else {
            logger.debug("Searching for bonus tokens with roomId = {}", roomId);
            List<BonusToken> list = service.getByRoomId(roomId);
            for (BonusToken t : list) {
                logger.debug("Deleting BonusToken = {}", t.toString());
                service.delete(t);
            }
        }
    }

    @PostMapping(FIND_MAPPING)
    public List<BonusToken> find(@RequestBody final FindQuery<BonusToken> findQuery) {
        logger.debug("Resolving find query: {}", findQuery);

        Set<BonusTokenPK> ids = findQueryService.resolveQuery(findQuery);

        logger.debug("Resolved find query to IDs: {}", ids);

        return service.get(new ArrayList<>(ids));
    }
}
