package io.music.metadata.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "artist_alias")
public class ArtistAliasEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private ArtistEntity artist;

    @Column(nullable = false, length = 500)
    private String alias;

    protected ArtistAliasEntity() {
    }

    ArtistAliasEntity(ArtistEntity artist, String alias) {
        this.artist = artist;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

}
