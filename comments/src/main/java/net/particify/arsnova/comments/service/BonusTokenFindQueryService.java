package net.particify.arsnova.comments.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.particify.arsnova.comments.model.BonusToken;
import net.particify.arsnova.comments.model.BonusTokenPK;

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
        bonusTokenIds.add(new BonusTokenPK(bt.getRoomId(), bt.getCommentId(), bt.getUserId()));
      }
    } else if (findQuery.getProperties().getUserId() != null) {
      List<BonusToken> bonusTokenList = bonusTokenService.getByUserId(findQuery.getProperties().getUserId());
      for (BonusToken bt : bonusTokenList) {
        bonusTokenIds.add(new BonusTokenPK(bt.getRoomId(), bt.getCommentId(), bt.getUserId()));
      }
    }
    return bonusTokenIds;
  }
}
