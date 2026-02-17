package com.app.playerservicejava.repository;
import com.app.playerservicejava.model.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, String> {
    Page<Player> findByBirthCountryIgnoreCase(String birthCountry, Pageable pageable);

}
