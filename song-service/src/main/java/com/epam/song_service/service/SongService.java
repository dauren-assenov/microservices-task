package com.epam.song_service.service;

import com.epam.song_service.entity.Song;
import com.epam.song_service.dto.SongDto;
import com.epam.song_service.exception.InvalidCsvException;
import com.epam.song_service.exception.InvalidIdException;
import com.epam.song_service.exception.SongAlreadyExistsException;
import com.epam.song_service.exception.SongNotFoundException;
import com.epam.song_service.repository.SongRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SongService {

    private final SongRepository repository;

    public SongService(SongRepository repository) {
        this.repository = repository;
    }

    public Integer createSong(SongDto dto) {
        if (repository.existsById(dto.getId())) {
            throw new SongAlreadyExistsException("Metadata for resource ID=" + dto.getId() + " already exists");
        }

        Song song = new Song();
        song.setId(dto.getId());
        song.setName(dto.getName());
        song.setArtist(dto.getArtist());
        song.setAlbum(dto.getAlbum());
        song.setDuration(dto.getDuration());
        song.setYear(dto.getYear());

        repository.save(song);
        return song.getId();
    }

    public SongDto getSong(String idStr) {
        Integer id = validateAndParseId(idStr);
        return repository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new SongNotFoundException("Song metadata for ID=" + id + " not found"));
    }

    public List<Integer> deleteSongs(String csvIds) {
        if (csvIds != null && csvIds.length() > 200) {
            throw new InvalidCsvException("CSV string is too long: received " + csvIds.length() + " characters, maximum allowed is 200");
        }

        List<Integer> deletedIds = new ArrayList<>();
        if (csvIds == null || csvIds.isBlank()) return deletedIds;

        String[] ids = csvIds.split(",");
        for (String idStr : ids) {
            try {
                Integer id = Integer.parseInt(idStr.trim());
                if (id <= 0) throw new NumberFormatException();

                if (repository.existsById(id)) {
                    repository.deleteById(id);
                    deletedIds.add(id);
                }
            } catch (NumberFormatException e) {
                throw new InvalidCsvException("Invalid ID format: '" + idStr + "'. Only positive integers are allowed");
            }
        }
        return deletedIds;
    }

    private Integer validateAndParseId(String idStr) {
        try {
            double val = Double.parseDouble(idStr);
            if (val <= 0 || val != Math.floor(val) || idStr.contains(".")) {
                throw new InvalidIdException("Invalid value '" + idStr + "' for ID. Must be a positive integer");
            }
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            throw new InvalidIdException("Invalid value '" + idStr + "' for ID. Must be a positive integer");
        }
    }

    private SongDto mapToDto(Song song) {
        SongDto dto = new SongDto();
        dto.setId(song.getId());
        dto.setName(song.getName());
        dto.setArtist(song.getArtist());
        dto.setAlbum(song.getAlbum());
        dto.setDuration(song.getDuration());
        dto.setYear(song.getYear());
        return dto;
    }
}
