package de.thm.arsnova.service.comment.service;

import de.thm.arsnova.service.comment.model.BonusToken;
import de.thm.arsnova.service.comment.model.BonusTokenPK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BonusTokenFindQueryService {
    private final BonusTokenService bonusTokenService;

    @Autowired
    public BonusTokenFindQueryService(
            BonusTokenService bonusTokenService
    ) {
        this.bonusTokenService = bonusTokenService;
    }

    public Set<BonusTokenPK> resolveQuery(final FindQuery<BonusToken> findQuery) {
        Set<BonusTokenPK> bonusTokenIds = new HashSet<>();
        if (findQuery.getProperties().getRoomId() != null) {
            List<BonusToken> bonusTokenList = bonusTokenService.getByRoomId(findQuery.getProperties().getRoomId());
            for (BonusToken bt : bonusTokenList) {
                bonusTokenIds.add(new BonusTokenPK(bt.getRoomId(), bt.getUserId()));
            }
        } else if (findQuery.getProperties().getUserId() != null) {
            List<BonusToken> bonusTokenList = bonusTokenService.getByUserId(findQuery.getProperties().getUserId());
            for (BonusToken bt : bonusTokenList) {
                bonusTokenIds.add(new BonusTokenPK(bt.getRoomId(), bt.getUserId()));
            }
        }
        return bonusTokenIds;
    }
}
