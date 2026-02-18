package com.epam.song_service.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "songs")
@Data
public class Song {
    @Id
    private Integer id;
    private String name;
    private String artist;
    private String album;
    private String duration;
    private String year;
}