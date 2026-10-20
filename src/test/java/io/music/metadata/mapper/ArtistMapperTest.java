package io.music.metadata.mapper;

import static io.music.metadata.repository.entity.ArtistMother.artist;
import static io.music.metadata.repository.entity.ArtistMother.artistOfTheDay;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import io.music.metadata.api.model.Artist;
import io.music.metadata.api.model.ArtistAlias;
import io.music.metadata.api.model.ArtistOfTheDay;
import io.music.metadata.api.model.ArtistPage;
import io.music.metadata.api.model.ArtistSummary;
import io.music.metadata.repository.entity.ArtistEntity;

class ArtistMapperTest {

    @Test
    void toArtistMapsEveryField() {
        ArtistEntity entity = artist(7, "Kanye West", 2);

        Artist artist = ArtistMapper.toArtist(entity);

        assertThat(artist.getArtistId()).isNotNull();
        assertThat(artist.getName()).isEqualTo("Kanye West");
        assertThat(artist.getVersion()).isEqualTo(2);
        assertThat(artist.getAliases()).isEmpty();
    }

    @Test
    void toArtistListsAliasesInTheOrderTheyWereAdded() {
        ArtistEntity entity = artist(7, "Puff Daddy");
        entity.rename("P. Diddy");
        entity.rename("Diddy");

        Artist artist = ArtistMapper.toArtist(entity);

        assertThat(artist.getName()).isEqualTo("Diddy");
        assertThat(artist.getAliases()).containsExactly(
                new ArtistAlias("Puff Daddy"), new ArtistAlias("P. Diddy"));
    }

    @Test
    void toSummaryMapsIdAndCurrentName() {
        assertThat(ArtistMapper.toSummary(artist(7, "Nova Lane")))
                .isEqualTo(new ArtistSummary(7L, "Nova Lane"));
    }

    @Test
    void toPageMapsPageMetadataAndContent() {
        PageImpl<ArtistEntity> page = new PageImpl<>(
                List.of(artist(3, "Kairo"), artist(4, "Ama Sol")), PageRequest.of(1, 2), 5);

        ArtistPage artistPage = ArtistMapper.toPage(page);

        assertThat(artistPage.getPage()).isEqualTo(1);
        assertThat(artistPage.getSize()).isEqualTo(2);
        assertThat(artistPage.getTotalElements()).isEqualTo(5L);
        assertThat(artistPage.getTotalPages()).isEqualTo(3);
        assertThat(artistPage.getContent())
                .containsExactly(new ArtistSummary(3L, "Kairo"), new ArtistSummary(4L, "Ama Sol"));
    }

    @Test
    void toPageMapsAnEmptyPage() {
        ArtistPage artistPage = ArtistMapper.toPage(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(artistPage.getContent()).isEmpty();
        assertThat(artistPage.getTotalElements()).isZero();
        assertThat(artistPage.getTotalPages()).isZero();
    }

    @Test
    void toArtistOfTheDayMapsTheDayAndTheArtist() {
        LocalDate day = LocalDate.of(2026, 11, 1);

        ArtistOfTheDay pick = ArtistMapper.toArtistOfTheDay(artistOfTheDay(day, artist(7, "Nova Lane")));

        assertThat(pick.getDate()).isEqualTo(day);
        assertThat(pick.getArtist()).isEqualTo(new ArtistSummary(7L, "Nova Lane"));
    }

}
