package com.weatherweargpt.repository;

import com.weatherweargpt.entity.OutfitImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitImageRepository extends JpaRepository<OutfitImageEntity, Long> {
    List<OutfitImageEntity> findByUserEntityUserId(Long userId);
    OutfitImageEntity findTopByDialogueDialogIdOrderByCreatedAtDesc(Long dialogId);
}