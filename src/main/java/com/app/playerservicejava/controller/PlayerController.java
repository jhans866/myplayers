package com.app.playerservicejava.controller;

import com.app.playerservicejava.config.PlayerAlreadyExistsException;
import com.app.playerservicejava.config.PlayerNotFoundException;
import com.app.playerservicejava.model.Player;
import com.app.playerservicejava.model.Players;
import com.app.playerservicejava.service.PlayerService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping(value = "v1/players", produces = { MediaType.APPLICATION_JSON_VALUE })
public class PlayerController {
    @Resource
    private PlayerService playerService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerController.class);

/*    @GetMapping("/all")
    public ResponseEntity<Players> getPlayers() {
        Players players = playerService.getPlayers();
        return ok(players);
    }*/

    @GetMapping
    public ResponseEntity<?> getplayers(@RequestParam(required = false) String country,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size)
    {
        if(size >100 || size<1) size = 20;
        Pageable pageable = PageRequest.of(page,size,Sort.by("playerID").ascending());
        Page<Player> pageResult = playerService.getPlayersPaginated(country,pageable);

        return ResponseEntity.ok(Map.of(
                "content",pageResult.getContent(),
                "page",pageResult.getNumber(),
                "size",pageResult.getSize(),
                "totalElements",pageResult.getTotalElements(),
                "totalPages",pageResult.getTotalPages()
                ));
    }


   /* @GetMapping("/{id}") ****** this is also correct  refer to this also jhansi
    public ResponseEntity<Player> getPlayerById(@PathVariable("id") String id) {
        Optional<Player> player = playerService.getPlayerById(id);

        if (player.isPresent()) {
            return new ResponseEntity<>(player.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }*/

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayerById(@PathVariable("id") String id) {
        return playerService.getPlayerById(id)
                .map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                .orElseThrow(() -> new PlayerNotFoundException(id));
    }

    @GetMapping("/stat/{id}")
    public ResponseEntity<String> getStats(@PathVariable("id")String id)
    {
        return playerService.getPlayerById(id)
                .map(p -> new ResponseEntity<>(p.getThrowStats(), HttpStatus.OK))
                .orElseThrow(() -> new PlayerNotFoundException(id));
       /* Optional <Player> player = playerService.getPlayerById(id);
        if(player.isPresent())
        {
            System.out.println("player.get().getThrowStats() : " +player.get().getThrowStats());
            return new ResponseEntity<>(player.get().getThrowStats(),HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }*/
    }

   /* @PostMapping. ******* this is also correct  refer to this also jhansi
    public ResponseEntity<Player> createPlayer(@RequestBody Player player)
    {
        Optional<Player> playerCheck = playerService.getPlayerById(player.getPlayerId());

        if (playerCheck.isPresent())
        {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        else{
            Player savePlayer = playerService.createPlayer(player);
            return new ResponseEntity<>(savePlayer,HttpStatus.CREATED);
        }

    }*/

    @PostMapping
    public ResponseEntity<Player> createPlayer(@Valid @RequestBody Player player) {
        if (playerService.getPlayerById(player.getPlayerId()).isPresent()) {
            throw new PlayerAlreadyExistsException(player.getPlayerId());
        }
        return new ResponseEntity<>(playerService.createPlayer(player), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer( @PathVariable("id") String id)
    {
        if (!playerService.deletePlayer(id)) {
            throw new PlayerNotFoundException(id);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
       /* boolean deleted = playerService.deletePlayer(id);
        //Optional<Player> player = playerService.deletePlayer(id);

        if (deleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }*/
        //return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping()
    public ResponseEntity<Player> updatePlayer(@RequestBody Player player)
    {
        Optional<Player> playerCheck = playerService.getPlayerById(player.getPlayerId());
        if(!playerCheck.isEmpty())
        {
            Player savePlayer = playerService.savePlayer(player);
            return new ResponseEntity<>(savePlayer,HttpStatus.OK);
        }
        else
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

    }

    @PutMapping("/{id}")
    public ResponseEntity<Player> updatePlayer(
            @PathVariable("id") String id, @RequestBody Player player) {
        if (playerService.getPlayerById(id).isEmpty()) {
            throw new PlayerNotFoundException(id);
        }
        player.setPlayerId(id);
        return new ResponseEntity<>(playerService.savePlayer(player), HttpStatus.OK);
    }

    //search players by prefix
    @GetMapping("/search")
    public ResponseEntity<?> searchPlayer(
            @RequestParam(required = true) String startsWith,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (startsWith == null || startsWith.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "startsWith must be at least 2 characters"));
        }

        if (size > 100 || size < 1) size = 20;
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("firstName").ascending().and(Sort.by("lastName").ascending()));

        Page<Player> results = playerService.searchPlayersByName(startsWith.trim(), pageable);

        return ResponseEntity.ok(Map.of(
                "content", results.getContent(),
                "page", results.getNumber(),
                "size", results.getSize(),
                "totalElements", results.getTotalElements(),
                "totalPages", results.getTotalPages(),
                "searchTerm", startsWith
        ));
    }

    // bulk create
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkCreatePlayers(@RequestBody List<Player> players) {
        Map<String, Object> result = playerService.bulkCreatePlayers(players);
        return ResponseEntity.ok(result);
    }

    // ===================== STORY 19 - Export as CSV =====================

    @GetMapping("/export")
    public void exportPlayersAsCsv(HttpServletResponse response) throws IOException {
        LOGGER.info("CSV export requested");

        // ✅ Set response headers
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"players.csv\"");

        PrintWriter writer = response.getWriter();

        // ✅ Write CSV header row
        writer.println("playerId,firstName,lastName,birthYear,birthMonth,birthDay," +
                "birthCountry,birthState,birthCity,weight,height,bats,throwsHand," +
                "debut,finalGame,retroId,bbrefId");

        // ✅ Stream players row by row - no full list in memory
        playerService.streamAllPlayers(writer);

        writer.flush();
        LOGGER.info("CSV export completed");
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Player> patchPlayer(
            @PathVariable String id,
            @RequestBody Player patch) {

        LOGGER.info("PATCH request for player: {}", id);
        return playerService.patchPlayer(id, patch)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
