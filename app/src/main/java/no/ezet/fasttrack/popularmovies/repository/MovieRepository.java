package no.ezet.fasttrack.popularmovies.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import no.ezet.fasttrack.popularmovies.db.MovieCacheDao;
import no.ezet.fasttrack.popularmovies.model.ApiList;
import no.ezet.fasttrack.popularmovies.model.Genre;
import no.ezet.fasttrack.popularmovies.model.Movie;
import no.ezet.fasttrack.popularmovies.network.NetworkResource;
import no.ezet.fasttrack.popularmovies.network.NetworkResponse;
import no.ezet.fasttrack.popularmovies.network.Resource;
import no.ezet.fasttrack.popularmovies.api.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MovieRepository {

    private final ApiService apiService;
    private final MovieCacheDao movieCacheDao;

    @Inject
    MovieRepository(ApiService apiService, MovieCacheDao movieCacheDao) {
        this.apiService = apiService;
        this.movieCacheDao = movieCacheDao;
    }

    @NonNull
    public LiveData<Resource<Movie>> getMovieDetails(int id) {
        MutableLiveData<Resource<Movie>> liveData = new MutableLiveData<>();
        apiService.getDetailsWithAppend(id, "videos,reviews,images").enqueue(new Callback<Movie>() {
            @Override
            public void onResponse(@NonNull Call<Movie> call, @NonNull Response<Movie> response) {
                Timber.d("onResponse: getMovieDetails()");
                if (response.body() != null) {
                    liveData.setValue(Resource.success(response.body()));
                } else if (response.errorBody() != null) {
                    //noinspection ConstantConditions
                    liveData.setValue(Resource.error(response.errorBody().toString(), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Movie> call, @NonNull Throwable t) {
                liveData.setValue(Resource.error(t.getMessage(), null));
                Timber.d("onFailure: ");
            }
        });
        return liveData;
    }

    @NonNull
    public LiveData<Resource<List<Genre>>> getGenres() {
        MutableLiveData<Resource<List<Genre>>> liveData = new MutableLiveData<>();
        apiService.getGenres().enqueue(new Callback<ApiList<Genre>>() {
            @Override
            public void onResponse(@NonNull Call<ApiList<Genre>> call, @NonNull Response<ApiList<Genre>> response) {
                Timber.d("onResponse: getGenres()");
                if (response.body() != null) {
                    //noinspection ConstantConditions
                    liveData.setValue(Resource.success(response.body().results));
                } else if (response.errorBody() != null) {
                    //noinspection ConstantConditions
                    liveData.setValue(Resource.error(response.errorBody().toString(), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiList<Genre>> call, @NonNull Throwable t) {
                Timber.d("onFailure: getGenres()");
                liveData.setValue(Resource.error(t.getMessage(), null));
            }
        });
        return liveData;
    }

    @NonNull
    public LiveData<Resource<List<Movie>>> getPopular() {
        return getMovies(MovieListResource.QUERY_POPULAR);
    }

    @NonNull
    public LiveData<Resource<List<Movie>>> getUpcoming() {
        return getMovies(MovieListResource.QUERY_UPCOMING);
    }

    @NonNull
    public LiveData<Resource<List<Movie>>> getTopRated() {
        return getMovies(MovieListResource.QUERY_TOP_RATED);
    }

    @NonNull
    public LiveData<Resource<List<Movie>>> getNowPlaying() {
        return getMovies(MovieListResource.QUERY_NOW_PLAYING);
    }


    @NonNull
    private LiveData<Resource<List<Movie>>> getMovies(String query) {
        return new MovieListResource(query, movieCacheDao, apiService).getAsLiveData();
    }

    public static class MovieListResource extends NetworkResource<List<Movie>, ApiList<Movie>> {

        private static final String QUERY_POPULAR = "popular";
        private static final String QUERY_UPCOMING = "upcoming";
        private static final String QUERY_TOP_RATED = "top_rated";
        private static final String QUERY_NOW_PLAYING = "now_playing";
        private static boolean[] cached = new boolean[4];
        private MovieCacheDao movieCacheDao;
        private ApiService apiService;
        private String query;

        private MovieListResource(String query, MovieCacheDao movieCacheDao, ApiService apiService) {
            this.query = query;
            this.movieCacheDao = movieCacheDao;
            this.apiService = apiService;
        }

        @Override
        protected void saveCallResult(ApiList<Movie> movies) {
            for (Movie movie : movies.results) {
                switch (query) {
                    case QUERY_POPULAR:
                        movie.setType(Movie.POPULAR);
                        break;
                    case QUERY_UPCOMING:
                        movie.setType(Movie.UPCOMING);
                        break;
                    case QUERY_TOP_RATED:
                        movie.setType(Movie.TOP_RATED);
                        break;
                    case QUERY_NOW_PLAYING:
                        movie.setType(Movie.NOW_PLAYING);
                        break;
                }
            }
            cached[movies.results.get(0).getType()] = true;
            movieCacheDao.insert(movies.results);
        }

        @Override
        protected boolean shouldFetch(List<Movie> data) {
            return data == null || data.size() == 0 || !cached[data.get(0).getType()];
        }

        @Override
        protected LiveData<List<Movie>> loadFromDb() {
            switch (query) {
                case QUERY_POPULAR:
                    return movieCacheDao.getPopular();
                case QUERY_UPCOMING:
                    return movieCacheDao.getUpcoming();
                case QUERY_TOP_RATED:
                    return movieCacheDao.getTopRated();
                case QUERY_NOW_PLAYING:
                    return movieCacheDao.getNowPlaying();
                default:
                    throw new IllegalArgumentException("Cannot execute: " + query);
            }
        }

        @Override
        protected LiveData<NetworkResponse<ApiList<Movie>>> createCall() {
            final MutableLiveData<NetworkResponse<ApiList<Movie>>> networkResponse = new MutableLiveData<>();
            apiService.getMovies(query).enqueue(new Callback<ApiList<Movie>>() {
                @Override
                public void onResponse(@NonNull Call<ApiList<Movie>> call, @NonNull Response<ApiList<Movie>> response) {
                    Timber.d("onResponse");
                    networkResponse.setValue(new NetworkResponse<>(response));
                }

                @Override
                public void onFailure(@NonNull Call<ApiList<Movie>> call, @NonNull Throwable t) {
                    Timber.d("onFailure");
                    networkResponse.setValue(new NetworkResponse<>(t));
                }
            });
            return networkResponse;
        }
    }

//    @SuppressWarnings("unused")
//    private boolean isOnline() {
//        ConnectivityManager cm =
//                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo netInfo = cm.getActiveNetworkInfo();
//        return netInfo != null && netInfo.isConnectedOrConnecting();
//    }


}
