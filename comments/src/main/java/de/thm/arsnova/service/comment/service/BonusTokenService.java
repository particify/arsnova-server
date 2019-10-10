package de.thm.arsnova.service.comment.service;

import de.thm.arsnova.service.comment.model.BonusToken;
import de.thm.arsnova.service.comment.model.BonusTokenPK;
import de.thm.arsnova.service.comment.service.persistence.BonusTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BonusTokenService {
    private static final Logger logger = LoggerFactory.getLogger(BonusTokenService.class);

    final BonusTokenRepository repository;

    @Autowired
    public BonusTokenService(
            BonusTokenRepository repository
    ) {
        this.repository = repository;
    }

    public List<BonusToken> get(List<BonusTokenPK> ids) {
        Iterable<BonusToken> it = repository.findAllById(ids);
        List<BonusToken> list = new ArrayList<>();
        it.forEach(list::add);

        return list;
    }

    public BonusToken create(BonusToken b) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        b.setToken(token);
        repository.save(b);

        return b;
    }

    public void delete(BonusToken b) {
        repository.delete(b);
    }

    public void deleteByPK(String roomId, String commentId, String userId) {
        BonusToken bt = repository.findById(new BonusTokenPK(roomId, commentId, userId)).orElse(null);
        repository.delete(bt);
    }

    public List<BonusToken> getByRoomId(String roomId) {
        return repository.findByRoomId(roomId);
    }

    public List<BonusToken> getByUserId(String userId) {
        return repository.findByUserId(userId);
    }
}
