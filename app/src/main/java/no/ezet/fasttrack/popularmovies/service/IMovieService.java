package no.ezet.fasttrack.popularmovies.service;

import no.ezet.fasttrack.popularmovies.model.MovieList;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface IMovieService {

    @GET("movie/{sortBy}")
    Call<MovieList> getMovies(@Path("sortBy") String sortBy);


}