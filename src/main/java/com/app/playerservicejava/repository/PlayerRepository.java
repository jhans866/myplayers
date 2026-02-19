package com.app.playerservicejava.repository;
import com.app.playerservicejava.model.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, String> {
    Page<Player> findByBirthCountryIgnoreCase(String birthCountry, Pageable pageable);

    Page<Player> findByFirstNameStartingWithIgnoreCase(String prefix, Pageable pageable);

    // Search by last name prefix
    Page<Player> findByLastNameStartingWithIgnoreCase(String prefix, Pageable pageable);

    // Search by either first or last name prefix (combined)
    @Query("SELECT p FROM Player p WHERE LOWER(p.firstName) LIKE LOWER(CONCAT(:prefix, '%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT(:prefix, '%'))")
    Page<Player> searchByNamePrefix(@Param("prefix") String prefix, Pageable pageable);

}
