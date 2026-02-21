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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.PrintWriter;
import java.util.*;

@Service
public class PlayerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerService.class);

    @Autowired
    private PlayerRepository playerRepository;

    @Cacheable(value = "allPlayers")
    public Players getPlayers() {
        Players players = new Players();
        playerRepository.findAll()
                .forEach(players.getPlayers()::add);
        return players;
    }

    @Cacheable(
            value = "playerPages",
            key = "(#birthCountry != null ? #birthCountry : 'ALL') + '_' + #pageable.pageNumber + '_' + #pageable.pageSize"
    )
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

    @CachePut(value = "players", key = "#result.playerId")
    @CacheEvict(value = "playerPages", allEntries = true)  // Clear pagination cache only
    public Player createPlayer(Player player)
    {
        LOGGER.info("Creating new player: {}", player.getPlayerId());
        return playerRepository.save(player);
    }

    @CacheEvict(value = {"players", "playerPages"}, allEntries = true)
    public Boolean deletePlayer(String id)
    {
        if (playerRepository.existsById(id))
        {
             playerRepository.deleteById(id);
             return true;
        }
        else
        {
            return false;
        }

    }

    // ✅ Update player - put updated value in cache, clear others
    @CachePut(value = "players", key = "#player.playerId")
    @CacheEvict(value = {"playerPages", "allPlayers", "playerSearch"}, allEntries = true)
    public Player savePlayer (Player player)
    {
        return playerRepository.save(player);

    }

    @Cacheable(value = "playerSearch", key = "#namePrefix + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<Player> searchPlayersByName(String namePrefix, Pageable pageable) {
        LOGGER.info("SEARCH: prefix='{}' page={} size={}",
                namePrefix, pageable.getPageNumber(), pageable.getPageSize());
        return playerRepository.searchByNamePrefix(namePrefix, pageable);
    }

    //bulk create players

    @CacheEvict(value = {"players", "playerPages", "playerSearch"}, allEntries = true)
    public Map<String, Object> bulkCreatePlayers(List<Player> players) {
        List<String> createdIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();

        for (Player player : players) {
            try {
                // Check duplicate
                if (playerRepository.existsById(player.getPlayerId())) {
                    failedIds.add(player.getPlayerId() + " (already exists)");
                    continue;
                }

                // Basic validation
                if (player.getPlayerId() == null || player.getPlayerId().trim().isEmpty()) {
                    failedIds.add("null-id (missing playerId)");
                    continue;
                }
                if (player.getFirstName() == null || player.getFirstName().trim().isEmpty()) {
                    failedIds.add(player.getPlayerId() + " (missing firstName)");
                    continue;
                }

                Player saved = playerRepository.save(player);
                createdIds.add(saved.getPlayerId());

            } catch (Exception e) {
                failedIds.add((player.getPlayerId() != null ? player.getPlayerId() : "unknown") + " (" + e.getMessage() + ")");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("createdIds", createdIds);
        result.put("failedIds", failedIds);
        result.put("message", "Created " + createdIds.size() + ", failed " + failedIds.size());

        return result;
    }

    public void streamAllPlayers(PrintWriter writer) {
        playerRepository.findAll().forEach(player -> {
            writer.println(String.join(",",
                    safe(player.getPlayerId()),
                  //  safe(player.getNameFirst()),
                    //safe(player.getNameLast()),
                    safe(player.getBirthYear()  != null ? player.getBirthYear().toString()  : ""),
                    safe(player.getBirthMonth() != null ? player.getBirthMonth().toString() : ""),
                    safe(player.getBirthDay()   != null ? player.getBirthDay().toString()   : ""),
                    safe(player.getBirthCountry()),
                    safe(player.getBirthState()),
                    safe(player.getBirthCity()),
                    safe(player.getWeight()     != null ? player.getWeight().toString()     : ""),
                    safe(player.getHeight()     != null ? player.getHeight().toString()     : ""),
                    safe(player.getBats()),
                  //  safe(player.getThrowsHand()),
                    safe(player.getDebut()),
                    safe(player.getFinalGame()),
                    safe(player.getRetroId()),
                    safe(player.getBbrefId())
            ));
        });
    }

    private String safe(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @CachePut(value = "players", key = "#id")
    @CacheEvict(value = {"playerPages", "allPlayers", "playerSearch"}, allEntries = true)
    public Optional<Player> patchPlayer(String id, Player patch) {
        Optional<Player> existing = playerRepository.findById(id);

        if (existing.isEmpty()) {
            LOGGER.warn("Player not found for PATCH: {}", id);
            return Optional.empty();
        }

        Player player = existing.get();

        // ✅ Only update non-null fields
       // if (patch.getNameFirst()    != null) player.setNameFirst(patch.getNameFirst());
        //if (patch.getNameLast()     != null) player.setNameLast(patch.getNameLast());
        if (patch.getBirthCity()    != null) player.setBirthCity(patch.getBirthCity());
        if (patch.getBirthState()   != null) player.setBirthState(patch.getBirthState());
        if (patch.getBirthCountry() != null) player.setBirthCountry(patch.getBirthCountry());
        if (patch.getBirthYear()    != null) player.setBirthYear(patch.getBirthYear());
        if (patch.getBirthMonth()   != null) player.setBirthMonth(patch.getBirthMonth());
        if (patch.getBirthDay()     != null) player.setBirthDay(patch.getBirthDay());
        if (patch.getBats()         != null) player.setBats(patch.getBats());
       // if (patch.getThrowsHand()   != null) player.setThrowsHand(patch.getThrowsHand());
        if (patch.getWeight()       != null) player.setWeight(patch.getWeight());
        if (patch.getHeight()       != null) player.setHeight(patch.getHeight());
        if (patch.getDebut()        != null) player.setDebut(patch.getDebut());
        if (patch.getFinalGame()    != null) player.setFinalGame(patch.getFinalGame());

        LOGGER.info("Patching player: {}", id);
        return Optional.of(playerRepository.save(player));
    }


}
