package com.app.playerservicejava.service;

import com.app.playerservicejava.model.Player;
import com.app.playerservicejava.model.Players;
import com.app.playerservicejava.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Service
public class PlayerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerService.class);

    @Autowired
    private PlayerRepository playerRepository;

    public Players getPlayers() {
        Players players = new Players();
        playerRepository.findAll()
                .forEach(players.getPlayers()::add);
        return players;
    }

    public Page<Player> getPlayersPaginated(String birthCountry, Pageable pageable) {
        if (birthCountry != null && !birthCountry.trim().isEmpty()) {
            return playerRepository.findByBirthCountryIgnoreCase(birthCountry.trim(), pageable);
        }
        return playerRepository.findAll(pageable);
    }

    @Cacheable(value = "players", key = "#playerId")
    public Optional<Player> getPlayerById(String playerId) {
        Optional<Player> player = null;

        /* simulated network delay */
        try {
            player = playerRepository.findById(playerId);
            Thread.sleep((long)(Math.random() * 2000));
        } catch (Exception e) {
            LOGGER.error("message=Exception in getPlayerById; exception={}", e.toString());
            return Optional.empty();
        }
        return player;
    }

    @PostMapping
    @CachePut(value = "players", key = "#result.playerId")
    @CacheEvict(value = "playerPages", allEntries = true)  // Clear pagination cache only
    public Player createPlayer(Player player)
    {
        LOGGER.info("Creating new player: {}", player.getPlayerId());
        return playerRepository.save(player);
    }

   /* @DeleteMapping
    public String deletePlayer(String id)
    {
        return playerRepository.deleteById(id);
    }
*/

}
