package com.zwriter.controller;

import com.zwriter.entity.NovelInfo;
import com.zwriter.repository.NovelInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelInfoRepository novelInfoRepository;

    @GetMapping
    public List<NovelInfo> listNovels() {
        return novelInfoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<NovelInfo> getNovel(@PathVariable Long id) {
        return novelInfoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public NovelInfo createNovel(@RequestBody NovelInfo novelInfo) {
        return novelInfoRepository.save(novelInfo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NovelInfo> updateNovel(@PathVariable Long id, @RequestBody NovelInfo novelInfo) {
        if (!novelInfoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        novelInfo.setId(id);
        return ResponseEntity.ok(novelInfoRepository.save(novelInfo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNovel(@PathVariable Long id) {
        if (!novelInfoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        novelInfoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
