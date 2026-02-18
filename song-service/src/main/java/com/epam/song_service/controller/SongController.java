package com.epam.song_service.controller;

import com.epam.song_service.dto.DeletedIdsResponse;
import com.epam.song_service.dto.IdResponse;
import com.epam.song_service.dto.SongDto;
import com.epam.song_service.service.SongService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/songs")
public class SongController {

    private final SongService service;

    public SongController(SongService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createSong(@Valid @RequestBody SongDto songDto) {
        Integer id = service.createSong(songDto);
        return ResponseEntity.ok(new IdResponse(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongDto> getSong(@PathVariable String id) {
        SongDto song = service.getSong(id);
        return ResponseEntity.ok(song);
    }

    @DeleteMapping
    public ResponseEntity<DeletedIdsResponse> deleteSongs(@RequestParam(value = "id", required = false) String ids) {
        List<Integer> deletedIds = service.deleteSongs(ids);
        return ResponseEntity.ok(new DeletedIdsResponse(deletedIds));
    }
}
