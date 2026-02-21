package com.app.playerservicejava;

import com.app.playerservicejava.model.Player;
import com.app.playerservicejava.model.Players;
import com.app.playerservicejava.repository.PlayerRepository;
import com.app.playerservicejava.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    private Player testPlayer;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        testPlayer = new Player();
        testPlayer.setPlayerId("player123");
        // testPlayer.setFirstName("John");
        // testPlayer.setLastName("Doe");
        testPlayer.setBirthCountry("USA");
        testPlayer.setBirthYear("1990");
        testPlayer.setWeight("200");
        testPlayer.setHeight("75");

        testPageable = PageRequest.of(0, 10);
    }

    @Test
    void testGetPlayers() {
        List<Player> playerList = Arrays.asList(testPlayer);
        when(playerRepository.findAll()).thenReturn(playerList);

        Players result = playerService.getPlayers();

        assertNotNull(result);
        assertEquals(1, result.getPlayers().size());
        verify(playerRepository, times(1)).findAll();
    }

    @Test
    void testGetPlayersEmpty() {
        when(playerRepository.findAll()).thenReturn(Collections.emptyList());

        Players result = playerService.getPlayers();

        assertNotNull(result);
        assertEquals(0, result.getPlayers().size());
    }

   /* @Test
    void testGetPlayersPaginatedWithCountry() {
        Page<Player> playerPage = new PageImpl<>(Arrays.asList(testPlayer), testPageable, 1);
        when(playerRepository.findByBirthCountryIgnoreCase("usa", testPageable))
                .thenReturn(playerPage);

        Page<Player> result = playerService.getPlayersPaginated("USA", testPageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("USA", result.getContent().get(0).getBirthCountry());
        verify(playerRepository, times(1)).findByBirthCountryIgnoreCase("usa", testPageable);
    }*/

    @Test
    void testGetPlayersPaginatedWithoutCountry() {
        Page<Player> playerPage = new PageImpl<>(Arrays.asList(testPlayer), testPageable, 1);
        when(playerRepository.findAll(testPageable)).thenReturn(playerPage);

        Page<Player> result = playerService.getPlayersPaginated(null, testPageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(playerRepository, times(1)).findAll(testPageable);
    }

    @Test
    void testGetPlayersPaginatedWithEmptyCountry() {
        Page<Player> playerPage = new PageImpl<>(Arrays.asList(testPlayer), testPageable, 1);
        when(playerRepository.findAll(testPageable)).thenReturn(playerPage);

        Page<Player> result = playerService.getPlayersPaginated("   ", testPageable);

        assertNotNull(result);
        verify(playerRepository, times(1)).findAll(testPageable);
    }

    @Test
    void testGetPlayerById() throws InterruptedException {
        when(playerRepository.findById("player123")).thenReturn(Optional.of(testPlayer));

        Optional<Player> result = playerService.getPlayerById("player123");

        assertTrue(result.isPresent());
        assertEquals("player123", result.get().getPlayerId());
        verify(playerRepository, times(1)).findById("player123");
    }

    @Test
    void testGetPlayerByIdNotFound() {
        when(playerRepository.findById("notfound")).thenReturn(Optional.empty());

        Optional<Player> result = playerService.getPlayerById("notfound");

        assertFalse(result.isPresent());
        verify(playerRepository, times(1)).findById("notfound");
    }

    @Test
    void testGetPlayerByIdException() {
        when(playerRepository.findById(anyString())).thenThrow(new RuntimeException("Database error"));

        Optional<Player> result = playerService.getPlayerById("player123");

        assertFalse(result.isPresent());
    }

    @Test
    void testCreatePlayer() {
        when(playerRepository.save(testPlayer)).thenReturn(testPlayer);

        Player result = playerService.createPlayer(testPlayer);

        assertNotNull(result);
        assertEquals("player123", result.getPlayerId());
        verify(playerRepository, times(1)).save(testPlayer);
    }

    @Test
    void testDeletePlayerSuccess() {
        when(playerRepository.existsById("player123")).thenReturn(true);
        doNothing().when(playerRepository).deleteById("player123");

        Boolean result = playerService.deletePlayer("player123");

        assertTrue(result);
        verify(playerRepository, times(1)).deleteById("player123");
    }

    @Test
    void testDeletePlayerNotFound() {
        when(playerRepository.existsById("notfound")).thenReturn(false);

        Boolean result = playerService.deletePlayer("notfound");

        assertFalse(result);
        verify(playerRepository, never()).deleteById(anyString());
    }

    @Test
    void testSavePlayer() {
        when(playerRepository.save(testPlayer)).thenReturn(testPlayer);

        Player result = playerService.savePlayer(testPlayer);

        assertNotNull(result);
        assertEquals("player123", result.getPlayerId());
        verify(playerRepository, times(1)).save(testPlayer);
    }

    @Test
    void testSearchPlayersByName() {
        Page<Player> playerPage = new PageImpl<>(Arrays.asList(testPlayer), testPageable, 1);
        when(playerRepository.searchByNamePrefix("Jo", testPageable)).thenReturn(playerPage);

        Page<Player> result = playerService.searchPlayersByName("Jo", testPageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(playerRepository, times(1)).searchByNamePrefix("Jo", testPageable);
    }

    @Test
    void testSearchPlayersByNameEmpty() {
        Page<Player> playerPage = new PageImpl<>(Collections.emptyList(), testPageable, 0);
        when(playerRepository.searchByNamePrefix("XYZ", testPageable)).thenReturn(playerPage);

        Page<Player> result = playerService.searchPlayersByName("XYZ", testPageable);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void testBulkCreatePlayersAllSuccess() {
        Player player1 = new Player();
        player1.setPlayerId("p1");
        player1.setFirstName("Player1");

        Player player2 = new Player();
        player2.setPlayerId("p2");
        player2.setFirstName("Player2");

        when(playerRepository.existsById(anyString())).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenReturn(player1).thenReturn(player2);

        Map<String, Object> result = playerService.bulkCreatePlayers(Arrays.asList(player1, player2));

        assertEquals(2, ((List<?>) result.get("createdIds")).size());
        assertEquals(0, ((List<?>) result.get("failedIds")).size());
    }

    @Test
    void testBulkCreatePlayersDuplicate() {
        Player player = new Player();
        player.setPlayerId("duplicate");
        player.setFirstName("Test");

        when(playerRepository.existsById("duplicate")).thenReturn(true);

        Map<String, Object> result = playerService.bulkCreatePlayers(Arrays.asList(player));

        assertEquals(0, ((List<?>) result.get("createdIds")).size());
        assertEquals(1, ((List<?>) result.get("failedIds")).size());
    }

    @Test
    void testBulkCreatePlayersMissingPlayerId() {
        Player player = new Player();
        player.setPlayerId("");
        player.setFirstName("Test");

        Map<String, Object> result = playerService.bulkCreatePlayers(Arrays.asList(player));

        assertEquals(0, ((List<?>) result.get("createdIds")).size());
        assertEquals(1, ((List<?>) result.get("failedIds")).size());
    }

    @Test
    void testBulkCreatePlayersMissingFirstName() {
        Player player = new Player();
        player.setPlayerId("p1");
        player.setFirstName("");

        Map<String, Object> result = playerService.bulkCreatePlayers(Arrays.asList(player));

        assertEquals(0, ((List<?>) result.get("createdIds")).size());
        assertEquals(1, ((List<?>) result.get("failedIds")).size());
    }

    @Test
    void testStreamAllPlayers() {
        when(playerRepository.findAll()).thenReturn(Arrays.asList(testPlayer));
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);

        playerService.streamAllPlayers(writer);
        writer.flush();

        String output = sw.toString();
        assertNotNull(output);
        assertTrue(output.contains("player123"));
    }

    @Test
    void testStreamAllPlayersEmpty() {
        when(playerRepository.findAll()).thenReturn(Collections.emptyList());
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);

        playerService.streamAllPlayers(writer);
        writer.flush();

        String output = sw.toString();
        assertEquals("", output.trim());
    }
}