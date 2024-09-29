package com.weatherweargpt.repository;

import com.weatherweargpt.entity.Dialogue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DialogueRepository extends JpaRepository<Dialogue, Long> {
    List<Dialogue> findByUserEntityUserIdOrderByConversationDateDesc(Long userId);

    Dialogue findTopByUserEntityUserIdOrderByConversationDateDesc(Long userId);

    Optional<Dialogue> findTopByUserEntityUserIdAndDestinationIsNotNullOrderByConversationDateDesc(Long userId);
}