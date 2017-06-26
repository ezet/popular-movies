package no.ezet.fasttrack.popularmovies.view;

import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import no.ezet.fasttrack.popularmovies.R;
import no.ezet.fasttrack.popularmovies.service.ImageService;
import no.ezet.fasttrack.popularmovies.viewmodel.MovieListItem;
import no.ezet.fasttrack.popularmovies.viewmodel.MovieListViewModel;
import timber.log.Timber;

/**
 * An activity representing a list of Movies. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link MovieDetailFragment} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
@SuppressWarnings("ConstantConditions")
public abstract class MovieListBaseFragment extends LifecycleFragment {

    @Inject
    ImageService imageService;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    // TODO: 22.06.2017 Manage with DI
//    @Inject
    FragmentListener listener;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView errorTextView;
    protected MovieListViewModel viewModel;

    private static int calculateNoOfColumns(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (dpWidth / 180);
    }
    protected abstract Class<? extends MovieListViewModel> getViewModelClass();

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
        this.listener = (FragmentListener) getActivity();
        postponeEnterTransition();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.d("onCreate: ");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(getViewModelClass());
        viewModel.onRestoreInstanceState(savedInstanceState);
        viewModel.getIsLoading().observe(this, loading -> {
                    if (loading != null && loading) showLoadingIndicator();
                    else showMovieList();
                }
        );

        recyclerView = (RecyclerView) getActivity().findViewById(R.id.movie_list);
        progressBar = (ProgressBar) getActivity().findViewById(R.id.pb_loading_indicator);
        errorTextView = (TextView) getActivity().findViewById(R.id.tv_error_message);

        setupRecyclerView(recyclerView);
        startPostponedEnterTransition();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movie_list, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.getMovies().removeObservers(this);
        viewModel.getIsLoading().removeObservers(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.isChecked()) return true;
        item.setChecked(true);
        switch (item.getItemId()) {
            case R.id.action_popular: {
//                loadPopular();
                break;
            }
            case R.id.action_top_rated: {
//                loadTopRated();
                break;
            }
            case R.id.action_upcoming: {
//                loadUpcoming();
                break;
            }
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.lists_menu, menu);
        int menuItemId = 0;
        switch (viewModel.getSortBy()) {
            case MovieListViewModel.POPULAR:
                menuItemId = R.id.action_popular;
                break;
            case MovieListViewModel.UPCOMING:
                menuItemId = R.id.action_upcoming;
                break;
            case MovieListViewModel.TOP_RATED:
                menuItemId = R.id.action_top_rated;
                break;
        }
//        menu.findItem(menuItemId).setChecked(true);
    }


    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), calculateNoOfColumns(getContext())));
        final MovieListRecyclerViewAdapter adapter = new MovieListRecyclerViewAdapter(imageService, listener);
        viewModel.getMovies().observe(this, movieList -> {
            setSubTitle();
            adapter.setMovies(movieList);
        });
        recyclerView.setAdapter(adapter);
    }

    protected void setSubTitle() {
        ActionBar toolbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        toolbar.setSubtitle(getSubtitle());
    }

    @StringRes
    protected abstract int getSubtitle();

    public void showLoadingIndicator() {
        Timber.d("showLoadingIndicator: ");
        errorTextView.setVisibility(View.INVISIBLE);
//        recyclerView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }


    private void showMovieList() {
        Timber.d("showMovieList: ");
        progressBar.setVisibility(View.INVISIBLE);
        errorTextView.setVisibility(View.INVISIBLE);
//        recyclerView.setVisibility(View.VISIBLE);
    }

    public void showLoadingError() {
        recyclerView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        errorTextView.setVisibility(View.VISIBLE);
    }

    public interface FragmentListener {

        void onItemClick(View f, MovieListItem movie);
    }

    private static class MovieListRecyclerViewAdapter
            extends RecyclerView.Adapter<MovieListRecyclerViewAdapter.ViewHolder> {

        private final List<MovieListItem> movies = new ArrayList<>();
        private ImageService imageService;
        private FragmentListener listener;
        private int counter;

        MovieListRecyclerViewAdapter(ImageService imageService, FragmentListener listener) {
            this.imageService = imageService;
            this.listener = listener;
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            return movies.get(position).id;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.movie_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final MovieListItem movie = movies.get(position);
            loadImage(movie.posterPath, holder.posterImage);
            holder.itemView.setOnClickListener((View view) -> listener.onItemClick(view, movie));
        }

        @Override
        public int getItemCount() {
            return movies.size();
        }

        private void loadImage(String relPath, ImageView imageView) {
            imageService.loadImage(relPath, ImageService.SIZE_W342, imageView);
        }

        void setMovies(List<MovieListItem> movieList) {
            Timber.d("setMovies: " + counter++);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(this.movies, movieList));
            this.movies.clear();
            this.movies.addAll(movieList);
            diffResult.dispatchUpdatesTo(this);
//            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView posterImage;

            ViewHolder(View itemView) {
                super(itemView);
                posterImage = (ImageView) itemView.findViewById(R.id.iv_poster_image);
            }
        }

        public class DiffCallback extends DiffUtil.Callback {

            private final List<MovieListItem> newList;
            private final List<MovieListItem> oldList;

            DiffCallback(List<MovieListItem> oldList, List<MovieListItem> newList) {
                this.newList = newList;
                this.oldList = oldList;
            }

            @Override
            public int getOldListSize() {
                return oldList != null ? oldList.size() : 0;
            }

            @Override
            public int getNewListSize() {
                return newList != null ? newList.size() : 0;
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return newList.get(newPosition) == oldList.get(oldPosition);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return newList.get(newPosition).posterPath.equals(oldList.get(oldPosition).posterPath);
            }
        }
    }
}


