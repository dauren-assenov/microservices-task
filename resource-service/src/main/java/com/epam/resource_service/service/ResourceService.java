package com.epam.resource_service.service;


import com.epam.resource_service.entity.Resource;
import com.epam.resource_service.dto.SongMetadataDto;
import com.epam.resource_service.exception.InvalidCsvException;
import com.epam.resource_service.exception.InvalidIdException;
import com.epam.resource_service.exception.ResourceNotFoundException;
import com.epam.resource_service.repository.ResourceRepository;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository repository;
    private final RestTemplate restTemplate;

    @Value("${song.service.url}")
    private String songServiceUrl;

    public ResourceService(ResourceRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Integer processAndSaveResource(byte[] audioData) {
        Resource resource = new Resource();
        resource.setData(audioData);
        resource = repository.save(resource);

        SongMetadataDto metadataDto = extractMetadata(audioData, resource.getId());

        try {
            restTemplate.postForEntity(songServiceUrl, metadataDto, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send metadata to Song Service", e);
        }

        return resource.getId();
    }

    public byte[] getResource(String idStr) {
        Integer id = validateAndParseId(idStr);
        return repository.findById(id)
                .map(Resource::getData)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + id + " not found"));
    }

    @Transactional
    public List<Integer> deleteResources(String csvIds) {
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

        if (!deletedIds.isEmpty()) {
            try {
                String deletedCsv = deletedIds.stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");

                restTemplate.delete(songServiceUrl + "?id=" + deletedCsv);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete metadata from Song Service", e);
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

    private SongMetadataDto extractMetadata(byte[] data, Integer resourceId) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            Mp3Parser parser = new Mp3Parser();

            parser.parse(input, handler, metadata, context);

            String durationStr = metadata.get("xmpDM:duration");
            String formattedDuration = formatDuration(durationStr);

            return new SongMetadataDto(
                    resourceId,
                    metadata.get("dc:title") != null ? metadata.get("dc:title") : "Unknown",
                    metadata.get("xmpDM:artist") != null ? metadata.get("xmpDM:artist") : "Unknown",
                    metadata.get("xmpDM:album") != null ? metadata.get("xmpDM:album") : "Unknown",
                    formattedDuration,
                    metadata.get("xmpDM:releaseDate") != null ? metadata.get("xmpDM:releaseDate").substring(0, 4) : "1900"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract metadata", e);
        }
    }

    private String formatDuration(String durationMsStr) {
        if (durationMsStr == null) return "00:00";
        try {
            double durationMs = Double.parseDouble(durationMsStr);
            long totalSeconds = (long) (durationMs / 1000);
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        } catch (NumberFormatException e) {
            return "00:00";
        }
    }
}
