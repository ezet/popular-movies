package no.ezet.fasttrack.popularmovies.viewmodel;

import no.ezet.fasttrack.popularmovies.model.Movie;

public class MovieListItem {

    public final int id;

    public final String posterPath;

    public MovieListItem(int id, String posterPath) {
        this.id = id;
        this.posterPath = posterPath;
    }

    public static MovieListItem create(Movie movie) {
        return new MovieListItem(movie.getId(), movie.getPosterPath());
    }
}
