package io.music.metadata.repository.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "artist")
public class ArtistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String name;

    @Version
    private int version;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ArtistAliasEntity> aliases = new ArrayList<>();

    protected ArtistEntity() {
    }

    public ArtistEntity(String name) {
        this.name = name;
    }

    /**
     * Renames the artist. The previous name becomes an alias;
     * if the new name was an alias, that alias is removed, so the current name never also
     * appears as an alias. A change of letter case only (for example "kairo" to "Kairo") isn't kept as an alias,
     * because aliases are compared case-insensitively.
     */
    public void rename(String newName) {
        String previousName = name;
        if (previousName.equals(newName)) {
            return;
        }
        aliases.removeIf(alias -> alias.getAlias().equalsIgnoreCase(newName));
        if (!previousName.equalsIgnoreCase(newName) && !aliasExists(previousName)) {
            aliases.add(new ArtistAliasEntity(this, previousName));
        }
        name = newName;
    }

    private boolean aliasExists(String candidate) {
        return aliases.stream().anyMatch(alias -> alias.getAlias().equalsIgnoreCase(candidate));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public List<ArtistAliasEntity> getAliases() {
        return aliases;
    }

}
