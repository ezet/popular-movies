package no.ezet.fasttrack.popularmovies.repository;

import javax.inject.Inject;

import no.ezet.fasttrack.popularmovies.api.ApiService;
import no.ezet.fasttrack.popularmovies.api.model.ApiList;
import no.ezet.fasttrack.popularmovies.api.model.PostResponse;
import no.ezet.fasttrack.popularmovies.db.MovieCacheDao;
import no.ezet.fasttrack.popularmovies.api.model.Movie;
import retrofit2.Call;


public class FavoriteRepository extends MutableMovieRepository {

    @Inject
    FavoriteRepository(MovieCacheDao movieCacheDao, ApiService apiService) {
        super(Movie.FAVORITE, movieCacheDao, apiService);
    }

    @Override
    public Call<ApiList<Movie>> createApiCall(ApiService apiService) {
        return apiService.getFavoriteMovies();
    }

    @Override
    protected Call<PostResponse> createAddMovieCall(ApiService apiService, Movie movie) {
        return apiService.setFavoriteMovie(movie.getId(), true);
    }

    @Override
    protected Call<PostResponse> createRemoveMovieCall(ApiService apiService, Movie movie) {
        return apiService.setFavoriteMovie(movie.getId(), false);
    }
}
