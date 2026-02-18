package com.epam.resource_service.controller;


import com.epam.resource_service.dto.DeletedIdsResponse;
import com.epam.resource_service.dto.IdResponse;
import com.epam.resource_service.exception.InvalidFileFormatException;
import com.epam.resource_service.service.ResourceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping(consumes = "audio/mpeg")
    public ResponseEntity<IdResponse> uploadResource(@RequestBody byte[] data) {
        Integer id = resourceService.processAndSaveResource(data);
        return ResponseEntity.ok(new IdResponse(id));
    }

    @PostMapping(consumes = "!audio/mpeg")
    public ResponseEntity<IdResponse> uploadResourceInvalidFormat(@RequestHeader("Content-Type") String contentType) {
        throw new InvalidFileFormatException("Invalid file format: " + contentType + ". Only MP3 files are allowed");
    }

    @GetMapping(value = "/{id}", produces = "audio/mpeg")
    public ResponseEntity<byte[]> getResource(@PathVariable String id) {
        byte[] data = resourceService.getResource(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentLength(data.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    @DeleteMapping
    public ResponseEntity<DeletedIdsResponse> deleteResources(@RequestParam(value = "id", required = false) String ids) {
        List<Integer> deletedIds = resourceService.deleteResources(ids);
        return ResponseEntity.ok(new DeletedIdsResponse(deletedIds));
    }
}
