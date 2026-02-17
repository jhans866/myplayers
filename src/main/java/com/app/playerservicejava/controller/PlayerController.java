package com.app.playerservicejava.controller;

import com.app.playerservicejava.model.Player;
import com.app.playerservicejava.model.Players;
import com.app.playerservicejava.service.PlayerService;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping(value = "v1/players", produces = { MediaType.APPLICATION_JSON_VALUE })
public class PlayerController {
    @Resource
    private PlayerService playerService;

    @GetMapping("/all")
    public ResponseEntity<Players> getPlayers() {
        Players players = playerService.getPlayers();
        return ok(players);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getPlayers(
            @RequestParam(required = false) String team,  // birthCountry filter
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > 100 || size < 1) size = 20;
        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending()
                .and(Sort.by("lastName").ascending()));

        Page<Player> pageResult = playerService.getPlayersPaginated(team, pageable);

        // Raw Page JSON: content, totalElements, totalPages, number (page), size
        return ResponseEntity.ok(Map.of(
                "content", pageResult.getContent(),
                "page", pageResult.getNumber(),
                "size", pageResult.getSize(),
                "totalElements", pageResult.getTotalElements(),
                "totalPages", pageResult.getTotalPages()
        ));
    }


    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayerById(@PathVariable("id") String id) {
        Optional<Player> player = playerService.getPlayerById(id);

        if (player.isPresent()) {
            return new ResponseEntity<>(player.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/stat/{id}")
    public ResponseEntity<String> getStats(@PathVariable("id")String id)
    {
        Optional <Player> player = playerService.getPlayerById(id);
        if(player.isPresent())
        {
            System.out.println("player.get().getThrowStats() : " +player.get().getThrowStats());
            return new ResponseEntity<>(player.get().getThrowStats(),HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody Player player)
    {
        Optional<Player> playerCheck = playerService.getPlayerById(player.getPlayerId());

        if (playerCheck.isPresent())
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        else{
            Player savePlayer = playerService.createPlayer(player);
            return new ResponseEntity<>(savePlayer,HttpStatus.CREATED);
        }

    }

    @DeleteMapping
    public ResponseEntity<Void> deletePlayer( @PathVariable("id") String id)
    {
        Optional<Player> player = playerService.deletePlayer(id);

        if (player.isPresent()) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        //return new ResponseEntity<>(HttpStatus.OK);
    }

}
